package org.ccrew.cchess.util;

import java.util.EventListener;
import java.util.EventObject;

@FunctionalInterface
public interface Handler<T extends EventObject, S> extends EventListener {

    public S handle(T e);

}
