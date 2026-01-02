package com.kraken.api.input;


import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import net.runelite.api.Client;
import net.runelite.api.GameState;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.CHAR_UNDEFINED;

public class KeyboardService {

    @Inject
    private Client client;
    
    /**
     * Executes a given action with the canvas temporarily made focusable if it wasn't already.
     * This ensures key events are properly dispatched to the game client.
     *
     * @param action the code to run while the canvas is focusable
     */
    private void withFocusCanvas(Runnable action) {
        Canvas canvas = client.getCanvas();
        boolean originalFocus = canvas.isFocusable();
        if (!originalFocus) canvas.setFocusable(true);

        try {
            action.run();
        } finally {
            if (!originalFocus) canvas.setFocusable(false);
        }
    }

    /**
     * Types a single character.
     * Useful for things like Bank Pins where you want control over the timing between digits.
     * @param c the character to type
     */
    public void typeChar(char c) {
        withFocusCanvas(() -> {
            int delay = RandomService.between(20, 100);
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c, delay);
        });
    }

    /**
     * Types a string with a customizable sleep between characters.
     * @param text the string to type
     * @param minSleep The minimum the thread should be slept between key strokes
     * @param maxSleep The max the thread should be slept between key strokes
     */
    public void typeString(String text, int minSleep, int maxSleep) {
        withFocusCanvas(() -> {
            for (char c : text.toCharArray()) {
                int delay = RandomService.between(20, 100);
                dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c, delay);
                SleepService.sleep(minSleep, maxSleep);
            }
        });
    }

    /**
     * Dispatches a low-level KeyEvent to the canvas after a specified delay.
     *
     * @param id       the KeyEvent type (e.g. KEY_TYPED, KEY_PRESSED, etc.)
     * @param keyCode  the key code from {@link KeyEvent}
     * @param keyChar  the character to type, if applicable
     * @param delay    the delay in milliseconds before the event time is set
     */
    private void dispatchKeyEvent(int id, int keyCode, char keyChar, int delay) {
        KeyEvent event = new KeyEvent(
                client.getCanvas(),
                id,
                System.currentTimeMillis() + delay,
                0,
                keyCode,
                keyChar
        );
        client.getCanvas().dispatchEvent(event);
    }

    /**
     * Types out a string character-by-character using KEY_TYPED events.
     * Each character is sent with a short randomized delay and sleep between characters.
     *
     * @param word the string to type into the game
     */
    public void typeString(final String word) {
        withFocusCanvas(() -> {
            for (char c : word.toCharArray())
            {
                int delay = RandomService.between(20, 200);
                dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c, delay);
                SleepService.sleep(100, 200);
            }
        });
    }

    /**
     * Simulates pressing a single character using a KEY_TYPED event.
     *
     * @param key the character to press
     */
    public void keyPress(final char key) {
        withFocusCanvas(() -> {
            int delay = RandomService.between(20, 200);
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, key, delay);
        });
    }

    /**
     * Simulates holding the Shift key using a KEY_PRESSED event.
     */
    public void holdShift() {
        withFocusCanvas(() -> {
            int delay = RandomService.between(20, 200);
            dispatchKeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED, delay);
        });
    }

    /**
     * Simulates releasing the Shift key using a KEY_RELEASED event.
     */
    public void releaseShift() {
        withFocusCanvas(() -> {
            int delay = RandomService.between(20, 200);
            dispatchKeyEvent(KeyEvent.KEY_RELEASED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED, delay);
        });
    }

    /**
     * Simulates holding down a key using a KEY_PRESSED event.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyHold(int key) {
        withFocusCanvas(() ->
                dispatchKeyEvent(KeyEvent.KEY_PRESSED, key, CHAR_UNDEFINED, 0)
        );
    }

    /**
     * Simulates releasing a key using a KEY_RELEASED event.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyRelease(int key) {
        withFocusCanvas(() -> {
            int delay = RandomService.between(20, 200);
            dispatchKeyEvent(KeyEvent.KEY_RELEASED, key, CHAR_UNDEFINED, delay);
        });
    }

    /**
     * Simulates pressing and releasing a key in quick succession.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyPress(int key) {
        keyHold(key);
        keyRelease(key);
    }

    /**
     * Simulates pressing the Enter key.
     * If the player is not logged in, this uses KEY_TYPED to avoid auto-login triggers.
     */
    public void enter() {
        if (!(client.getGameState() == GameState.LOGGED_IN)) {
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n', 0);
            return;
        }

        keyPress(KeyEvent.VK_ENTER);
    }
}
