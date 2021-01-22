package org.ccrew.cchess.lib;

import java.util.ArrayList;
import java.util.List;

import org.ccrew.cchess.lib.ChessPlayer.DoMoveSource;
import org.ccrew.cchess.util.Handler;
import org.ccrew.cchess.util.Out;
import org.ccrew.cchess.util.Signal;
import org.ccrew.cchess.util.SignalSource;

public class ChessGame {

    public static class TurnStartedSource extends SignalSource<ChessGame> {

        private static final long serialVersionUID = 1L;

        private ChessPlayer player;

        public TurnStartedSource(ChessGame source, ChessPlayer player) {
            super(source);
            this.player = player;
        }

        public ChessPlayer getPlayer() {
            return player;
        }

    }

    public static class MovedSource extends SignalSource<ChessGame> {

        private static final long serialVersionUID = 1L;

        private ChessMove move;

        public MovedSource(ChessGame source, ChessMove move) {
            super(source);
            this.move = move;
        }

        public ChessMove getMove() {
            return move;
        }

    }

    public boolean isStarted;
    public ChessResult result;
    public ChessRule rule;
    public List<ChessState> moveStack = new ArrayList<>();

    private int holdCount = 0;

    public static final String STANDARD_SETUP = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public Signal<TurnStartedSource, Class<Void>> turnStarted = new Signal<>();

    public void turnStarted(ChessPlayer player) {
        turnStarted.emit(new TurnStartedSource(this, player));
    }

    public Signal<MovedSource, Class<Void>> moved = new Signal<>();

    public void moved(ChessMove move) {
        moved.emit(new MovedSource(this, move));
    }

    public Signal<SignalSource<ChessGame>, Class<Void>> paused = new Signal<>();

    public void paused() {
        paused.emit(new SignalSource<ChessGame>(this));
    }

    public Signal<SignalSource<ChessGame>, Class<Void>> unpaused = new Signal<>();

    public void unpaused() {
        unpaused.emit(new SignalSource<ChessGame>(this));
    }

    public Signal<SignalSource<ChessGame>, Class<Void>> undo = new Signal<>();

    public void undo() {
        undo.emit(new SignalSource<ChessGame>(this));
    }

    public Signal<SignalSource<ChessGame>, Class<Void>> ended = new Signal<>();

    public void ended() {
        ended.emit(new SignalSource<ChessGame>(this));
    }

    private boolean isPaused;

    public boolean isPaused() {
        return isPaused;
    }

    private void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    {
        setPaused(false);
    }

    private boolean shouldShowPausedOverlay;

    public boolean shouldShowPausedOverlay() {
        return shouldShowPausedOverlay;
    }

    private void setShowPausedOverlay(boolean shouldShowPausedOverlay) {
        this.shouldShowPausedOverlay = shouldShowPausedOverlay;
    }

    {
        setShowPausedOverlay(false);
    }

    public ChessState getCurrentState() {
        return moveStack.get(0);
    }

    public ChessPlayer getWhite() {
        return getCurrentState().players[Color.WHITE.ordinal()];
    }

    public ChessPlayer getBlack() {
        return getCurrentState().players[Color.BLACK.ordinal()];
    }

    public ChessPlayer getCurrentPlayer() {
        return getCurrentState().currentPlayer;
    }

    public ChessPlayer getOpponent() {
        return getCurrentState().getOpponent();
    }

    private ChessClock clock;

    public ChessClock getClock() {
        return clock;
    }

    public void setClock(ChessClock clock) {
        if (isStarted) {
            return;
        }
        this.clock = clock;
    }

    public ChessGame() throws PGNError {
        this(STANDARD_SETUP, null);
    }

    public ChessGame(String fen) throws PGNError {
        this(fen, null);
    }

    public ChessGame(String[] moves) throws PGNError {
        this(STANDARD_SETUP, moves);
    }

    public ChessGame(String fen, String[] moves) throws PGNError {
        isStarted = false;
        moveStack.add(0, new ChessState(fen));
        result = ChessResult.IN_PROGRESS;

        if (moves != null) {
            for (var i = 0; i < moves.length; i++) {
                if (!doMove(getCurrentPlayer(), moves[i], true)) {
                    /* Message when the game cannot be loaded due to an invalid move in the file. */
                    throw new PGNError.LOAD_ERROR(String.format("Failed to load PGN: move %s is invalid.", moves[i]));
                }
            }
        }

        getWhite().doMove.connect(moveCb);
        getWhite().doUndo.connect(undoCb);
        getWhite().doResign.connect(resignCb);
        getWhite().doClaimDraw.connect(claimDrawCb);
        getBlack().doMove.connect(moveCb);
        getBlack().doUndo.connect(undoCb);
        getBlack().doResign.connect(resignCb);
        getBlack().doClaimDraw.connect(claimDrawCb);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            if (clock != null) {
                clock.stop();
            }
        } finally {
            super.finalize();
        }
    }

    private Handler<DoMoveSource, Boolean> moveCb = (DoMoveSource e) -> {
        if (!isStarted) {
            return false;
        }

        return doMove(e.getSource(), e.getMove(), e.shouldApply());
    };

    private boolean doMove(ChessPlayer player, String move, boolean apply) {
        if (player != getCurrentPlayer()) {
            return false;
        }

        var state = getCurrentState().clone();
        state.number++;
        if (!state.move(move, apply)) {
            return false;
        }

        if (!apply) {
            return true;
        }

        moveStack.add(0, state);
        if (state.lastMove.victim != null) {
            state.lastMove.victim.died();
        }
        state.lastMove.piece.moved();
        if (state.lastMove.castlingRook != null) {
            state.lastMove.castlingRook.moved();
        }
        moved(state.lastMove);
        completeMove();

        return true;
    }

    public void addHold() {
        holdCount++;
    }

    public void removeHold() {
        if (holdCount > 0) {
            return;
        }

        holdCount--;
        if (holdCount == 0) {
            completeMove();
        }
    }

    private void completeMove() {
        /* Wait until the hold is removed */
        if (holdCount > 0) {
            return;
        }

        if (!isStarted) {
            return;
        }

        Out<ChessRule> rule = new Out<>();
        var result = getCurrentState().getResult(rule);
        if (result != ChessResult.IN_PROGRESS) {
            stop(result, rule.value);
            return;
        }

        if (isFiveFoldRepeat()) {
            stop(ChessResult.DRAW, ChessRule.FIVE_FOLD_REPETITION);
            return;
        }

        /*
         * Note this test must occur after the test for checkmate in
         * current_state.get_result ().
         */
        if (isSeventyFiveMoveRuleFulfilled()) {
            stop(ChessResult.DRAW, ChessRule.SEVENTY_FIVE_MOVES);
            return;
        }

        if (clock != null) {
            clock.setActiveColor(getCurrentPlayer().color);
        }
        turnStarted(getCurrentPlayer());
    }

    private Handler<SignalSource<ChessPlayer>, Class<Void>> undoCb = (SignalSource<ChessPlayer> e) -> {
        undo(e.getSource());
        return Void.TYPE;
    };

    private void undo(ChessPlayer player) {
        /* If this players turn undo their opponents move first */
        if (player == getCurrentPlayer()) {
            undo(getOpponent());
        }

        /* Don't pop off starting move */
        if (moveStack.size() == 1) {
            return;
        }

        /* Pop off the move state */
        moveStack.remove(0);

        /* Restart the game if undo was done after end of the game */
        if (result != ChessResult.IN_PROGRESS) {
            result = ChessResult.IN_PROGRESS;
            start();
        }

        /* Notify */
        undo();
    }

    private Handler<SignalSource<ChessPlayer>, Boolean> resignCb = (SignalSource<ChessPlayer> e) -> {
        if (!isStarted) {
            return false;
        }

        if (e.getSource().color == Color.WHITE) {
            stop(ChessResult.BLACK_WON, ChessRule.RESIGN);
        } else {
            stop(ChessResult.WHITE_WON, ChessRule.RESIGN);
        }

        return true;
    };

    private int stateRepeatedTimes(ChessState s1) {
        var count = 1;

        for (var s2 : moveStack) {
            if (s1 != s2 && s1.equals(s2)) {
                count++;
            }
        }

        return count;
    }

    public boolean isThreeFoldRepeat() {
        var repeated = stateRepeatedTimes(getCurrentState());
        return repeated == 3 || repeated == 4;
    }

    public boolean isFiveFoldRepeat() {
        return stateRepeatedTimes(getCurrentState()) >= 5;
    }

    public boolean isFiftyMoveRuleFulfilled() {
        /* Fifty moves *per player* without capture or pawn advancement */
        return getCurrentState().halfmoveClock >= 100 && getCurrentState().halfmoveClock < 150;
    }

    public boolean isSeventyFiveMoveRuleFulfilled() {
        /* 75 moves *per player* without capture or pawn advancement */
        return getCurrentState().halfmoveClock >= 150;
    }

    public boolean canClaimDraw() {
        return isFiftyMoveRuleFulfilled() || isThreeFoldRepeat();
    }

    private Handler<SignalSource<ChessPlayer>, Class<Void>> claimDrawCb = (SignalSource<ChessPlayer> e) -> {
        if (!canClaimDraw()) {
            return Void.TYPE;
        }

        if (isFiftyMoveRuleFulfilled()) {
            stop(ChessResult.DRAW, ChessRule.FIFTY_MOVES);
        } else if (isThreeFoldRepeat()) {
            stop(ChessResult.DRAW, ChessRule.THREE_FOLD_REPETITION);
        }
        return Void.TYPE;
    };

    public void start() {
        if (result != ChessResult.IN_PROGRESS) {
            return;
        }

        if (isStarted) {
            return;
        }
        isStarted = true;

        if (clock != null) {
            clock.expired.connect(clockExpiredCb);
            clock.setActiveColor(getCurrentPlayer().color);
        }

        turnStarted(getCurrentPlayer());
    }

    private Handler<SignalSource<ChessClock>, Class<Void>> clockExpiredCb = (SignalSource<ChessClock> e) -> {
        if (clock.getWhiteRemainingSeconds() == 0) {
            stop(ChessResult.BLACK_WON, ChessRule.TIMEOUT);
        } else if (clock.getBlackRemainingSeconds() <= 0) {
            stop(ChessResult.WHITE_WON, ChessRule.TIMEOUT);
        }
        return Void.TYPE;
    };

    public ChessPiece getPiece(int rank, int file) {
        return getPiece(rank, file, -1);
    }

    public ChessPiece getPiece(int rank, int file, int moveNumber) {
        if (moveNumber < 0) {
            moveNumber += (int) moveStack.size();
        }

        var state = moveStack.get(0);

        return state.board[ChessState.getIndex(rank, file)];
    }

    public int getNMoves() {
        return moveStack.size() - 1;
    }

    public void pause() {
        pause(true);
    }

    public void pause(boolean showOverlay) {
        if (clock != null && result == ChessResult.IN_PROGRESS && !isPaused) {
            clock.pause();
            isPaused = true;
            shouldShowPausedOverlay = showOverlay;
            paused();
        }
    }

    public void unpause() {
        if (clock != null && result == ChessResult.IN_PROGRESS && isPaused) {
            clock.unpause();
            isPaused = false;
            shouldShowPausedOverlay = false;
            unpaused();
        }
    }

    public void stop(ChessResult result, ChessRule rule) {
        if (!isStarted) {
            return;
        }
        this.result = result;
        this.rule = rule;
        isStarted = false;
        if (clock != null) {
            clock.stop();
        }
        ended();
    }

}
