package com.kraken.api.core.loader.model;

import lombok.Data;

/**
 * Represents an artifact from a bootstrap JSON file
 */
@Data
public class Artifact {
    private String hash;
    private String name;
    private String path;
    private int size;
}
