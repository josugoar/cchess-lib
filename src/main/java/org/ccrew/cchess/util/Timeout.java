package org.ccrew.cchess.util;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public final class Timeout {

    private static Timer timer = new Timer();

    private static HashMap<Integer, TimerTask> functions = new HashMap<>();

    private Timeout() {
    }

    public static int add(int interval, Supplier<TimerTask> supplier) {
        TimerTask function = supplier.get();
        int id = function.hashCode();
        functions.put(id, function);
        timer.scheduleAtFixedRate(function, 0, 1000);
        return id;
    }

    public static boolean remove(int id) {
        TimerTask function = functions.remove(id);
        if (function == null) {
            return false;
        }
        function.cancel();
        return true;
    }

}
