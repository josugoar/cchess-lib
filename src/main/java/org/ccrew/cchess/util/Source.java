package org.ccrew.cchess.util;

public final class Source {

    private Source() {
    }

    public static boolean remove(int id) {
        return Timeout.remove(id);
    }

}
