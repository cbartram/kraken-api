package com.kraken.api.input.mouse.strategy.replay;

import com.google.gson.Gson;
import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.NormalizedPath;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PathLibrary {
    private static final String DATA_DIR = System.getProperty("user.home") + "/.runelite/kraken/mouse_data/";

    /**
     * Loads and normalizes a set of mouse gestures from a specified library file,
     * converting them into {@link NormalizedPath} objects. The method searches for
     * a corresponding JSON file within the directory defined by {@code DATA_DIR}.
     * If the file is found, it parses the content, normalizes valid mouse gestures,
     * and logs the results.
     *
     * <p>Mouse gestures are read line-by-line. Invalid or malformed entries
     * are skipped with appropriate warnings logged. Normalization is performed
     * through the {@code PathNormalizer.normalize()} method, which filters
     * out gestures that cannot be reliably normalized.</p>
     *
     * @param library The name of the library for which to load mouse gestures.
     *                This name is used to search for a JSON file in the
     *                {@code DATA_DIR}. Spaces in the library name are replaced
     *                with underscores during the filename matching.
     *
     * @return A {@code List} of {@link NormalizedPath} objects representing
     *         the normalized mouse gestures loaded from the library file.
     *         Returns {@literal null} if no matching file is found or if
     *         an error occurs during the loading process.
     */
    public static List<NormalizedPath> load(String library) {
        final Gson gson = new Gson();

        try {
            Path path;
            String searchFileName = library.replaceAll(" ", "_") + ".json";
            try (Stream<Path> stream = Files.list(Paths.get(DATA_DIR))) {
                path = stream
                        .filter(p -> p.toString().endsWith(searchFileName))
                        .findFirst()
                        .orElse(null);
            }

            if (path != null) {
                List<NormalizedPath> candidates = new ArrayList<>();
                log.info("Loading mouse data from library file: {}", path.getFileName());
                try (java.io.BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    int count = 0;
                    int skipped = 0;

                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        try {
                            MouseGesture raw = gson.fromJson(line, MouseGesture.class);
                            NormalizedPath norm = PathNormalizer.normalize(raw);
                            if (norm != null) {
                                candidates.add(norm);
                                count++;
                            } else {
                                skipped++;
                            }
                        } catch (Exception parseEx) {
                            log.warn("Skipping malformed mouse gesture line in {}: {}", path.getFileName(), parseEx.getMessage());
                            skipped++;
                        }
                    }
                    log.info("Loaded and normalized {}/{} mouse gestures from {}", count, count + skipped, path.getFileName());
                    return candidates;
                } catch (Exception e) {
                    log.error("Error parsing file: {}", path, e);
                }
            } else {
                log.warn("No mouse data file found for library: {}", library);
            }
        } catch (Exception e) {
            log.error("Failed to load mouse data for library: {}", library, e);
        }

        return null;
    }

    /**
     * Selects a {@link NormalizedPath} from a provided list of candidate paths
     * that most closely matches a target distance, considering tolerance thresholds
     * and randomization within a subset of viable matches.
     *
     * <p>The method evaluates the "fitness" of each candidate based on the absolute
     * difference between its original distance and the target distance. Viable paths
     * are those whose distance is within a calculated tolerance of the target distance.</p>
     *
     * <p>If no viable paths exist, a fallback strategy is employed to select randomly
     * from the top three closest matches. Otherwise, a standard strategy randomly
     * selects a path from the top 50% of the viable matches, ensuring some degree
     * of diversity and non-determinism in the selection.</p>
     *
     * @param candidates a list of {@link NormalizedPath} objects representing potential matches.
     *                   This list must not be null but can be empty, in which case the method
     *                   will log a warning and return {@literal null}.
     * @param targetDistance the target distance to match with the candidates' original distance
     *                       values. This value is used for calculating fitness and tolerance thresholds.
     * @return a {@link NormalizedPath} object selected as the best match to the target distance,
     *         or {@literal null} if no candidates are provided in the list.
     */
    public static NormalizedPath getSimilarPath(List<NormalizedPath> candidates, double targetDistance) {
        if (candidates == null || candidates.isEmpty()) {
            log.warn("No mouse data in candidate list (list is empty)");
            return null;
        }

        // We create a temporary list sorted by how close the original distance is to target
        List<NormalizedPath> sortedCandidates = candidates.stream()
                .sorted(Comparator.comparingDouble(p -> Math.abs(p.getOriginalDistance() - targetDistance)))
                .collect(Collectors.toList());

        double tolerance = Math.max(targetDistance * 0.30, 15.0);

        // Filter to 'Viable' pool
        List<NormalizedPath> viable = sortedCandidates.stream()
                .filter(p -> Math.abs(p.getOriginalDistance() - targetDistance) <= tolerance)
                .collect(Collectors.toList());

        if (viable.isEmpty()) {
            // Pick from the top 3 closest matches if there are no viable candidates
            int fallbackSize = Math.min(sortedCandidates.size(), 3);
            List<NormalizedPath> fallbackPool = sortedCandidates.subList(0, fallbackSize);
            log.info("Picking from fallback pool, no viable candidates.");
            return fallbackPool.get(new Random().nextInt(fallbackPool.size()));
        }

        // Pick randomly from the top 20% of viable matches.
        log.info("Choosing random match from top 20% of viable matches");
        int poolSize = (int) Math.ceil(viable.size() * 0.20);
        poolSize = Math.max(poolSize, Math.min(viable.size(), 5));

        List<NormalizedPath> bestFitPool = viable.subList(0, poolSize);
        return bestFitPool.get(new Random().nextInt(bestFitPool.size()));
    }
}