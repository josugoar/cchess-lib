package org.ccrew.cchess.lib;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class PGNGame {

    /*
     * This is the Seven Tag Roster (STR). They have to appear at the top, in this
     * order.
     */
    private int strIndex(String name) {
        if (name == "Event") {
            return 0;
        } else if (name == "Site") {
            return 1;
        } else if (name == "Date") {
            return 2;
        } else if (name == "Round") {
            return 3;
        } else if (name == "White") {
            return 4;
        } else if (name == "Black") {
            return 5;
        } else if (name == "Result") {
            return 6;
        } else {
            return 7;
        }
    }

    private Comparator<String> compareTag = (String name0, String name1) -> {
        int strIndex0 = strIndex(name0);
        int strIndex1 = strIndex(name1);

        /* If both not in STR then just order alphabetically */
        if (strIndex0 == 7 && strIndex1 == 7) {
            return name0.compareTo(name1);
        } else {
            return strIndex0 - strIndex1;
        }
    };

    public HashMap<String, String> tags;
    public List<String> moves = new ArrayList<>();

    public static final String RESULT_IN_PROGRESS = "*";
    public static final String RESULT_DRAW = "1/2-1/2";
    public static final String RESULT_WHITE = "1-0";
    public static final String RESULT_BLACK = "0-1";

    public static final String TERMINATE_ABANDONED = "abandoned";
    public static final String TERMINATE_ADJUDICATION = "adjudication";
    public static final String TERMINATE_DEATH = "death";
    public static final String TERMINATE_EMERGENCY = "emergency";
    public static final String TERMINATE_NORMAL = "normal";
    public static final String TERMINATE_RULES_INFRACTION = "rules infraction";
    public static final String TERMINATE_TIME_FORFEIT = "time forfeit";
    public static final String TERMINATE_UNTERMINATED = "unterminated";

    public String getEvent() {
        return tags.get("Event");
    }

    public void setEvent(String event) {
        tags.put("Event", event);
    }

    public String getSite() {
        return tags.get("Site");
    }

    public void setSite(String site) {
        tags.put("Site", site);
    }

    public String getDate() {
        return tags.get("Date");
    }

    public void setDate(String date) {
        tags.put("Date", date);
    }

    public String getTime() {
        return tags.get("Time");
    }

    public void setTime(String time) {
        tags.put("Time", time);
    }

    public String getRound() {
        return tags.get("Round");
    }

    public void setRound(String round) {
        tags.put("Round", round);
    }

    public String getWhite() {
        return tags.get("White");
    }

    public void setWhite(String white) {
        tags.put("White", white);
    }

    public String getBlack() {
        return tags.get("Black");
    }

    public void setBlack(String black) {
        tags.put("Black", black);
    }

    public String getResult() {
        return tags.get("Result");
    }

    public void setResult(String result) {
        tags.put("Result", result);
    }

    public String getAnnotator() {
        return tags.get("Annotator");
    }

    public void setAnnotator(String annotator) {
        tags.put("Annotator", annotator);
    }

    public String getTimeControl() {
        return tags.get("TimeControl");
    }

    public void setTimeControl(String timeControl) {
        tags.put("TimeControl", timeControl);
    }

    public String getWhiteTimeLeft() {
        return tags.get("X-GNOME-WhiteTimeLeft");
    }

    public void setWhiteTimeLeft(String whiteTimeLeft) {
        tags.put("X-GNOME-WhiteTimeLeft", whiteTimeLeft);
    }

    public String getBlackTimeLeft() {
        return tags.get("X-GNOME-BlackTimeLeft");
    }

    public void setBlackTimeLeft(String blackTimeLeft) {
        tags.put("X-GNOME-BlackTimeLeft", blackTimeLeft);
    }

    public String getClockType() {
        return tags.get("X-GNOME-ClockType");
    }

    public void setClockType(String clockType) {
        tags.put("X-GNOME-ClockType", clockType);
    }

    public String getTimerIncrement() {
        return tags.get("X-GNOME-TimerIncrement");
    }

    public void setTimerIncrement(String timerIncrement) {
        tags.put("X-GNOME-TimerIncrement", timerIncrement);
    }

    public boolean getSetUp() {
        String v = tags.get("SetUp");
        return v != null && v.equals("1") ? true : false;
    }

    public void setSetUp(boolean setUp) {
        tags.put("SetUp", setUp ? "1" : "0");
    }

    public String getFen() {
        return tags.get("FEN");
    }

    public void setFen(String fen) {
        tags.put("FEN", fen);
    }

    public String getTermination() {
        return tags.get("Termination");
    }

    public void setTermination(String termination) {
        tags.put("Termination", termination);
    }

    public String getWhiteAi() {
        return tags.get("X-GNOME-WhiteAI");
    }

    public void setWhiteAi(String whiteAi) {
        tags.put("X-GNOME-WhiteAI", whiteAi);
    }

    public String getWhiteLevel() {
        return tags.get("X-GNOME-WhiteLevel");
    }

    public void setWhiteLevel(String whiteLevel) {
        tags.put("X-GNOME-WhiteLevel", whiteLevel);
    }

    public String getBlackAi() {
        return tags.get("X-GNOME-BlackAI");
    }

    public void setBlackAi(String blackAi) {
        tags.put("X-GNOME-BlackAI", blackAi);
    }

    public String getBlackLevel() {
        return tags.get("X-GNOME-BlackLevel");
    }

    public void setBlackLevel(String blackLevel) {
        tags.put("X-GNOME-BlackLevel", blackLevel);
    }

    public PGNGame() {
        tags = new HashMap<String, String>();
        tags.put("Event", "?");
        tags.put("Site", "?");
        tags.put("Date", "????.??.??");
        tags.put("Round", "?");
        tags.put("White", "?");
        tags.put("Black", "?");
        tags.put("Result", PGNGame.RESULT_IN_PROGRESS);
    }

    public static String escape(String value) {
        var a = value.replace("\\", "\\\\");
        return a.replace("\"", "\\\"");
    }

    public void write(File file) throws Exception {
        var data = new StringBuilder();

        var keys = new ArrayList<>(tags.keySet());
        keys.sort(compareTag);
        for (var key : keys) {
            data.append(String.format("[%s \"%s\"]\n", key, escape(tags.get(key))));
        }
        data.append("\n");

        int i = 0;
        for (String move : moves) {
            if (i % 2 == 0) {
                data.append(String.format("%d. ", i / 2 + 1));
            }
            data.append(move);
            data.append(" ");
            i++;
        }
        data.append(getResult());
        data.append("\n");

        Files.writeString(file.toPath(), data.toString());
    }

}
