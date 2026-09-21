package com.ultraop.nametag.api;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-process event bus for domain-level NameTag events.
 */
public final class TagEventBus {
    private final CopyOnWriteArrayList<TagEventListener> listeners = new CopyOnWriteArrayList<>();

    public void register(TagEventListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    public void unregister(TagEventListener listener) {
        listeners.remove(listener);
    }

    public int listenerCount() {
        return listeners.size();
    }

    public void publish(TagEvent event) {
        Objects.requireNonNull(event, "event");
        for (TagEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
