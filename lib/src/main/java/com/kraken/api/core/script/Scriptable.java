package com.kraken.api.core.script;

public interface Scriptable {
    public void onStart();
    public void onEnd();
    public long loop();
}
