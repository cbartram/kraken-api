package com.kraken.api.core.script;

/**
 * Interface representing a script that can be executed.
 * Implementations of this interface define the main loop of a script.
 */
public interface Scriptable {

    /**
     * The main loop of the script. This method will be called repeatedly
     * at the start of every game tick as long as the script is running.
     */
    void loop() throws Exception;

    /**
     * Gracefully stops a running asynchronous loop().
     * @param callback Callback function to execute once the the loop() is stopped.
     */
    void stop(Runnable callback);
}
