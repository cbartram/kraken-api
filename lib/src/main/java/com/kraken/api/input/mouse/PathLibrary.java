package com.kraken.api.input.mouse;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.NormalizedPath;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class PathLibrary {

    private final Map<String, List<NormalizedPath>> library = new HashMap<>();
    private final PathNormalizer normalizer = new PathNormalizer();
    private final Gson gson = new Gson();

    // Directory where you saved the JSONs
    private static final String DATA_DIR = System.getProperty("user.home") + "/.runelite/kraken/mouse_data/";

    public void load(String label) {
        try {
            Path path;
            // distinct file search specific to the label
            String searchFileName = label.replaceAll(" ", "_") + ".json";

            // 1. Find the file (Wrapped in try-with-resources to close the Stream safely)
            try (Stream<Path> stream = Files.list(Paths.get(DATA_DIR))) {
                path = stream
                        .filter(p -> p.toString().endsWith(searchFileName))
                        .findFirst()
                        .orElse(null);
            }

            if (path != null) {
                log.info("Switching mouse data to: {}", path.getFileName());

                // 2. Clear existing data to enforce "1 library at a time"
                library.clear();

                loadFile(path);
            } else {
                log.warn("No mouse data file found for label: {}", label);
            }
        } catch (Exception e) {
            log.error("Failed to load mouse data for label: " + label, e);
        }
    }

    private void loadFile(Path filePath) {
        // Use BufferedReader explicitly to access readLine()
        try (java.io.BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Parse the single line as a single MouseGesture object
                    MouseGesture raw = gson.fromJson(line, MouseGesture.class);

                    // Proceed with your normalization logic
                    NormalizedPath norm = normalizer.normalize(raw);
                    if (norm != null) {
                        library.computeIfAbsent(norm.getLabel(), k -> new ArrayList<>()).add(norm);
                        count++;
                    }
                } catch (Exception parseEx) {
                    // Optional: Log specific bad lines without crashing the whole load
                    log.warn("Skipping malformed line in {}: {}", filePath.getFileName(), parseEx.getMessage());
                }
            }
            log.info("Loaded {} paths from {}", count, filePath.getFileName());

        } catch (Exception e) {
            log.error("Error parsing file: " + filePath, e);
        }
    }

    /**
     * Finds the best matching path for the current scenario.
     */
    public NormalizedPath getBestPath(String label, double targetDistance) {
        List<NormalizedPath> candidates = library.get(label);
        if (candidates == null || candidates.isEmpty()) {
            log.warn("No mouse data found in memory for label: {}", label);
            return null;
        }

        // 2. Score and Sort Candidates
        // We create a temporary list sorted by "Fitness" (how close the original distance is to target)
        List<NormalizedPath> sortedCandidates = candidates.stream()
                .sorted(Comparator.comparingDouble(p -> Math.abs(p.getOriginalDistance() - targetDistance)))
                .collect(Collectors.toList());

        // 3. define Tolerance
        double tolerance = Math.max(targetDistance * 0.30, 15.0);

        // 4. Filter to 'Viable' pool
        List<NormalizedPath> viable = sortedCandidates.stream()
                .filter(p -> Math.abs(p.getOriginalDistance() - targetDistance) <= tolerance)
                .collect(Collectors.toList());

        // 5. Selection Strategy
        if (viable.isEmpty()) {
            // FALLBACK: Pick from the top 3 closest matches.
            int fallbackSize = Math.min(sortedCandidates.size(), 3);
            List<NormalizedPath> fallbackPool = sortedCandidates.subList(0, fallbackSize);
            return fallbackPool.get(new Random().nextInt(fallbackPool.size()));
        }

        // STANDARD: Pick randomly from the top 50% of viable matches.
        int poolSize = (int) Math.ceil(viable.size() * 0.50);
        poolSize = Math.max(poolSize, Math.min(viable.size(), 5));

        List<NormalizedPath> bestFitPool = viable.subList(0, poolSize);
        return bestFitPool.get(new Random().nextInt(bestFitPool.size()));
    }
}