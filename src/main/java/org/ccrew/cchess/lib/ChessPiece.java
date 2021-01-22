package org.ccrew.cchess.lib;

import org.ccrew.cchess.util.Signal;
import org.ccrew.cchess.util.SignalSource;

public class ChessPiece {

    public ChessPlayer player;
    public PieceType type;

    public Signal<SignalSource<ChessPiece>, Class<Void>> moved = new Signal<>();

    public void moved() {
        moved.emit(new SignalSource<ChessPiece>(this));
    }

    public Signal<SignalSource<ChessPiece>, Class<Void>> promoted = new Signal<>();

    public void promoted() {
        promoted.emit(new SignalSource<ChessPiece>(this));
    }

    public Signal<SignalSource<ChessPiece>, Class<Void>> died = new Signal<>();

    public void died() {
        died.emit(new SignalSource<ChessPiece>(this));
    }

    public Color getColor() {
        return player.color;
    }

    public char getSymbol() {
        char c = ' ';
        switch (type) {
            case PAWN:
                c = 'p';
                break;
            case ROOK:
                c = 'r';
                break;
            case KNIGHT:
                c = 'n';
                break;
            case BISHOP:
                c = 'b';
                break;
            case QUEEN:
                c = 'q';
                break;
            case KING:
                c = 'k';
                break;
        }
        if (player.color == Color.WHITE) {
            c = Character.toUpperCase(c);
        }
        return c;
    }

    public ChessPiece(ChessPlayer player, PieceType type) {
        this.player = player;
        this.type = type;
    }

}
