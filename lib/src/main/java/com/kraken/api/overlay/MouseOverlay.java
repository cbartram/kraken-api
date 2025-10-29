package com.kraken.api.overlay;

import com.kraken.api.input.VirtualMouse;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

public class MouseOverlay extends Overlay {
    private final Client client;
    private final VirtualMouse mouse;

    @Inject
    public MouseOverlay(Client client, VirtualMouse mouse) {
        this.client = client;
        this.mouse = mouse;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getCanvas() == null) {
            return null;
        }

        // Enable antialiasing for smoother rendering
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawCurrentPosition(graphics);
        return null;
    }

    private void drawCurrentPosition(Graphics2D graphics) {
        Point mousePos = mouse.getCanvasMousePosition();
        if (mousePos == null) {
            return;
        }

        // Draw current position indicator
        graphics.setColor(Color.RED);
        int size = 5;

        // Draw crosshair
        graphics.setStroke(new BasicStroke(2));
        graphics.drawLine(mousePos.x - size, mousePos.y, mousePos.x + size, mousePos.y);
        graphics.drawLine(mousePos.x, mousePos.y - size, mousePos.x, mousePos.y + size);

        // Draw position text
        String posText = String.format("(%d, %d)", mousePos.x, mousePos.y);
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(posText);
        int textHeight = fm.getHeight();

        // Position text to avoid overlap with crosshair
        int textX = mousePos.x + size + 5;
        int textY = mousePos.y - size;

        // Adjust if text would go off screen
        if (textX + textWidth > client.getCanvasWidth()) {
            textX = mousePos.x - size - textWidth - 5;
        }

        if (textY < textHeight) {
            textY = mousePos.y + size + textHeight;
        }

        // Draw text background
        graphics.setColor(new Color(0, 0, 0, 128));
        graphics.fillRect(textX - 2, textY - textHeight + 2, textWidth + 4, textHeight);

        // Draw text
        graphics.setColor(Color.RED);
        graphics.drawString(posText, textX, textY);
    }
}
