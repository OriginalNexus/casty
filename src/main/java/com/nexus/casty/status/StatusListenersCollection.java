package com.nexus.casty.status;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class StatusListenersCollection<T> {
    private final CopyOnWriteArrayList<StatusListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(StatusListener listener) {
        listeners.add(listener);
        listener.onUpdate(getOnAddListenerStatus());
    }

    public void removeListener(StatusListener listener) {
        listeners.remove(listener);
    }

    public void updateStatus(T status) {
        for (StatusListener listener : listeners)
            listener.onUpdate(status);
    }

    protected abstract T getOnAddListenerStatus();
}
