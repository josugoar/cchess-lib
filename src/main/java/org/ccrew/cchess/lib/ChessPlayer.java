package org.ccrew.cchess.lib;

import org.ccrew.cchess.util.Signal;
import org.ccrew.cchess.util.SignalSource;

public class ChessPlayer {

    public static class DoMoveSource extends SignalSource<ChessPlayer> {

        private static final long serialVersionUID = 1L;

        private String move;

        private boolean apply;

        public DoMoveSource(ChessPlayer source, String move, boolean apply) {
            super(source);
            this.move = move;
            this.apply = apply;
        }

        public String getMove() {
            return move;
        }

        public boolean shouldApply() {
            return apply;
        }

    }

    public Color color;

    public Signal<DoMoveSource, Boolean> doMove = new Signal<>();

    public boolean doMove(String move, boolean apply) {
        return doMove.emit(new DoMoveSource(this, move, apply));
    }

    public Signal<SignalSource<ChessPlayer>, Class<Void>> doUndo = new Signal<>();

    public void doUndo() {
        doUndo.emit(new SignalSource<ChessPlayer>(this));
    }

    public Signal<SignalSource<ChessPlayer>, Boolean> doResign = new Signal<>();

    public boolean doResign() {
        return doResign.emit(new SignalSource<ChessPlayer>(this));
    }

    public Signal<SignalSource<ChessPlayer>, Class<Void>> doClaimDraw = new Signal<>();

    public void doClaimDraw() {
        doClaimDraw.emit(new SignalSource<ChessPlayer>(this));
    }

    private boolean localHuman = false;

    public boolean isLocalHuman() {
        return localHuman;
    }

    public void setLocalHuman(boolean localHuman) {
        this.localHuman = localHuman;
    }

    public ChessPlayer(Color color) {
        this.color = color;
    }

    public boolean move(String move) {
        return move(move, true);
    }

    public boolean move(String move, boolean apply) {
        return doMove(move, apply);
    }

    public boolean moveWithCoords(int r0, int f0, int r1, int f1) {
        return moveWithCoords(r0, f0, r1, f1, true, PieceType.QUEEN);
    }

    public boolean moveWithCoords(int r0, int f0, int r1, int f1, boolean apply) {
        return moveWithCoords(r0, f0, r1, f1, apply, PieceType.QUEEN);
    }

    public boolean moveWithCoords(int r0, int f0, int r1, int f1, PieceType promotionType) {
        return moveWithCoords(r0, f0, r1, f1, true, promotionType);
    }

    public boolean moveWithCoords(int r0, int f0, int r1, int f1, boolean apply, PieceType promotionType) {
        String move = String.format("%c%d%c%d", 'a' + f0, r0 + 1, 'a' + f1, r1 + 1);

        switch (promotionType) {
            case QUEEN:
                /* Default is queen so don't add anything */
                break;
            case KNIGHT:
                move += "=N";
                break;
            case ROOK:
                move += "=R";
                break;
            case BISHOP:
                move += "=B";
                break;
            default:
                break;
        }

        return doMove(move, apply);
    }

    public void undo() {
        doUndo();
    }

    public boolean resign() {
        return doResign();
    }

    public void claimDraw() {
        doClaimDraw();
    }

}
