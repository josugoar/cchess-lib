package org.ccrew.cchess.lib;

import static org.ccrew.cchess.util.Logging.debug;

import org.ccrew.cchess.util.Out;

public class ChessState {

    public int number = 0;
    public ChessPlayer[] players = new ChessPlayer[2];
    public ChessPlayer currentPlayer;

    public ChessPlayer getOpponent() {
        return currentPlayer.color == Color.WHITE ? players[Color.BLACK.ordinal()] : players[Color.WHITE.ordinal()];
    };

    public boolean[] canCastleKingside = new boolean[2];
    public boolean[] canCastleQueenside = new boolean[2];
    public int enPassantIndex = -1;
    public CheckState checkState;
    public int halfmoveClock;

    public ChessPiece[] board = new ChessPiece[64];
    public ChessMove lastMove = null;

    /* Bitmap of all the pieces */
    private long[] pieceMasks = new long[2];

    private ChessState() {
    }

    public ChessState(String fen) {
        players[Color.WHITE.ordinal()] = new ChessPlayer(Color.WHITE);
        players[Color.BLACK.ordinal()] = new ChessPlayer(Color.BLACK);
        for (int i = 0; i < 64; i++) {
            board[i] = null;
        }

        String[] fields = fen.split(" ");

        /* Field 1: Piece placement */
        String[] ranks = fields[0].split("/");
        for (int rank = 0; rank < 8; rank++) {
            var rankString = ranks[7 - rank];
            for (int file = 0, offset = 0; file < 8 && offset < rankString.length(); offset++) {
                var c = rankString.charAt(offset);
                if (c >= '1' && c <= '8') {
                    file += c - '0';
                    continue;
                }

                Out<PieceType> type = new Out<>();
                var color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
                decodePieceType(Character.toUpperCase(c), type);

                int index = getIndex(rank, file);
                ChessPiece piece = new ChessPiece(players[color.ordinal()], type.value);
                board[index] = piece;
                long mask = BitBoard.setLocationMasks[index];
                pieceMasks[color.ordinal()] |= mask;
                file++;
            }
        }

        /* Field 2: Active color */
        if (fields[1].equals("w")) {
            currentPlayer = players[Color.WHITE.ordinal()];
        } else if (fields[1].equals("b")) {
            currentPlayer = players[Color.BLACK.ordinal()];
        }

        /* Field 3: Castling availability */
        if (!fields[2].equals("-")) {
            for (int i = 0; i < fields[2].length(); i++) {
                var c = fields[2].charAt(i);
                if (c == 'K') {
                    canCastleKingside[Color.WHITE.ordinal()] = true;
                } else if (c == 'Q') {
                    canCastleQueenside[Color.WHITE.ordinal()] = true;
                } else if (c == 'k') {
                    canCastleKingside[Color.BLACK.ordinal()] = true;
                } else if (c == 'q') {
                    canCastleQueenside[Color.BLACK.ordinal()] = true;
                }
            }
        }

        /* Field 4: En passant target square */
        if (!fields[3].equals("-")) {
            enPassantIndex = getIndex(fields[3].charAt(1) - '1', fields[3].charAt(0) - 'a');
        }

        /* Field 5: Halfmove clock */
        halfmoveClock = Integer.parseInt(fields[4]);

        /* Field 6: Fullmove number */
        number = (Integer.parseInt(fields[5]) - 1) * 2;
        if (currentPlayer.color == Color.BLACK) {
            number++;
        }

        checkState = getCheckState(currentPlayer);
    }

    @Override
    public ChessState clone() {
        ChessState state = new ChessState();

        state.number = number;
        state.players[Color.WHITE.ordinal()] = players[Color.WHITE.ordinal()];
        state.players[Color.BLACK.ordinal()] = players[Color.BLACK.ordinal()];
        state.currentPlayer = currentPlayer;
        state.canCastleKingside[Color.WHITE.ordinal()] = canCastleKingside[Color.WHITE.ordinal()];
        state.canCastleQueenside[Color.WHITE.ordinal()] = canCastleQueenside[Color.WHITE.ordinal()];
        state.canCastleKingside[Color.BLACK.ordinal()] = canCastleKingside[Color.BLACK.ordinal()];
        state.canCastleQueenside[Color.BLACK.ordinal()] = canCastleQueenside[Color.BLACK.ordinal()];
        state.enPassantIndex = enPassantIndex;
        state.checkState = checkState;
        if (lastMove != null) {
            state.lastMove = lastMove.clone();
        }
        for (int i = 0; i < 64; i++) {
            state.board[i] = board[i];
        }
        state.pieceMasks[Color.WHITE.ordinal()] = pieceMasks[Color.WHITE.ordinal()];
        state.pieceMasks[Color.BLACK.ordinal()] = pieceMasks[Color.BLACK.ordinal()];
        state.halfmoveClock = halfmoveClock;

        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChessState)) {
            return false;
        }

        ChessState state = (ChessState) obj;

        /*
         * Check first if there is the same layout of pieces (unlikely), then that the
         * same player is on move, then that the move castling and en-passant state are
         * the same. This follows the rules for determining threefold repetition:
         *
         * https://en.wikipedia.org/wiki/Threefold_repetition
         */
        if (pieceMasks[Color.WHITE.ordinal()] != state.pieceMasks[Color.WHITE.ordinal()]
                || pieceMasks[Color.BLACK.ordinal()] != state.pieceMasks[Color.BLACK.ordinal()]
                || currentPlayer.color != state.currentPlayer.color
                || canCastleKingside[Color.WHITE.ordinal()] != state.canCastleKingside[Color.WHITE.ordinal()]
                || canCastleQueenside[Color.WHITE.ordinal()] != state.canCastleQueenside[Color.WHITE.ordinal()]
                || canCastleKingside[Color.BLACK.ordinal()] != state.canCastleKingside[Color.BLACK.ordinal()]
                || canCastleQueenside[Color.BLACK.ordinal()] != state.canCastleQueenside[Color.BLACK.ordinal()]
                || enPassantIndex != state.enPassantIndex)
            return false;

        /* Finally check the same piece types are present */
        for (int i = 0; i < 64; i++) {
            if (board[i] != null && board[i].type != state.board[i].type) {
                return false;
            }
        }

        return true;
    }

    public String getFen() {
        var value = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int skipCount = 0;
            for (int file = 0; file < 8; file++) {
                var p = board[getIndex(rank, file)];
                if (p == null) {
                    skipCount++;
                } else {
                    if (skipCount > 0) {
                        value.append(String.format("%d", skipCount));
                        skipCount = 0;
                    }
                    value.append(String.format("%c", p.getSymbol()));
                }
            }
            if (skipCount > 0) {
                value.append(String.format("%d", skipCount));
            }
            if (rank != 0) {
                value.append('/');
            }
        }

        value.append(' ');
        if (currentPlayer.color == Color.WHITE) {
            value.append('w');
        } else {
            value.append('b');
        }

        value.append(' ');
        if (canCastleKingside[Color.WHITE.ordinal()]) {
            value.append('K');
        }
        if (canCastleQueenside[Color.WHITE.ordinal()]) {
            value.append('Q');
        }
        if (canCastleKingside[Color.BLACK.ordinal()]) {
            value.append('k');
        }
        if (canCastleQueenside[Color.BLACK.ordinal()]) {
            value.append('q');
        }
        if (!(canCastleKingside[Color.WHITE.ordinal()] || canCastleQueenside[Color.WHITE.ordinal()]
                || canCastleKingside[Color.BLACK.ordinal()] || canCastleQueenside[Color.BLACK.ordinal()])) {
            value.append('-');
        }

        value.append(' ');
        if (enPassantIndex >= 0) {
            value.append(String.format("%c%d", 'a' + getFile(enPassantIndex), getRank(enPassantIndex) + 1));
        } else {
            value.append('-');
        }

        value.append(' ');
        value.append(String.format("%d", halfmoveClock));

        value.append(' ');
        if (currentPlayer.color == Color.WHITE) {
            value.append(String.format("%d", number / 2));
        } else {
            value.append(String.format("%d", number / 2 + 1));
        }

        return value.toString();
    }

    public static int getIndex(int rank, int file) {
        return rank * 8 + file;
    }

    public static int getRank(int index) {
        return index / 8;
    }

    public static int getFile(int index) {
        return index % 8;
    }

    public boolean move(String move) {
        return move(move, true);
    }

    public boolean move(String move, boolean apply) {
        Out<Integer> r0 = new Out<>();
        Out<Integer> f0 = new Out<>();
        Out<Integer> r1 = new Out<>();
        Out<Integer> f1 = new Out<>();
        Out<PieceType> promotionType = new Out<>();

        if (!decodeMove(currentPlayer, move, r0, f0, r1, f1, promotionType)) {
            return false;
        }

        if (!moveWithCoords(currentPlayer, r0.value, f0.value, r1.value, f1.value, promotionType.value, apply)) {
            return false;
        }

        return true;
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1) {
        return moveWithCoords(player, r0, f0, r1, f1, PieceType.QUEEN, true, true);
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1, PieceType promotionType) {
        return moveWithCoords(player, r0, f0, r1, f1, promotionType, true, true);
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1, boolean apply) {
        return moveWithCoords(player, r0, f0, r1, f1, PieceType.QUEEN, apply, true);
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1, PieceType promotionType,
            boolean apply) {
        return moveWithCoords(player, r0, f0, r1, f1, promotionType, apply, true);
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1, boolean apply,
            boolean testCheck) {
        return moveWithCoords(player, r0, f0, r1, f1, PieceType.QUEEN, apply, testCheck);
    }

    public boolean moveWithCoords(ChessPlayer player, int r0, int f0, int r1, int f1, PieceType promotionType,
            boolean apply, boolean testCheck) {
        var start = getIndex(r0, f0);
        var end = getIndex(r1, f1);

        var color = player.color;
        var opponentColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        /* Must be moving own piece */
        var piece = board[start];
        if ((piece == null) || piece.player != player) {
            return false;
        }

        /* Check valid move */
        long endMask = BitBoard.setLocationMasks[end];
        long moveMask = BitBoard.moveMasks[color.ordinal() * 64 * 6 + piece.type.ordinal() * 64 + start];
        if ((endMask & moveMask) == 0) {
            return false;
        }

        /* Check no pieces in the way */
        long overMask = BitBoard.overMasks[start * 64 + end];
        if ((overMask & (pieceMasks[Color.WHITE.ordinal()] | pieceMasks[Color.BLACK.ordinal()])) != 0) {
            return false;
        }

        /* Get victim of move */
        var victim = board[end];
        var victimIndex = end;

        /* Can't take own pieces */
        if (victim != null && victim.player == player) {
            return false;
        }

        /* Check special moves */
        int rookStart = -1;
        int rookEnd = -1;
        boolean isPromotion = false;
        boolean enPassant = false;
        boolean ambiguousRank = false;
        boolean ambiguousFile = false;
        switch (piece.type) {
            case PAWN:
                /* Check if taking an marched pawn */
                if (victim == null && end == enPassantIndex) {
                    enPassant = true;
                    victimIndex = getIndex(r1 == 2 ? 3 : 4, f1);
                    victim = board[victimIndex];
                }

                /* If moving diagonally there must be a victim */
                if (f0 != f1) {
                    if (victim == null) {
                        return false;
                    }
                } else {
                    /* If moving forward can't take enemy */
                    if (victim != null) {
                        return false;
                    }
                }
                isPromotion = r1 == 0 || r1 == 7;

                /* Always show the file of a pawn capturing */
                if (victim != null) {
                    ambiguousFile = true;
                }
                break;
            case KING:
                /* If moving more than one square must be castling */
                if (Math.abs(f0 - f1) > 1) {
                    /* File the rook is on */
                    rookStart = getIndex(r0, f1 > f0 ? 7 : 0);
                    rookEnd = getIndex(r0, f1 > f0 ? f1 - 1 : f1 + 1);

                    /* Check if can castle */
                    if (f1 > f0) {
                        if (!canCastleKingside[color.ordinal()]) {
                            return false;
                        }
                    } else {
                        if (!canCastleQueenside[color.ordinal()]) {
                            return false;
                        }
                    }

                    var rook = board[rookStart];
                    if (rook == null || rook.type != PieceType.ROOK || rook.getColor() != color) {
                        return false;
                    }

                    /* Check rook can move */
                    long rookOverMask = BitBoard.overMasks[rookStart * 64 + rookEnd];
                    if ((rookOverMask & (pieceMasks[Color.WHITE.ordinal()] | pieceMasks[Color.BLACK.ordinal()])) != 0) {
                        return false;
                    }

                    /* Can't castle when in check */
                    if (checkState == CheckState.CHECK) {
                        return false;
                    }

                    /* Square moved across can't be under attack */
                    if (!moveWithCoords(player, r0, f0, getRank(rookEnd), getFile(rookEnd), PieceType.QUEEN, false,
                            true)) {
                        return false;
                    }
                }
                break;
            default:
                break;
        }

        if (!apply && !testCheck) {
            return true;
        }

        /*
         * Check if other pieces of the same type can make this move - this is required
         * for SAN notation
         */
        if (apply) {
            for (int i = 0; i < 64; i++) {
                /* Ignore our move */
                if (i == start) {
                    continue;
                }

                /* Check for a friendly piece of the same type */
                var p = board[i];
                if (p == null || p.player != player || p.type != piece.type) {
                    continue;
                }

                /* If more than one piece can move then the rank and/or file are ambiguous */
                var r = getRank(i);
                var f = getFile(i);
                if (moveWithCoords(player, r, f, r1, f1, PieceType.QUEEN, false)) {
                    if (r != r0) {
                        ambiguousRank = true;
                    }
                    if (f != f0) {
                        ambiguousFile = true;
                    }
                }
            }
        }

        var oldWhiteMask = pieceMasks[Color.WHITE.ordinal()];
        var oldBlackMask = pieceMasks[Color.BLACK.ordinal()];
        var oldWhiteCanCastleKingside = canCastleKingside[Color.WHITE.ordinal()];
        var oldWhiteCanCastleQueenside = canCastleQueenside[Color.WHITE.ordinal()];
        var oldBlackCanCastleKingside = canCastleKingside[Color.BLACK.ordinal()];
        var oldBlackCanCastleQueenside = canCastleQueenside[Color.BLACK.ordinal()];
        var OldEnPassantIndex = enPassantIndex;
        var OldHalfmoveClock = halfmoveClock;

        /* Update board */
        board[start] = null;
        pieceMasks[Color.WHITE.ordinal()] &= BitBoard.clearLocationMasks[start];
        pieceMasks[Color.BLACK.ordinal()] &= BitBoard.clearLocationMasks[start];
        if (victim != null) {
            board[victimIndex] = null;
            pieceMasks[Color.WHITE.ordinal()] &= BitBoard.clearLocationMasks[victimIndex];
            pieceMasks[Color.BLACK.ordinal()] &= BitBoard.clearLocationMasks[victimIndex];
        }
        if (isPromotion) {
            board[end] = new ChessPiece(player, promotionType);
        } else {
            board[end] = piece;
        }
        pieceMasks[color.ordinal()] |= endMask;
        pieceMasks[opponentColor.ordinal()] &= BitBoard.clearLocationMasks[end];
        if (rookStart >= 0) {
            var rook = board[rookStart];
            board[rookStart] = null;
            pieceMasks[color.ordinal()] &= BitBoard.clearLocationMasks[rookStart];
            board[rookEnd] = rook;
            pieceMasks[color.ordinal()] |= BitBoard.setLocationMasks[rookEnd];
        }

        /* Can't castle once king has moved */
        if (piece.type == PieceType.KING) {
            canCastleKingside[color.ordinal()] = false;
            canCastleQueenside[color.ordinal()] = false;
        }
        /* Can't castle once rooks have moved */
        else if (piece.type == PieceType.ROOK) {
            int baseRank = color == Color.WHITE ? 0 : 7;
            if (r0 == baseRank) {
                if (f0 == 0) {
                    canCastleQueenside[color.ordinal()] = false;
                } else if (f0 == 7) {
                    canCastleKingside[color.ordinal()] = false;
                }
            }
        }
        /* Can't castle once the rooks have been captured */
        else if (victim != null && victim.type == PieceType.ROOK) {
            int baseRank = opponentColor == Color.WHITE ? 0 : 7;
            if (r1 == baseRank) {
                if (f1 == 0) {
                    canCastleQueenside[opponentColor.ordinal()] = false;
                } else if (f1 == 7) {
                    canCastleKingside[opponentColor.ordinal()] = false;
                }
            }
        }

        /* Pawn square moved over is vulnerable */
        if (piece.type == PieceType.PAWN && overMask != 0) {
            enPassantIndex = getIndex((r0 + r1) / 2, f0);
        } else {
            enPassantIndex = -1;
        }

        /* Reset halfmove count when pawn moved or piece taken */
        if (piece.type == PieceType.PAWN || victim != null) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        /* Test if this move would leave that player in check */
        boolean result = true;
        if (testCheck && isInCheck(player)) {
            result = false;
        }

        /* Undo move */
        if (!apply || !result) {
            board[start] = piece;
            board[end] = null;
            if (victim != null) {
                board[victimIndex] = victim;
            }
            if (rookStart >= 0) {
                var rook = board[rookEnd];
                board[rookStart] = rook;
                board[rookEnd] = null;
            }
            pieceMasks[Color.WHITE.ordinal()] = oldWhiteMask;
            pieceMasks[Color.BLACK.ordinal()] = oldBlackMask;
            canCastleKingside[Color.WHITE.ordinal()] = oldWhiteCanCastleKingside;
            canCastleQueenside[Color.WHITE.ordinal()] = oldWhiteCanCastleQueenside;
            canCastleKingside[Color.BLACK.ordinal()] = oldBlackCanCastleKingside;
            canCastleQueenside[Color.BLACK.ordinal()] = oldBlackCanCastleQueenside;
            enPassantIndex = OldEnPassantIndex;
            halfmoveClock = OldHalfmoveClock;

            return result;
        }

        currentPlayer = color == Color.WHITE ? players[Color.BLACK.ordinal()] : players[Color.WHITE.ordinal()];
        checkState = getCheckState(currentPlayer);

        lastMove = new ChessMove();
        lastMove.number = number;
        lastMove.piece = piece;
        if (isPromotion) {
            lastMove.promotionPiece = board[end];
        }
        lastMove.victim = victim;
        if (rookEnd >= 0) {
            lastMove.castlingRook = board[rookEnd];
        }
        lastMove.r0 = r0;
        lastMove.f0 = f0;
        lastMove.r1 = r1;
        lastMove.f1 = f1;
        lastMove.ambiguousRank = ambiguousRank;
        lastMove.ambiguousFile = ambiguousFile;
        lastMove.enPassant = enPassant;
        lastMove.checkState = checkState;

        return true;
    }

    public ChessResult getResult(Out<ChessRule> rule) {
        rule.value = ChessRule.CHECKMATE;
        if (checkState == CheckState.CHECKMATE) {
            if (currentPlayer.color == Color.WHITE) {
                rule.value = ChessRule.CHECKMATE;
                return ChessResult.BLACK_WON;
            } else {
                rule.value = ChessRule.CHECKMATE;
                return ChessResult.WHITE_WON;
            }
        }

        if (!canMove(currentPlayer)) {
            rule.value = ChessRule.STALEMATE;
            return ChessResult.DRAW;
        }

        if (lastMove != null && lastMove.victim != null && !haveSufficientMaterial()) {
            rule.value = ChessRule.INSUFFICIENT_MATERIAL;
            return ChessResult.DRAW;
        }

        return ChessResult.IN_PROGRESS;
    }

    private CheckState getCheckState(ChessPlayer player) {
        if (isInCheck(player)) {
            if (isInCheckmate(player)) {
                return CheckState.CHECKMATE;
            } else {
                return CheckState.CHECK;
            }
        }
        return CheckState.NONE;
    }

    public boolean isInCheck(ChessPlayer player) {
        var opponent = player.color == Color.WHITE ? players[Color.BLACK.ordinal()] : players[Color.WHITE.ordinal()];

        /* Is in check if any piece can take the king */
        for (int kingIndex = 0; kingIndex < 64; kingIndex++) {
            var p = board[kingIndex];
            if (p != null && p.player == player && p.type == PieceType.KING) {
                /* See if any enemy pieces can take the king */
                for (int start = 0; start < 64; start++) {
                    if (moveWithCoords(opponent, getRank(start), getFile(start), getRank(kingIndex), getFile(kingIndex),
                            PieceType.QUEEN, false, false)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isInCheckmate(ChessPlayer player) {
        /* Is in checkmate if no pieces can move */
        for (int pieceIndex = 0; pieceIndex < 64; pieceIndex++) {
            var p = board[pieceIndex];
            if (p != null && p.player == player) {
                for (int end = 0; end < 64; end++) {
                    if (moveWithCoords(player, getRank(pieceIndex), getFile(pieceIndex), getRank(end), getFile(end),
                            PieceType.QUEEN, false, true)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean canMove(ChessPlayer player) {
        boolean havePieces = false;

        for (int start = 0; start < 64; start++) {
            var p = board[start];
            if (p != null && p.player == player) {
                havePieces = true;

                /* See if can move anywhere */
                for (int end = 0; end < 64; end++) {
                    if (moveWithCoords(player, getRank(start), getFile(start), getRank(end), getFile(end),
                            PieceType.QUEEN, false, true)) {
                        return true;
                    }
                }
            }
        }

        /* Only mark as stalemate if have at least one piece */
        if (havePieces) {
            return false;
        } else {
            return true;
        }
    }

    public boolean haveSufficientMaterial() {
        var whiteKnightCount = 0;
        var whiteBishopCount = 0;
        var whiteBishopOnWhiteSquare = false;
        var whiteBishopOnBlackSquare = false;
        var blackKnightCount = 0;
        var blackBishopCount = 0;
        var blackBishopOnWhiteSquare = false;
        var blackBishopOnBlackSquare = false;

        for (int i = 0; i < 64; i++) {
            var p = board[i];
            if (p == null) {
                continue;
            }

            /* Any pawns, rooks or queens can perform checkmate */
            if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                return true;
            }

            /* Otherwise, count the minor pieces for each colour... */
            if (p.type == PieceType.KNIGHT) {
                if (p.getColor() == Color.WHITE) {
                    whiteKnightCount++;
                } else {
                    blackKnightCount++;
                }
            }

            if (p.type == PieceType.BISHOP) {
                var color = Color.BLACK;
                if ((i + i / 8) % 2 != 0) {
                    color = Color.WHITE;
                }

                if (p.getColor() == Color.WHITE) {
                    if (color == Color.WHITE) {
                        whiteBishopOnWhiteSquare = true;
                    } else {
                        whiteBishopOnBlackSquare = true;
                    }
                    whiteBishopCount++;
                } else {
                    if (color == Color.WHITE) {
                        blackBishopOnWhiteSquare = true;
                    } else {
                        blackBishopOnBlackSquare = true;
                    }
                    blackBishopCount++;
                }
            }

            /*
             * We count the following positions as insufficient:
             *
             * 1) king versus king 2) king and bishop versus king 3) king and knight versus
             * king 4) king and bishop versus king and bishop with the bishops on the same
             * color. (Any number of additional bishops of either color on the same color of
             * square due to underpromotion do not affect the situation.)
             *
             * From: https://en.wikipedia.org/wiki/Draw_(chess)#Draws_in_all_games
             *
             * Note also that this follows FIDE rules, not USCF rules. E.g. K+N+N vs. K
             * cannot be forced, so it's not counted as a draw.
             *
             * This is also what CECP engines will be expecting:
             *
             * "Note that (in accordance with FIDE rules) only KK, KNK, KBK and KBKB with
             * all bishops on the same color can be claimed as draws on the basis of
             * insufficient mating material. The end-games KNNK, KBKN, KNKN and KBKB with
             * unlike bishops do have mate positions, and cannot be claimed. Complex draws
             * based on locked Pawn chains will not be recognized as draws by most
             * interfaces, so do not claim in such positions, but just offer a draw or play
             * on."
             *
             * From: http://www.open-aurec.com/wbforum/WinBoard/engine-intf.html
             *
             * (In contrast, UCI seems to expect the interface to handle draws itself.)
             */

            /*
             * Two knights versus king can checkmate (though not against an optimal
             * opponent)
             */
            if (whiteKnightCount > 1 || blackKnightCount > 1) {
                return true;
            }

            /* Bishop and knight versus king can checkmate */
            if (whiteBishopCount > 0 && whiteKnightCount > 0) {
                return true;
            }
            if (blackBishopCount > 0 && blackKnightCount > 0) {
                return true;
            }

            /*
             * King and bishops versus king can checkmate as long as the bishops are on both
             * colours
             */
            if (whiteBishopOnWhiteSquare && whiteBishopOnBlackSquare) {
                return true;
            }
            if (blackBishopOnWhiteSquare && blackBishopOnBlackSquare) {
                return true;
            }

            /* King and minor piece vs. King and knight is surprisingly not a draw */
            if ((whiteBishopCount > 0 || whiteKnightCount > 0) && blackKnightCount > 0) {
                return true;
            }
            if ((blackBishopCount > 0 || blackKnightCount > 0) && whiteKnightCount > 0) {
                return true;
            }

            /*
             * King and bishop can checkmate vs. king and bishop if bishops are on opposite
             * colors
             */
            if (whiteBishopCount > 0 && blackBishopCount > 0) {
                if (whiteBishopOnWhiteSquare && blackBishopOnBlackSquare) {
                    return true;
                } else if (whiteBishopOnBlackSquare && blackBishopOnWhiteSquare) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean decodePieceType(char c, Out<PieceType> type) {
        type.value = PieceType.PAWN;
        switch (c) {
            case 'P':
                type.value = PieceType.PAWN;
                return true;
            case 'R':
                type.value = PieceType.ROOK;
                return true;
            case 'N':
                type.value = PieceType.KNIGHT;
                return true;
            case 'B':
                type.value = PieceType.BISHOP;
                return true;
            case 'Q':
                type.value = PieceType.QUEEN;
                return true;
            case 'K':
                type.value = PieceType.KING;
                return true;
            default:
                return false;
        }
    }

    private boolean decodeMove(ChessPlayer player, String move, Out<Integer> r0, Out<Integer> f0, Out<Integer> r1,
            Out<Integer> f1, Out<PieceType> promotionType) {
        int i = 0;

        promotionType.value = PieceType.QUEEN;
        if (move.startsWith("O-O-O")) {
            if (player.color == Color.WHITE) {
                r0.value = r1.value = 0;
            } else {
                r0.value = r1.value = 7;
            }
            f0.value = 4;
            f1.value = 2;
            i += "O-O-O".length();
        } else if (move.startsWith("O-O")) {
            if (player.color == Color.WHITE) {
                r0.value = r1.value = 0;
            } else {
                r0.value = r1.value = 7;
            }
            f0.value = 4;
            f1.value = 6;
            i += "O-O".length();
        } else {
            Out<PieceType> type = new Out<>(PieceType.PAWN);
            if (move.length() > i && decodePieceType(move.charAt(i), type)) {
                i++;
            }

            r0.value = f0.value = r1.value = f1.value = -1;
            if (move.length() > i && move.charAt(i) >= 'a' && move.charAt(i) <= 'h') {
                f1.value = move.charAt(i) - 'a';
                i++;
            }
            if (move.length() > i && move.charAt(i) >= '1' && move.charAt(i) <= '8') {
                r1.value = move.charAt(i) - '1';
                i++;
            }
            if (move.length() > i && (move.charAt(i) == 'x' || move.charAt(i) == '-')) {
                i++;
            }
            if (move.length() > i && move.charAt(i) >= 'a' && move.charAt(i) <= 'h') {
                f0.value = f1.value;
                f1.value = move.charAt(i) - 'a';
                i++;
            }
            if (move.length() > i && move.charAt(i) >= '1' && move.charAt(i) <= '8') {
                r0.value = r1.value;
                r1.value = move.charAt(i) - '1';
                i++;
            }
            if (move.length() > i && move.charAt(i) == '=') {
                i++;
                if (move.length() > i && decodePieceType(move.charAt(i), promotionType)) {
                    i++;
                }
            } else if (move.length() > i) {
                switch (move.charAt(i)) {
                    case 'q':
                        // Fall through
                    case 'Q':
                        promotionType.value = PieceType.QUEEN;
                        i++;
                        break;
                    case 'n':
                        // Fall through
                    case 'N':
                        promotionType.value = PieceType.KNIGHT;
                        i++;
                        break;
                    case 'r':
                        // Fall through
                    case 'R':
                        promotionType.value = PieceType.ROOK;
                        i++;
                        break;
                    case 'b':
                        // Fall through
                    case 'B':
                        promotionType.value = PieceType.BISHOP;
                        i++;
                        break;
                    default:
                        break;
                }
            }

            /* Don't have a destination to move to */
            if (r1.value < 0 || f1.value < 0) {
                debug("Move %s missing destination", move);
                return false;
            }

            /* Find source piece */
            if (r0.value < 0 || f0.value < 0) {
                int matchRank = -1;
                int matchFile = -1;

                for (int file = 0; file < 8; file++) {
                    if (f0.value >= 0 && file != f0.value) {
                        continue;
                    }

                    for (int rank = 0; rank < 8; rank++) {
                        if (r0.value >= 0 && rank != r0.value) {
                            continue;
                        }

                        /* Only check this players pieces of the correct type */
                        var piece = board[getIndex(rank, file)];
                        if (piece == null || piece.type != type.value || piece.player != player) {
                            continue;
                        }

                        /* See if can move here */
                        if (!this.moveWithCoords(player, rank, file, r1.value, f1.value, PieceType.QUEEN, false)) {
                            continue;
                        }

                        /* Duplicate match */
                        if (matchRank >= 0) {
                            debug("Move %s is ambiguous", move);
                            return false;
                        }

                        matchRank = rank;
                        matchFile = file;
                    }
                }

                if (matchRank < 0) {
                    debug("Move %s has no matches", move);
                    return false;
                }

                r0.value = matchRank;
                f0.value = matchFile;
            }
        }

        if (move.length() > i && move.charAt(i) == '+') {
            i++;
        } else if (move.length() > i && move.charAt(i) == '#') {
            i++;
        }

        if (i < move.length()) {
            debug("Move %s has unexpected characters", move);
            return false;
        }

        return true;
    }

}
