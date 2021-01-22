package org.ccrew.cchess.lib;

import static org.ccrew.cchess.util.Logging.warning;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PGN {

    public List<PGNGame> games = new ArrayList<>();

    private static void insertTag(PGNGame game, String tagName, String tagValue) {
        switch (tagName) {
            case "TimeControl":
                try {
                    Long.parseLong(tagValue);
                    game.tags.put("TimeControl", tagValue);
                } catch (NumberFormatException e) {
                    warning(String.format("Invalid %s : %s in PGN, setting timer to infinity.", tagName, tagValue));
                }
                break;
            case "WhiteTimeLeft":
                // Fall through
            case "X-GNOME-WhiteTimeLeft":
                try {
                    Long.parseLong(tagValue);
                    game.tags.put("X-GNOME-WhiteTimeLeft", tagValue);
                } catch (NumberFormatException e) {
                    warning(String.format("Invalid %s : %s in PGN, setting timer to infinity.", tagName, tagValue));
                }
                break;
            case "BlackTimeLeft":
                // Fall through
            case "X-GNOME-BlackTimeLeft":
                try {
                    Long.parseLong(tagValue);
                    game.tags.put("X-GNOME-BlackTimeLeft", tagValue);
                } catch (NumberFormatException e) {
                    warning(String.format("Invalid %s : %s in PGN, setting timer to infinity.", tagName, tagValue));
                }
                break;
            case "X-GNOME-ClockType":
                if (ClockType.stringToEnum(tagValue) == ClockType.INVALID) {
                    warning(String.format("Invalid clock type in PGN: %s, using a simple clock.", tagValue));
                    game.tags.put("X-GNOME-ClockType", "simple");
                }
                break;
            case "X-GNOME-TimerIncrement":
                try {
                    Long.parseLong(tagValue);
                } catch (NumberFormatException e) {
                    warning(String.format("Invalid timer increment in PGN: %s, using a simple clock.", tagValue));
                    game.tags.put("X-GNOME-ClockType", "simple");
                    game.tags.put("X-GNOME-TimerIncrement", "0");
                }
                break;
            case "WhiteAI":
                // Fall through
            case "X-GNOME-WhiteAI":
                game.tags.put("X-GNOME-WhiteAI", tagValue);
                break;
            case "WhiteLevel":
                // Fall through
            case "X-GNOME-WhiteLevel":
                game.tags.put("X-GNOME-WhiteLevel", tagValue);
                break;
            case "BlackAI":
                // Fall through
            case "X-GNOME-BlackAI":
                game.tags.put("X-GNOME-BlackAI", tagValue);
                break;
            case "BlackLevel":
                // Fall through
            case "X-GNOME-BlackLevel":
                game.tags.put("X-GNOME-BlackLevel", tagValue);
                break;
            default:
                game.tags.put(tagName, tagValue);
                break;
        }
    }

    public PGN(String data) throws PGNError {
        // Fix for Java String not having null character
        if (data.length() > 0 && !Character.isWhitespace(data.charAt(data.length() - 1))) {
            data += " ";
        }

        State state = State.TAGS;
        State homeState = State.TAGS;
        PGNGame game = new PGNGame();
        boolean inEscape = false;
        int tokenStart = 0;
        int lineOffset = 0;
        String tagName = "";
        StringBuilder tagValue = new StringBuilder();
        int line = 1;
        int ravLevel = 0;
        for (int offset = 0; offset < data.length(); offset++) {
            char c = data.charAt(offset);

            if (c == '\n') {
                line++;
                lineOffset = offset + 1;
            }

            switch (state) {
                case TAGS:
                    homeState = State.TAGS;
                    if (c == ';') {
                        state = State.LINE_COMMENT;
                    } else if (c == '{') {
                        state = State.BRACE_COMMENT;
                    } else if (c == '[') {
                        state = State.TAG_START;
                    } else if (!Character.isWhitespace(c)) {
                        offset--;
                        state = State.MOVE_TEXT;
                        continue;
                    }
                    break;

                case MOVE_TEXT:
                    homeState = State.TAGS;
                    if (c == ';') {
                        state = State.LINE_COMMENT;
                    } else if (c == '{') {
                        state = State.BRACE_COMMENT;
                    } else if (c == '*') {
                        if (ravLevel == 0) {
                            game.setResult(PGNGame.RESULT_IN_PROGRESS);
                            games.add(game);
                            game = new PGNGame();
                            state = State.TAGS;
                        }
                    } else if (c == '.') {
                        offset--;
                        state = State.PERIOD;
                    } else if (Character.isLetterOrDigit(c)) {
                        tokenStart = offset;
                        state = State.SYMBOL;
                    } else if (c == '$') {
                        tokenStart = offset + 1;
                        state = State.NAG;
                    } else if (c == '(') {
                        ravLevel++;
                        continue;
                    } else if (c == ')') {
                        if (ravLevel == 0) {
                            state = State.ERROR;
                        } else {
                            ravLevel--;
                        }
                    } else if (!Character.isWhitespace(c)) {
                        state = State.ERROR;
                    }
                    break;

                case LINE_COMMENT:
                    if (c == '\n') {
                        state = homeState;
                    }
                    break;

                case BRACE_COMMENT:
                    if (c == '}') {
                        state = homeState;
                    }
                    break;

                case TAG_START:
                    if (Character.isWhitespace(c)) {
                        continue;
                    } else if (Character.isLetterOrDigit(c)) {
                        tokenStart = offset;
                        state = State.TAG_NAME;
                    } else {
                        state = State.ERROR;
                    }
                    break;

                case TAG_NAME:
                    if (Character.isWhitespace(c)) {
                        tagName = data.substring(tokenStart, offset);
                        state = State.PRE_TAG_VALUE;
                    } else if (Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '#' || c == '=' || c == ':'
                            || c == '-') {
                        continue;
                    } else {
                        state = State.ERROR;
                    }
                    break;

                case PRE_TAG_VALUE:
                    if (Character.isWhitespace(c)) {
                        continue;
                    } else if (c == '"') {
                        state = State.TAG_VALUE;
                        // Fix for empty StringBuilder
                        if (tagValue.length() > 0) {
                            tagValue.delete(0, tagValue.length() - 1);
                        }
                        inEscape = false;
                    } else {
                        state = State.ERROR;
                    }
                    break;

                case TAG_VALUE:
                    if (c == '\\' && !inEscape) {
                        inEscape = true;
                    } else if (c == '"' && !inEscape) {
                        state = State.POST_TAG_VALUE;
                    } else if (Character.isDefined(c)) {
                        tagValue.append(c);
                        inEscape = false;
                    } else {
                        state = State.ERROR;
                    }
                    break;

                case POST_TAG_VALUE:
                    if (Character.isWhitespace(c)) {
                        continue;
                    } else if (c == ']') {
                        insertTag(game, tagName, tagValue.toString());
                        state = State.TAGS;
                    } else {
                        state = State.ERROR;
                    }
                    break;

                case SYMBOL:
                    /* NOTE: '/' not in spec but required for 1/2-1/2 symbol */
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '#' || c == '=' || c == ':'
                            || c == '-' || c == '/') {
                        continue;
                    } else {
                        String symbol = data.substring(tokenStart, offset);

                        boolean isNumber = true;
                        for (int i = 0; i < symbol.length(); i++) {
                            if (!Character.isDigit(symbol.charAt(i))) {
                                isNumber = false;
                            }
                        }

                        state = State.MOVE_TEXT;
                        offset--;

                        /* Game termination markers */
                        if (symbol == PGNGame.RESULT_DRAW || symbol == PGNGame.RESULT_WHITE
                                || symbol == PGNGame.RESULT_BLACK) {
                            if (ravLevel == 0) {
                                game.setResult(symbol);
                                games.add(game);
                                game = new PGNGame();
                                state = State.TAGS;
                            }
                        } else if (!isNumber) {
                            if (ravLevel == 0) {
                                game.moves.add(symbol);
                            }
                        }
                    }
                    break;

                case PERIOD:
                    state = State.MOVE_TEXT;
                    break;

                case NAG:
                    if (Character.isDigit(c)) {
                        continue;
                    } else {
                        state = State.MOVE_TEXT;
                        offset--;
                    }
                    break;

                case ERROR:
                    int charOffset = offset - lineOffset - 1;
                    System.err.printf("%d.%d: error: Unexpected character\n", line, (int) (charOffset + 1));
                    System.err.printf("%s\n", data.substring(lineOffset, offset));
                    for (int i = 0; i < charOffset; i++) {
                        System.err.printf(" ");
                    }
                    System.err.printf("^\n");
                    return;
            }
        }

        if (game.moves.size() > 0 || game.tags.size() > 0) {
            games.add(game);
        }

        /* Must have at least one game */
        if (games.size() == 0) {
            throw new PGNError.LOAD_ERROR("No games in PGN file");
        }
    }

    public PGN(File file) throws Exception {
        this(Files.readString(file.toPath()));
    }

}
