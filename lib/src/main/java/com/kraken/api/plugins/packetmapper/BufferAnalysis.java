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
