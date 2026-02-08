package com.kraken.api.plugins.packetmapper;


import lombok.Data;

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

