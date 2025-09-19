package com.kraken.api.sim;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kraken.api.sim.ui.SimulationVisualizer;

import javax.swing.*;

/**
 * Runs game simulations
 */
public class Simulation {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector();
        SimulationVisualizer visualizer = injector.getInstance(SimulationVisualizer.class);
        SwingUtilities.invokeLater(() -> {
            visualizer.init();
            visualizer.setVisible(true);
        });
    }
}
