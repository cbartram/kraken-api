package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PacketDefinition {
    private final String name;
    private final String[] writeData;
    private final String[][] writeMethods;
    private final PacketType type;
}
