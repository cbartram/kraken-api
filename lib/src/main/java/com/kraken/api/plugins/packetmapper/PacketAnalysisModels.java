package com.kraken.api.plugins.packetmapper;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the analysis of a packet buffer
 */
@Data
public class BufferAnalysis {
    private int totalBytes;
    private byte[] bufferData;
    private List<WriteOperation> operations = new ArrayList<>();
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BufferAnalysis{bytes=").append(totalBytes);
        sb.append(", operations=").append(operations.size());
        sb.append(", data=[");
        
        // Show first 16 bytes
        int displayLength = Math.min(16, totalBytes);
        for (int i = 0; i < displayLength; i++) {
            sb.append(String.format("%02X", bufferData[i] & 0xFF));
            if (i < displayLength - 1) sb.append(" ");
        }
        
        if (totalBytes > displayLength) {
            sb.append("...");
        }
        
        sb.append("]}");
        return sb.toString();
    }
}

/**
 * Represents a single write operation to a buffer
 */
@Data
public class WriteOperation {
    private int position;
    private int size; // 1, 2, or 4 bytes
    private int value;
    private String transformation; // e.g., "r 8", "v", "s 128", "none"
    private String methodName; // The obfuscated method name used to write this value
    private List<String> transformations = new ArrayList<>(); // Multiple transformations can apply
    
    /**
     * Sets the transformation and updates the transformations list
     */
    public void setTransformation(String transformation) {
        this.transformation = transformation;
        if (transformation != null && !transformation.equals("none")) {
            this.transformations.add(transformation);
        }
    }
    
    /**
     * Adds an additional transformation
     */
    public void addTransformation(String transformation) {
        if (!this.transformations.contains(transformation)) {
            this.transformations.add(transformation);
        }
    }
    
    /**
     * Determines the likely method name based on size and transformation
     */
    public String inferMethodName() {
        if (methodName != null) {
            return methodName;
        }
        
        // This is a placeholder - actual method names would need to be
        // extracted from bytecode analysis
        if (size == 1) {
            if (transformation != null) {
                switch (transformation) {
                    case "s 128": return "writeByte128";
                    case "v": return "writeByteAdd";
                    case "r 8": return "writeByteNeg";
                    default: return "writeByte";
                }
            }
            return "writeByte";
        } else if (size == 2) {
            if (transformation != null) {
                switch (transformation) {
                    case "v": return "writeShortAdd";
                    case "r 8": return "writeShortLE";
                    default: return "writeShort";
                }
            }
            return "writeShort";
        } else if (size == 4) {
            return "writeInt";
        }
        
        return "unknown";
    }
    
    @Override
    public String toString() {
        return String.format("WriteOp{pos=%d, size=%d, value=%d, transform=%s}", 
                           position, size, value, transformation);
    }
}

/**
 * Maps a parameter name to its write operation
 */
@Data
public class ParameterMapping {
    private String parameterName;
    private WriteOperation writeOperation;
    
    @Override
    public String toString() {
        return String.format("%s -> %s", parameterName, writeOperation);
    }
}
