package org.ccrew.cchess.util;

import java.util.logging.Logger;

public final class Logging {

    private static Logger logger = Logger.getGlobal();

    private Logging() {
    }

    public static void debug(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    public static void warning(String format, Object... args) {
        logger.warning(String.format(format, args));
    }

}
