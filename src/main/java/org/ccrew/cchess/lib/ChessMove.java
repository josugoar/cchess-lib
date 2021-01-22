package org.ccrew.cchess.lib;

public class ChessMove {

    public int number;
    public ChessPiece piece;
    public ChessPiece promotionPiece;
    public ChessPiece castlingRook;
    public ChessPiece victim;
    public int r0;
    public int f0;
    public int r1;
    public int f1;
    public boolean ambiguousRank;
    public boolean ambiguousFile;
    public boolean enPassant;
    public CheckState checkState;

    public String getLan() {
        if (castlingRook != null) {
            if (f1 > f0) {
                return "O-O";
            } else {
                return "O-O-O";
            }
        }

        var builder = new StringBuilder();
        if (victim != null) {
            builder.append(String.format("%c%dx%c%d", 'a' + f0, r0 + 1, 'a' + f1, r1 + 1));
        } else {
            builder.append(String.format("%c%d-%c%d", 'a' + f0, r0 + 1, 'a' + f1, r1 + 1));
        }

        final char[] promotionSymbols = { ' ', 'R', 'N', 'B', 'Q', 'K' };
        if (promotionPiece != null) {
            builder.append(String.format("=%c", promotionSymbols[promotionPiece.type.ordinal()]));
        }

        switch (checkState) {
            case CHECK:
                builder.append('+');
                break;
            case CHECKMATE:
                builder.append('#');
                break;
            default:
                break;
        }

        return builder.toString();
    }

    public String getSan() {
        final String[] pieceNames = { "", "R", "N", "B", "Q", "K" };
        return makeSan(pieceNames);
    }

    public String getFan() {
        final String[] whitePieceNames = { "", "♖", "♘", "♗", "♕", "♔" };
        final String[] blackPieceNames = { "", "♜", "♞", "♝", "♛", "♚" };
        if (piece.getColor() == Color.WHITE) {
            return makeSan(whitePieceNames);
        } else {
            return makeSan(blackPieceNames);
        }
    }

    public String makeSan(String[] pieceNames) {
        if (castlingRook != null) {
            if (f1 > f0) {
                return "0-0";
            } else {
                return "0-0-0";
            }
        }

        var builder = new StringBuilder();
        builder.append(pieceNames[piece.type.ordinal()]);
        if (ambiguousFile) {
            builder.append(String.format("%c", 'a' + f0));
        }
        if (ambiguousRank) {
            builder.append(String.format("%d", r0 + 1));
        }
        if (victim != null) {
            builder.append("x");
        }
        builder.append(String.format("%c%d", 'a' + f1, r1 + 1));

        if (promotionPiece != null) {
            builder.append(String.format("=%s", pieceNames[promotionPiece.type.ordinal()]));
        }

        switch (checkState) {
            case CHECK:
                builder.append('+');
                break;
            case CHECKMATE:
                builder.append('#');
                break;
            default:
                break;
        }

        return builder.toString();
    }

    /* Move suitable for a chess engine (CECP/UCI) */
    public String getEngine() {
        var builder = new StringBuilder();
        final char[] promotionSymbols = { ' ', 'r', 'n', 'b', 'q', ' ' };
        if (promotionPiece != null) {
            builder.append(String.format("%c%d%c%d%c", 'a' + f0, r0 + 1, 'a' + f1, r1 + 1,
                    promotionSymbols[promotionPiece.type.ordinal()]));
        } else {
            builder.append(String.format("%c%d%c%d", 'a' + f0, r0 + 1, 'a' + f1, r1 + 1));
        }
        return builder.toString();
    }

    @Override
    public ChessMove clone() {
        var move = new ChessMove();
        move.number = number;
        move.piece = piece;
        move.promotionPiece = promotionPiece;
        move.castlingRook = castlingRook;
        move.victim = victim;
        move.r0 = r0;
        move.f0 = f0;
        move.r1 = r1;
        move.f1 = f1;
        move.ambiguousRank = ambiguousRank;
        move.ambiguousFile = ambiguousFile;
        move.enPassant = enPassant;
        move.checkState = checkState;
        return move;
    }

}
