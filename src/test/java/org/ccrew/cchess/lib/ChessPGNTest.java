package org.ccrew.cchess.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class ChessPGNTest {

    private static void testPGNFile(String data, String moves) {
        PGN file;
        try {
            file = new PGN(data);
        } catch (PGNError e) {
            fail(e.getMessage());
            return;
        }

        var game = file.games.get(0);
        var moveString = "";
        for (var move : game.moves) {
            moveString += String.format("%s ", move);
        }
        moveString = moveString.strip();

        assertEquals(moves, moveString);
    }

    @Test
    public void testSimpleFileInExportFormat() {
        /* Simple file in export format */
        testPGNFile("[Event \"?\"]\n" + "[Site \"?\"]\n" + "[Date \"????.??.??\"]\n" + "[Round \"?\"]\n"
                + "[White \"\"]\n" + "[Black \"\"]\n" + "[Result \"*\"]\n" + "\n" + "1. *\n", "");
    }

    @Test
    public void testNoTags() {
        /* No tags */
        testPGNFile("1. e1 *\n", "e1");
    }

    @Test
    public void testNoMoveNumbers() {
        /* No move numbers */
        testPGNFile("e1 *\n", "e1");
    }

    @Test
    public void testNoMoveResult() {
        /* No move result */
        testPGNFile("e1\n", "e1");
    }

    @Test
    public void testNoTrailingNewline() {
        /* No trailing newline */
        testPGNFile("e1", "e1");
    }

    @Test
    public void testCarriageReturnsInsteadOfNewlines() {
        /* Carriage returns instead of newlines */
        testPGNFile("[Event \"?\"]\r" + "\r" + "1. d4 *\r", "d4");
    }

    @Test
    public void testComments() {
        /* Comments */
        testPGNFile("; Line comment 1\n" + "[Event \"?\"]\n" + "; Line comment 2\n" + "\n"
                + "1. e4 {First Move} e5 {Multi\n" + "line\n"
                + "comment} 2. Nc3 {More comments} * {Comment about game end}\n", "e4 e5 Nc3");
    }

    @Test
    public void testFormatUsedByYahooChess() {
        /* Format used by Yahoo Chess */
        testPGNFile(";Title: Yahoo! Chess Game\n" + ";White: roovis\n" + ";Black: ladyjones96\n"
                + ";Date: Fri Oct 19 12:51:54 GMT 2007\n" + "\n" + "1. e2-e4 e7-e5\n", "e2-e4 e7-e5");
    }

    @Test
    public void testRecursiveAnnotationVariation() {
        /* Recursive Annotation Variation */
        testPGNFile("1.Ra8+ (1.Bxd6+ Kb7 2.Rc7+ Kb8 (2...Kb6 3.Ra6#) 3.Rd7+ Kc8 4.Rc1# (4.Ra8#))", "Ra8+");
    }

    @Test
    public void testNumericAnnotationGlyph() {
        /* Numeric Annotation Glyph */
        testPGNFile("e4 e5 $1 Nc3 $2", "e4 e5 Nc3");
    }

}
