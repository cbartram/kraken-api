package com.kraken.api.core;

public interface Scriptable {
    public void onStart();
    public void onEnd();
    public long loop();
}
