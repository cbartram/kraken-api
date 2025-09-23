package com.kraken.api.sim.ui;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
@Singleton
public class SimulationUIState {
    private boolean showGrid = true;
    private boolean showFlags = true;
    private boolean showTooltip = false;
    private boolean showLineOfSight = false;
    private boolean playerRunning = false;
    private boolean npcEditDialogOpen = false;
    private JLabel infoLabel;
    private Point hoveredTile;
    private double zoomLevel = 1.0;
}

