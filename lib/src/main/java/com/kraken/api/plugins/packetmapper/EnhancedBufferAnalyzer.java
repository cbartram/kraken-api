package com.kraken.api.plugins.packetmapper;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced buffer analyzer that detects write transformations more accurately
 */
@Slf4j
public class EnhancedBufferAnalyzer {

    /**
     * Analyzes buffer data and attempts to identify write operations and transformations
     */
    public static List<WriteOperation> analyzeBuffer(byte[] data, int length) {
        List<WriteOperation> operations = new ArrayList<>();
        int pos = 0;
        
        while (pos < length) {
            WriteOperation op = identifyWriteOperation(data, pos, length);
            if (op != null) {
                operations.add(op);
                pos += op.getSize();
            } else {
                // Unknown byte, skip
                pos++;
            }
        }
        
        return operations;
    }

    /**
     * Identifies a single write operation at the given position
     */
    private static WriteOperation identifyWriteOperation(byte[] data, int pos, int length) {
        // Try to identify the write operation type and transformation
        
        // Check for 4-byte int
        if (pos + 3 < length) {
            WriteOperation intOp = tryIdentifyInt(data, pos);
            if (intOp != null) {
                return intOp;
            }
        }
        
        // Check for 2-byte short
        if (pos + 1 < length) {
            WriteOperation shortOp = tryIdentifyShort(data, pos);
            if (shortOp != null) {
                return shortOp;
            }
        }
        
        // Single byte
        return tryIdentifyByte(data, pos);
    }

    /**
     * Tries to identify a 4-byte integer write
     */
    private static WriteOperation tryIdentifyInt(byte[] data, int pos) {
        int b1 = data[pos] & 0xFF;
        int b2 = data[pos + 1] & 0xFF;
        int b3 = data[pos + 2] & 0xFF;
        int b4 = data[pos + 3] & 0xFF;
        
        WriteOperation op = new WriteOperation();
        op.setPosition(pos);
        op.setSize(4);
        
        // Standard big-endian
        int value = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        op.setValue(value);
        
        // Check for transformations
        // IME (Inverse Middle Endian): b3 b4 b1 b2
        int valueIME = (b3 << 24) | (b4 << 16) | (b1 << 8) | b2;
        
        // Little-endian: b4 b3 b2 b1
        int valueLE = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        
        // For now, assume standard big-endian unless we have more context
        op.setTransformation("none");
        
        return op;
    }

    /**
     * Tries to identify a 2-byte short write with transformations
     */
    private static WriteOperation tryIdentifyShort(byte[] data, int pos) {
        int b1 = data[pos] & 0xFF;
        int b2 = data[pos + 1] & 0xFF;
        
        WriteOperation op = new WriteOperation();
        op.setPosition(pos);
        op.setSize(2);
        
        // Standard big-endian short
        short standardValue = (short) ((b1 << 8) | b2);
        
        // Check various transformations
        List<String> possibleTransformations = new ArrayList<>();
        
        // Check for "v" transformation (add 128 to high byte)
        int vTransformedHigh = (b1 - 128) & 0xFF;
        short vValue = (short) ((vTransformedHigh << 8) | b2);
        if (Math.abs(vValue) < 10000) { // Reasonable value heuristic
            possibleTransformations.add("v");
        }
        
        // Check for "r 8" transformation (reverse bytes with negation)
        short reversedValue = (short) ((b2 << 8) | b1);
        short negatedReversedValue = (short) -reversedValue;
        if (Math.abs(negatedReversedValue) < 10000) {
            possibleTransformations.add("r 8");
        }
        
        // Check for little-endian
        if (Math.abs(reversedValue) < 10000) {
            possibleTransformations.add("LE");
        }
        
        // Use the most likely transformation
        if (!possibleTransformations.isEmpty()) {
            op.setTransformation(possibleTransformations.get(0));
            op.getTransformations().addAll(possibleTransformations);
        } else {
            op.setTransformation("none");
        }
        
        op.setValue(standardValue);
        
        return op;
    }

    /**
     * Tries to identify a single byte write with transformations
     */
    private static WriteOperation tryIdentifyByte(byte[] data, int pos) {
        int byteValue = data[pos] & 0xFF;
        
        WriteOperation op = new WriteOperation();
        op.setPosition(pos);
        op.setSize(1);
        op.setValue(byteValue);
        
        List<String> possibleTransformations = new ArrayList<>();
        
        // Check for "s 128" transformation (subtract 128)
        if (byteValue == 128) {
            possibleTransformations.add("s 128");
        }
        
        // Check for "add" transformation
        int addValue = (byteValue - 128) & 0xFF;
        if (addValue == 0 || addValue == 1) {
            possibleTransformations.add("add");
        }
        
        // Check for "neg" transformation
        int negValue = (-byteValue) & 0xFF;
        if (byteValue == negValue) {
            possibleTransformations.add("neg");
        }
        
        if (!possibleTransformations.isEmpty()) {
            op.setTransformation(possibleTransformations.get(0));
            op.getTransformations().addAll(possibleTransformations);
        } else {
            op.setTransformation("none");
        }
        
        return op;
    }

    /**
     * Attempts to reverse-engineer the write method name based on transformation
     */
    public static String inferWriteMethodName(WriteOperation op) {
        int size = op.getSize();
        String transformation = op.getTransformation();
        
        if (size == 1) {
            if ("s 128".equals(transformation)) {
                return "writeByte128";
            } else if ("add".equals(transformation) || "v".equals(transformation)) {
                return "writeByteAdd";
            } else if ("neg".equals(transformation)) {
                return "writeByteNeg";
            } else {
                return "writeByte";
            }
        } else if (size == 2) {
            if ("v".equals(transformation)) {
                return "writeShortAdd";
            } else if ("r 8".equals(transformation)) {
                return "writeShortLEAdd";
            } else if ("LE".equals(transformation)) {
                return "writeShortLE";
            } else {
                return "writeShort";
            }
        } else if (size == 4) {
            if ("IME".equals(transformation)) {
                return "writeIntIME";
            } else if ("LE".equals(transformation)) {
                return "writeIntLE";
            } else {
                return "writeInt";
            }
        }
        
        return "unknown";
    }

    /**
     * Formats write operations as the transformation string array format
     */
    public static String[][] formatAsTransformationArray(List<WriteOperation> operations) {
        String[][] result = new String[operations.size()][];
        
        for (int i = 0; i < operations.size(); i++) {
            WriteOperation op = operations.get(i);
            List<String> transformations = op.getTransformations();
            
            if (transformations.isEmpty()) {
                result[i] = new String[0];
            } else {
                result[i] = transformations.toArray(new String[0]);
            }
        }
        
        return result;
    }

    /**
     * Compares two buffers to find differences (useful for identifying which bytes changed)
     */
    public static List<Integer> findDifferences(byte[] buffer1, byte[] buffer2) {
        List<Integer> differences = new ArrayList<>();
        int minLength = Math.min(buffer1.length, buffer2.length);
        
        for (int i = 0; i < minLength; i++) {
            if (buffer1[i] != buffer2[i]) {
                differences.add(i);
            }
        }
        
        return differences;
    }

    /**
     * Analyzes a sequence of packets to identify patterns
     */
    public static PacketPattern analyzePacketPattern(List<byte[]> packetBuffers) {
        if (packetBuffers.isEmpty()) {
            return null;
        }
        
        PacketPattern pattern = new PacketPattern();
        
        // Find common length
        int commonLength = packetBuffers.get(0).length;
        boolean fixedLength = true;
        
        for (byte[] buffer : packetBuffers) {
            if (buffer.length != commonLength) {
                fixedLength = false;
                break;
            }
        }
        
        pattern.setFixedLength(fixedLength);
        pattern.setCommonLength(commonLength);
        
        // Find positions that always have the same value
        List<Integer> constantPositions = new ArrayList<>();
        if (fixedLength) {
            for (int i = 0; i < commonLength; i++) {
                boolean constant = true;
                byte firstValue = packetBuffers.get(0)[i];
                
                for (byte[] buffer : packetBuffers) {
                    if (buffer[i] != firstValue) {
                        constant = false;
                        break;
                    }
                }
                
                if (constant) {
                    constantPositions.add(i);
                }
            }
        }
        
        pattern.setConstantPositions(constantPositions);
        
        return pattern;
    }

    /**
     * Represents a pattern found in multiple packets
     */
    public static class PacketPattern {
        private boolean fixedLength;
        private int commonLength;
        private List<Integer> constantPositions;
        private List<Integer> variablePositions;

        public boolean isFixedLength() { return fixedLength; }
        public void setFixedLength(boolean fixedLength) { this.fixedLength = fixedLength; }
        
        public int getCommonLength() { return commonLength; }
        public void setCommonLength(int commonLength) { this.commonLength = commonLength; }
        
        public List<Integer> getConstantPositions() { return constantPositions; }
        public void setConstantPositions(List<Integer> constantPositions) { 
            this.constantPositions = constantPositions;
            // Calculate variable positions
            this.variablePositions = new ArrayList<>();
            for (int i = 0; i < commonLength; i++) {
                if (!constantPositions.contains(i)) {
                    variablePositions.add(i);
                }
            }
        }
        
        public List<Integer> getVariablePositions() { return variablePositions; }
    }
}
