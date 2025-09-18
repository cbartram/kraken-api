package com.kraken.api.core.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kraken.api.core.loader.model.Artifact;
import com.kraken.api.core.loader.model.BootstrapResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Predicate;

/**
 * This class is responsible for downloading and parsing a RuneLite structured bootstrap file. A bootstrap
 * file is a JSON file which contains jar files necessary for the process to run. RuneLite's bootstrap file follow the following format
 *
 * {
 *     "artifacts": [
 *         {
 *             "name": "artifact-name",
 *             "path": "path/to/artifact.jar",
 *             "hash": "sha256-hash-of-artifact",
 *             "size": 123456
 *         },
 *     ],
 * }
 *
 * This class fetches the bootstrap file from a given URL, parses it, and provides access the artifacts. It is intended to
 * be used with RuneLite's bootstrap.json file, but can be used with any similar structured file to load additional classes
 * at runtime.
 */
@Slf4j
public class BootstrapLoader {

    private BootstrapResponse bootstrapResponse;

    /**
     * Retrieves an artifact matching the given predicate from the loaded bootstrap response.
     * @param predicate the condition to match the artifact
     * @return the matching artifact, or null if none found or if bootstrapResponse is not loaded
     */
    public Artifact getArtifact(Predicate<Artifact> predicate) {
        if(this.bootstrapResponse == null) {
            return null;
        }

        return this.bootstrapResponse.getArtifacts().stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Fetches and parses the bootstrap file from the specified URL.
     * @param uri The URI to load the bootstrap file
     */
    public void load(final URI uri) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to fetch bootstrap file. HTTP status: {}", response.statusCode());
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            BootstrapResponse bootstrapResponse = mapper.readValue(response.body(),
                    BootstrapResponse.class);

            if (bootstrapResponse.getErrorMessage() != null && !bootstrapResponse.getErrorMessage().isEmpty()) {
                log.error("failed to retrieve bootstrap.json file: {}", bootstrapResponse.getErrorMessage());
            }

            this.bootstrapResponse = bootstrapResponse;
        } catch (Exception e) {
            log.error("Failed to fetch and parse bootstrap file from {}: {}", uri.getHost(), e.getMessage());
        }
    }
}