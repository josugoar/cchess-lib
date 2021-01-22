package org.ccrew.cchess.util;

public final class AssertNotReached extends Error {

    private static final long serialVersionUID = 1L;

    private AssertNotReached() {
    }

    public static AssertNotReached assertNotReached() {
        return new AssertNotReached();
    }

}
