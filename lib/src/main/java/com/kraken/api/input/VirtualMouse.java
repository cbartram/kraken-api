package com.kraken.api.input;

import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.RandomService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.model.NewMenuEntry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class VirtualMouse {

    private final Client client;
    private final ScheduledExecutorService scheduledExecutorService;
    
    
    private boolean exited = true;
    private static final int POINT_LIFETIME = 14;// Maximum number of points to store
    final int MAX_POINTS = 500;
    Deque<Point> points = new ConcurrentLinkedDeque<>();
    Point lastClick = new Point(-1, -1); // getter for last click
    // getter for click before last click
    Point lastClick2 = new Point(-1, -1);
    Point lastMove = new Point(-1, -1); // getter for last move
    float hue = 0.0f; // Initial hue value
    Timer timer = new Timer(POINT_LIFETIME, e -> points.pollFirst());

    public Canvas getCanvas() {
        return client.getCanvas();
    }

    @Inject
    public VirtualMouse(Client client) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(10);
        //getCanvas().setFocusable(false);
        this.client = client;
    }

    public void setLastClick(Point point) {
        lastClick2 = lastClick;
        lastClick = point;
    }

    public void setLastMove(Point point) {
        lastMove = point;
        points.add(point);
        if (points.size() > MAX_POINTS) {
            points.pollFirst();
        }
    }

    private void handleClick(Point point, boolean rightClick) {
        entered(point);
        exited(point);
        moved(point);
        pressed(point, rightClick ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
        released(point, rightClick ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
        clicked(point, rightClick ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
        setLastClick(point);
    }
    public VirtualMouse click(Point point, boolean rightClick) {
        if (point == null) return this;

        Runnable clickAction = () -> {
            handleClick(point, rightClick);
        };

        if (client.isClientThread()) {
            scheduledExecutorService.schedule(clickAction, 0, TimeUnit.MILLISECONDS);
        } else {
            clickAction.run();
        }

        return this;
    }


    public VirtualMouse click(Point point, boolean rightClick, NewMenuEntry entry) {
        if (point == null) return this;

        Runnable clickAction = () -> {
            Context.targetMenu = entry;
            handleClick(point, rightClick);
        };

        if (client.isClientThread()) {
            scheduledExecutorService.schedule(clickAction, 0, TimeUnit.MILLISECONDS);
        } else {
            clickAction.run();
        }

        return this;
    }


    public VirtualMouse click(int x, int y) {
        return click(new Point(x, y), false);
    }

    public VirtualMouse click(double x, double y) {
        return click(new Point((int) x, (int) y), false);
    }

    public VirtualMouse click(Rectangle rectangle) {
        return click(UIService.getClickingPoint(rectangle, true), false);
    }

    
    public VirtualMouse click(int x, int y, boolean rightClick) {
        return click(new Point(x, y), rightClick);
    }

    
    public VirtualMouse click(Point point) {
        return click(point, false);
    }

    
    public VirtualMouse click(Point point, NewMenuEntry entry) {
        return click(point, false, entry);
    }

    
    public VirtualMouse click() {
        return click(client.getMouseCanvasPosition());
    }

    public VirtualMouse move(Point point) {
        setLastMove(point);
        MouseEvent mouseMove = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        mouseMove.setSource("Kraken");
        getCanvas().dispatchEvent(mouseMove);

        return this;
    }

    public VirtualMouse move(Rectangle rect) {
        MouseEvent mouseMove = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, (int) rect.getCenterX(), (int) rect.getCenterY(), 0, false);
        mouseMove.setSource("Kraken");
        getCanvas().dispatchEvent(mouseMove);

        return this;
    }

    public VirtualMouse move(Polygon polygon) {
        Point point = new Point((int) polygon.getBounds().getCenterX(), (int) polygon.getBounds().getCenterY());

        MouseEvent mouseMove = new MouseEvent(getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        mouseMove.setSource("Kraken");
        getCanvas().dispatchEvent(mouseMove);

        return this;
    }

    public VirtualMouse scrollDown(Point point) {
        long time = System.currentTimeMillis();

        move(point);

        scheduledExecutorService.schedule(() -> {
            MouseEvent mouseScroll = new MouseWheelEvent(getCanvas(), MouseEvent.MOUSE_WHEEL, time, 0, point.getX(), point.getY(), 0, false,
                    0, 10, 2);
            mouseScroll.setSource("Kraken");
            getCanvas().dispatchEvent(mouseScroll);
        }, RandomService.between(40,100), TimeUnit.MILLISECONDS);
        return this;
    }

    public VirtualMouse scrollUp(Point point) {
        long time = System.currentTimeMillis();

        move(point);

        scheduledExecutorService.schedule(() -> {
            MouseEvent mouseScroll = new MouseWheelEvent(getCanvas(), MouseEvent.MOUSE_WHEEL, time, 0, point.getX(), point.getY(), 0, false,
                    0, -10, -2);
            mouseScroll.setSource("Kraken");
            getCanvas().dispatchEvent(mouseScroll);
        }, RandomService.between(40,100), TimeUnit.MILLISECONDS);
        return this;
    }

    
    public java.awt.Point getMousePosition() {
        Point point = lastMove;
        return new java.awt.Point(point.getX(), point.getY());
    }

    public java.awt.Point getCanvasMousePosition() {
       return client.getCanvas().getMousePosition();
    }
    
    public VirtualMouse move(int x, int y) {
        return move(new Point(x, y));
    }

    
    public VirtualMouse move(double x, double y) {
        return move(new Point((int) x, (int) y));
    }

    @Deprecated
    private void mouseEvent(int id, Point point, boolean rightClick) {
        int button = rightClick ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1;
        MouseEvent e = new MouseEvent(client.getCanvas(), id, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        getCanvas().dispatchEvent(e);
    }

    private synchronized void pressed(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
    }

    private synchronized void released(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
    }

    private synchronized void clicked(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
    }

    private synchronized void exited(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
        exited = true;
    }

    private synchronized void entered(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
        exited = false;
    }

    private synchronized void moved(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        event.setSource("Kraken");
        getCanvas().dispatchEvent(event);
    }

    // New drag method
//    public VirtualMouse drag(Point startPoint, Point endPoint) {
//        if (startPoint == null || endPoint == null) return this;
//
//        if (Rs2AntibanSettings.naturalMouse && (startPoint.getX() > 1 && startPoint.getY() > 1))
//            Microbot.naturalMouse.moveTo(startPoint.getX(), startPoint.getY());
//        else
//            move(startPoint);
//        sleep(50, 80);
//        // Press the mouse button at the start point
//        pressed(startPoint, MouseEvent.BUTTON1);
//        sleep(80, 120);
//        // Move to the end point while holding the button down
//        if (Rs2AntibanSettings.naturalMouse && (endPoint.getX() > 1 && endPoint.getY() > 1))
//            Microbot.naturalMouse.moveTo(endPoint.getX(), endPoint.getY());
//        else
//            move(endPoint);
//        sleep(80, 120);
//        // Release the mouse button at the end point
//        released(endPoint, MouseEvent.BUTTON1);
//
//        return this;
//    }
}


