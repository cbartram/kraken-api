package com.kraken.api.sim.ui;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class SimulationUIState {
    private boolean showGrid = true;
    private boolean showFlags = true;
    private JLabel infoLabel;
    private Point hoveredTile;
    private double zoomLevel = 1.0;
}

