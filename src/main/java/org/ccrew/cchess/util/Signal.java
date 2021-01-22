package org.ccrew.cchess.util;

import java.util.EventObject;
import java.util.HashSet;

public class Signal<T extends EventObject, S> {

    private HashSet<Handler<T, S>> handlers = new HashSet<>();

    public long connect(Handler<T, S> handler) {
        handlers.add(handler);
        return handler.hashCode();
    }

    public void disconnect(Handler<T, S> handler) {
        handlers.remove(handler);
    }

    public void disconnect(long handlerId) {
        for (Handler<T, S> handler : handlers) {
            if (handler.hashCode() == handlerId) {
                handlers.remove(handler);
                return;
            }
        }
    }

    public S emit(T e) {
        S value = null;
        for (Handler<T, S> handler : handlers) {
            value = handler.handle(e);
        }
        return value;
    }

}
