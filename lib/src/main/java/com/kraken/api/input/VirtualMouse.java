package com.kraken.api.input;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.callback.ClientThread;

import java.awt.event.MouseEvent;

@Slf4j
@Singleton
public class VirtualMouse  {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public void click(Point point) {
        if (point == null) return;

        Runnable clickAction = () -> {
            entered(point);
            moved(point);
            pressed(point);
            released(point);
            clicked(point);
            exited(point);
        };

        clientThread.invoke(clickAction);
    }

    private synchronized void pressed(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        client.getCanvas().dispatchEvent(event);
    }

    private synchronized void released(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        client.getCanvas().dispatchEvent(event);
    }

    private synchronized void clicked(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        client.getCanvas().dispatchEvent(event);
    }

    private synchronized void exited(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        client.getCanvas().dispatchEvent(event);
    }

    private synchronized void entered(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        client.getCanvas().dispatchEvent(event);
    }

    private synchronized void moved(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        client.getCanvas().dispatchEvent(event);
    }
}

