package com.kraken.api.sim.model;

import net.runelite.api.Actor;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

public interface SimActor {
    WorldPoint getLocation();
    WorldArea getWorldArea(); // The world area the actor takes up i.e. 5x5, 3x3, 7x7 is defined by an area.
    Actor copy();
}
