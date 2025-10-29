package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;

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