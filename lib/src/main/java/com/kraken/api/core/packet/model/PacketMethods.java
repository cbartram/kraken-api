package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * An object which holds references to the parsed packet methods from the game client. The {@code PacketMethodLocator}
 * will parse the method which sends packets to the game server and store references to the method in this object.
 */
@Getter
@AllArgsConstructor
public class PacketMethods {

    /**
     * The static `addNode` method. This will be null if
     * `usingClientAddNode` is true.
     */
    private Method addNodeMethod;

    /**
     * True if the `addNode` method is a member of the PacketWriter class
     * (named "client" in the decompiled code).
     */
    private boolean usingClientAddNode;
}