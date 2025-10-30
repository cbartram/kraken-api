package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.ObfuscatedNames;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import com.kraken.api.util.MathUtils;
import com.kraken.api.util.RandomUtils;
import lombok.SneakyThrows;
import net.runelite.api.Client;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

@Singleton
public class MousePackets {
    
    @Inject
    private Client client;

    @Inject
    private Provider<PacketClient> packetSenderProvider;

    @Inject
    private PacketDefFactory packetDefFactory;

    private static long randomDelay = RandomUtils.randomDelay();
    
    @SneakyThrows
    public void queueClickPacket(int x, int y) {
        long mouseHandlerMS = System.currentTimeMillis();
        setMouseHandlerLastMillis(mouseHandlerMS);
        long clientMS = getClientLastMillis();
        long deltaMs = mouseHandlerMS - clientMS;
        setClientLastMillis(mouseHandlerMS);

        if (deltaMs < 0) deltaMs = 0L;
        if (deltaMs > 32767) deltaMs = 32767L;
        int mouseInfo = ((int) deltaMs << 1);

        packetSenderProvider.get().sendPacket(packetDefFactory.getEventMouseClick(), mouseInfo, x, y, 0);

        int idleClientTicks = client.getKeyboardIdleTicks();
        if (client.getMouseIdleTicks() < idleClientTicks) {
            idleClientTicks = client.getMouseIdleTicks();
        }

        if (idleClientTicks >= randomDelay) {
            randomDelay = RandomUtils.randomDelay();
            Executors.newSingleThreadExecutor().submit(() -> {
                KeyEvent keyPress = new KeyEvent(client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), BUTTON1_DOWN_MASK, KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_BACK_SPACE);
                client.getCanvas().dispatchEvent(keyPress);
                KeyEvent keyRelease = new KeyEvent(client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_BACK_SPACE);
                client.getCanvas().dispatchEvent(keyRelease);
                KeyEvent keyTyped = new KeyEvent(client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char) KeyEvent.VK_UNDEFINED);
                client.getCanvas().dispatchEvent(keyTyped);
            });
        }
    }

    @SneakyThrows
    private long getClientLastMillis() {
        Field clientLastPressedTimeMillis = client.getClass().getDeclaredField(ObfuscatedNames.clientMillisField);
        clientLastPressedTimeMillis.setAccessible(true);
        long retValue = clientLastPressedTimeMillis.getLong(client) * Long.parseLong(ObfuscatedNames.clientMillisMultiplier);
        clientLastPressedTimeMillis.setAccessible(false);
        return retValue;
    }

    @SneakyThrows
    private void setMouseHandlerLastMillis(long time) {
        Class<?> mouseHandler = client.getClass().getClassLoader().loadClass(ObfuscatedNames.MouseHandler_lastPressedTimeMillisClass);
        Field mouseHandlerLastPressedTime = mouseHandler.getDeclaredField(ObfuscatedNames.MouseHandler_lastPressedTimeMillisField);
        mouseHandlerLastPressedTime.setAccessible(true);
        mouseHandlerLastPressedTime.setLong(null, time * MathUtils.modInverse(Long.parseLong(ObfuscatedNames.mouseHandlerMillisMultiplier)));
        mouseHandlerLastPressedTime.setAccessible(false);
    }

    @SneakyThrows
    private void setClientLastMillis(long time) {
        Field clientLastPressedTimeMillis = client.getClass().getDeclaredField(ObfuscatedNames.clientMillisField);
        clientLastPressedTimeMillis.setAccessible(true);
        clientLastPressedTimeMillis.setLong(client, time * MathUtils.modInverse(Long.parseLong(ObfuscatedNames.clientMillisMultiplier)));
        clientLastPressedTimeMillis.setAccessible(false);
    }
}
