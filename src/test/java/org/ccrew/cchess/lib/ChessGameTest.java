package org.ccrew.cchess.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ccrew.cchess.util.Out;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ChessGameTest {

    private static void testGoodMove(String fen, String move, String resultFen) {
        testGoodMove(fen, move, resultFen, ChessResult.IN_PROGRESS, ChessRule.CHECKMATE, false);
    }

    private static void testGoodMove(String fen, String move, String resultFen, ChessResult result) {
        testGoodMove(fen, move, resultFen, result, ChessRule.CHECKMATE, false);
    }

    private static void testGoodMove(String fen, String move, String resultFen, ChessResult result, ChessRule rule) {
        testGoodMove(fen, move, resultFen, result, rule, false);
    }

    protected static void testGoodMove(String fen, String move, String resultFen, ChessResult result, ChessRule rule,
            boolean verifySAN) {
        ChessState state = new ChessState(fen);
        assertTrue(state.move(move), String.format("%s + %s is an invalid move", fen, move));

        assertEquals(resultFen, state.getFen(),
                String.format("%s + %s has state %s not %s", fen, move, state.getFen(), resultFen));

        // We don't typically want to test this since get_san returns exactly one
        // canonical SAN,
        // but some test cases want to verify that slightly different notations are
        // accepted.
        if (verifySAN) {
            assertEquals(move, state.lastMove.getSan(),
                    String.format("%s + %s has SAN move %s", fen, move, state.lastMove.getSan()));
        }

        Out<ChessRule> moveRule = new Out<>();
        var moveResult = state.getResult(moveRule);
        assertEquals(result, moveResult, String.format("%s + %s has result %s not %s", fen, move, moveResult, result));
        assertEquals(rule, moveRule.value,
                String.format("%s + %s has result %s not %s", fen, move, moveResult, result));
    }

    protected static void testBadMove(String fen, String move) {
        ChessState state = new ChessState(fen);
        assertFalse(state.move(move, false), String.format("%s + %s is valid", fen, move));
    }

    @Test
    public void testPawnMove() {
        /* Pawn move */
        testGoodMove("8/8/8/8/8/8/P7/8 w - - 0 1", "a3", "8/8/8/8/8/P7/8/8 b - - 0 1");
    }

    @Test
    public void testPawnMarch() {
        /* Pawn march */
        testGoodMove("8/8/8/8/8/8/P7/8 w - - 0 1", "a4", "8/8/8/8/P7/8/8/8 b - a3 0 1");
    }

    @Test
    public void testPawnMarchOnlyAllowedFromBaseline() {
        /* Pawn march only allowed from baseline */
        testBadMove("8/8/8/8/8/P7/8/8 w - - 0 1", "a2a5");
    }

    @Test
    public void testPawnPromotion() {
        /* Pawn promotion */
        testGoodMove("8/P7/8/8/8/8/8/8 w - - 0 1", "a8=Q", "Q7/8/8/8/8/8/8/8 b - - 0 1");
        testGoodMove("8/P7/8/8/8/8/8/8 w - - 0 1", "a7a8q", "Q7/8/8/8/8/8/8/8 b - - 0 1");
        testGoodMove("8/P7/8/8/8/8/8/8 w - - 0 1", "a7a8N", "N7/8/8/8/8/8/8/8 b - - 0 1");
    }

    @Test
    public void testEnPassant() {
        /* En passant */
        testGoodMove("8/8/8/pP6/8/8/8/8 w - a6 0 1", "bxa6", "8/8/P7/8/8/8/8/8 b - - 0 1");
    }

    @Test
    public void testNotEnPassantIfNotAllowed() {
        /* Can't en passant if wasn't allowed */
        testBadMove("8/8/8/pP6/8/8/8/8 w - - 0 1", "b5a6");
    }

    @Test
    public void testNotCaptureEnPassantUnlessPawn() {
        /* Can't capture en passant unless we are a pawn */
        testGoodMove("8/8/8/pQ6/8/8/8/8 w - a6 0 1", "Qa6", "8/8/Q7/p7/8/8/8/8 b - - 1 1");
        testBadMove("8/8/8/pQ6/8/8/8/8 w - a6 0 1", "bxa6");
    }

    @Test
    public void testCastleKingside() {
        /* Castle kingside */
        testGoodMove("8/8/8/8/8/8/8/4K2R w K - 0 1", "O-O", "8/8/8/8/8/8/8/5RK1 b - - 1 1");
    }

    @Test
    public void testCastleQueenside() {
        /* Castle queenside */
        testGoodMove("8/8/8/8/8/8/8/R3K3 w Q - 0 1", "O-O-O", "8/8/8/8/8/8/8/2KR4 b - - 1 1");
    }

    @Test
    public void testNotCastleIfPiecesMoved() {
        /* Can't castle if pieces moved */
        testBadMove("8/8/8/8/8/8/8/4K2R w - - 0 1", "O-O");
    }

    @Test
    public void testNotCastleIfPieceMisplaced() {
        /*
         * Can't castle if piece misplaced (shouldn't occur as then the castle flag
         * would not be there)
         */
        testBadMove("8/8/8/8/8/8/8/4K3 w K - 0 1", "O-O");
        testBadMove("8/8/8/8/8/8/8/5K2 w K - 0 1", "O-O");
    }

    @Test
    public void testNotCastleWhenInCheck() {
        /* Can't castle when in check */
        testBadMove("4r3/8/8/8/8/8/8/4K2R w K - 0 1", "O-O");
    }

    @Test
    public void testNotMoveAcrossSquareIntoCheck() {
        /* Can't move across square that would put into check */
        testBadMove("5r2/8/8/8/8/8/8/4K2R w K - 0 1", "O-O");
        testBadMove("8/8/8/8/8/8/6p1/4K2R w K - 0 1", "O-O");
        testBadMove("8/8/8/8/8/8/4p3/R3K3 w Q - 0 1", "O-O-O");
    }

    @Test
    public void testNotMoveIntoCheck() {
        /* Can't move into check */
        testBadMove("4r3/8/8/8/8/8/4R3/4K3 w - - 0 1", "e2f2");
    }

    @Test
    public void testCheck() {
        /* Check */
        testGoodMove("k7/8/8/8/8/8/8/1R6 w - - 0 1", "Ra1+", "k7/8/8/8/8/8/8/R7 b - - 1 1");
    }

    @Test
    public void testCheckmate() {
        /* Checkmate */
        testGoodMove("k7/8/8/8/8/8/1R6/1R6 w - - 0 1", "Ra1#", "k7/8/8/8/8/8/1R6/R7 b - - 1 1", ChessResult.WHITE_WON,
                ChessRule.CHECKMATE);
    }

    @Test
    public void testNotCheckmate() {
        /* Not checkmate (piece can be moved to intercept) */
        testGoodMove("k7/7r/8/8/8/8/1R6/1R6 w - - 0 1", "Ra1+", "k7/7r/8/8/8/8/1R6/R7 b - - 1 1",
                ChessResult.IN_PROGRESS);
    }

    @Test
    public void testStalemate() {
        /* Stalemate */
        testGoodMove("k7/8/7R/8/8/8/8/1R6 w - - 0 1", "Rh7", "k7/7R/8/8/8/8/8/1R6 b - - 1 1", ChessResult.DRAW,
                ChessRule.STALEMATE);
    }

    @Test
    public void testInsufficientMaterialKingVsKing() {
        /* Insufficient material - King vs. King */
        testGoodMove("k7/7p/7K/8/8/8/8/8 w - - 0 1", "Kxh7", "k7/7K/8/8/8/8/8/8 b - - 0 1", ChessResult.DRAW,
                ChessRule.INSUFFICIENT_MATERIAL);
    }

    @Test
    public void testInsufficientMaterialKingAndKnightVsKing() {
        /* Insufficient material - King and knight vs. King */
        testGoodMove("k7/7p/7K/8/8/8/8/7N w - - 0 1", "Kxh7", "k7/7K/8/8/8/8/8/7N b - - 0 1", ChessResult.DRAW,
                ChessRule.INSUFFICIENT_MATERIAL);
        /* Sufficient if a knight on each side */
        testGoodMove("k7/7p/7K/8/8/8/8/6Nn w - - 0 1", "Kxh7", "k7/7K/8/8/8/8/8/6Nn b - - 0 1",
                ChessResult.IN_PROGRESS);
        /* A bishop would suffice as well */
        testGoodMove("k7/7p/7K/8/8/8/8/6Nb w - - 0 1", "Kxh7", "k7/7K/8/8/8/8/8/6Nb b - - 0 1",
                ChessResult.IN_PROGRESS);
    }

    @Test
    public void testInsufficientMaterialKingAndNSameColorBishopsVsKingAndNSameColorBishops() {
        /*
         * Insufficient material - King and n same-color bishops vs. King and n
         * same-color bishops
         */
        testGoodMove("k2b1b1b/6bp/7K/4B1B1/8/8/8/8 w - - 0 1", "Kxh7", "k2b1b1b/6bK/8/4B1B1/8/8/8/8 b - - 0 1",
                ChessResult.DRAW, ChessRule.INSUFFICIENT_MATERIAL);
        /* Sufficient if the players' bishops are on opposite colors */
        testGoodMove("k2b1b1b/6bp/7K/5B1B/8/8/8/8 w - - 0 1", "Kxh7", "k2b1b1b/6bK/8/5B1B/8/8/8/8 b - - 0 1",
                ChessResult.IN_PROGRESS);
        /* Still sufficient with only one bishop per player, opposite colors */
        testGoodMove("k6b/6bp/7K/7B/8/8/8/8 w - - 0 1", "Kxh7", "k6b/6bK/8/7B/8/8/8/8 b - - 0 1",
                ChessResult.IN_PROGRESS);
        /* Sufficient if one player has a bishop on each color */
        testGoodMove("k2b1b1b/6bp/7K/6BB/8/8/8/8 w - - 0 1", "Kxh7", "k2b1b1b/6bK/8/6BB/8/8/8/8 b - - 0 1",
                ChessResult.IN_PROGRESS);
    }

    @Disabled
    @Test
    public void testClaimDrawDueTo50MoveRule() {
        testGoodMove("p7/8/8/8/8/8/8/P7 w - - 100 1", "draw", "p7/8/8/8/8/8/8/P7 w - - 100 1", ChessResult.DRAW,
                ChessRule.FIFTY_MOVES);
    }

    @Test
    public void testNeed100HalfmovesFor50MoveRule() {
        testBadMove("p7/8/8/8/8/8/8/P7 w - - 99 1", "draw");
    }

}
