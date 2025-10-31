package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents the cache file stored on disk in the ~/.runelite/kraken directory containing cache contents
 * for a specific client and RuneLite revision. This helps speed up packet loading since the client doesn't
 * have to be analyzed every time the game loads.
 */
@Data
@AllArgsConstructor
public class PacketCache {
    private boolean usingClient;
    private String methodName;
}
