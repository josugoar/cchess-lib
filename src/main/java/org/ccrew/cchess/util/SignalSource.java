package org.ccrew.cchess.util;

import java.util.EventObject;

public class SignalSource<T> extends EventObject {

    private static final long serialVersionUID = 1L;

    public SignalSource(T source) {
        super(source);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getSource() {
        return (T) super.getSource();
    }

}
