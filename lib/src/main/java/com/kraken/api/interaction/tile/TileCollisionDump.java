package com.kraken.api.interaction.tile;

import lombok.Value;

import java.util.List;

@Value
public class TileCollisionDump {
    int x;
    int y;
    int sceneX;
    int sceneY;
    int worldPointX;
    int worldPointY;
    int plane;
    int distance;
    int rawFlags;
    List<String> movementFlags;
}