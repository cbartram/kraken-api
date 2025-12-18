package com.kraken.api.core.script;

/**
 * Interface representing a script that can be executed.
 * Implementations of this interface define the main loop of a script.
 */
public interface Scriptable {

    /**
     * Gracefully stops a running asynchronous loop().
     * @param callback Callback function to execute once the the loop() is stopped.
     */
    void stop(Runnable callback);
}
