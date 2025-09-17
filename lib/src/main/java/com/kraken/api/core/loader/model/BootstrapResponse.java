package com.kraken.api.core.loader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapResponse {
    private List<Artifact> artifacts;
    private String hash;
    private String errorMessage;
}
