package org.ccrew.cchess.lib;

public class PGNError extends Exception {

    private static final long serialVersionUID = 1L;

    private PGNError(String message) {
        super(message);
    }

    public static class LOAD_ERROR extends PGNError {

        private static final long serialVersionUID = 1L;

        public LOAD_ERROR(String message) {
            super(message);
        }

    }

}
