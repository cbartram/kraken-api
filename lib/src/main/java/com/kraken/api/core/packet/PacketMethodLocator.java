package com.kraken.api.core.packet;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketCache;
import com.kraken.api.core.packet.model.PacketMethods;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import org.benf.cfr.reader.Main;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A static utility class to find and cache the obfuscated packet-sending method
 * ("addNode") from the game client.
 * <p>
 * This class is intended to be run once at startup.
 */
@Slf4j
@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PacketMethodLocator {

    // This is the primary output of the locator.
    // It will be null until initialize() is successfully called.
    public static PacketMethods packetMethods;
    private static final int REQUIRED_CLIENT_REV = 234;
    private static final Path WORKING_DIRECTORY = RuneLite.RUNELITE_DIR.toPath().resolve("kraken");
    private static String loadedCacheFileName = "";

    // A regex to find method calls like "someName.someMethod("
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)\\s*\\(");
    private static final Gson gson = new Gson();

    /**
     * Initializes the packet method locator. This is the main entry point.
     * It will attempt to load from cache, or perform a full client analysis
     * if no valid cache is found.
     *
     * @param client The RuneLite Client instance.
     * @return true if the packet methods were successfully located, false otherwise.
     */
    public static synchronized boolean initialize(Client client) {
        if (packetMethods != null) {
            log.info("PacketMethodLocator class is already initialized.");
            return true;
        }

        if (Runtime.version().feature() != 11) {
            log.error("PacketMethodLocator class is detected a Java version that is not 11. Older Java versions are NOT supported.");
            return false;
        }

        if (client.getRevision() != REQUIRED_CLIENT_REV) {
            log.warn("PacketMethodLocator was built for client revision {}, but is running on {}.", REQUIRED_CLIENT_REV, client.getRevision());
            log.warn("This may cause instability or failure. Proceed with caution.");
        }

        try {
            // 3. Find packet methods (cache or analysis)
            findPacketMethods(client, RuneLiteProperties.getVersion());

            // 4. Clean up old analysis files
            cleanupStaleFiles();

            // 5. Verify config
            if (loadedCacheFileName.isEmpty()) {
                log.warn("Client packet method analysis failed to produce a cache file.");
            } else if (!loadedCacheFileName.equals(getCacheFileName(client))) {
                log.error("Unexpected cache file name detected. Loaded: {}, Expected: {}", loadedCacheFileName, getCacheFileName(client));
            } else {
                log.info("Loaded client packet method from cache: {}", loadedCacheFileName);
            }

            if (packetMethods == null) {
                log.error("Failed to locate client packet methods from cache or analysis.");
                return false;
            }

            log.info("Client packet method loaded successfully, Client AddNode: {}, " +
                    "AddNode Method: {}", packetMethods.usingClientAddNode, packetMethods.addNodeMethod != null ? packetMethods.addNodeMethod.getName() : "N/A");
            return true;
        } catch (Exception e) {
            log.error("A critical error occurred during PacketMethodLocator initialization: ", e);
            return false;
        }
    }

    /**
     * Attempts to find packet methods, first by checking cache, then by
     * performing a full client analysis.
     */
    @SneakyThrows
    private static void findPacketMethods(Client client, String runeliteVersion) {
        if (loadFromCache(client)) {
            return;
        }

        log.warn("No valid cache found. Starting full client analysis. This may take a moment...");
        analyzeClient(client, runeliteVersion);
    }

    /**
     * Attempts to load the packet method configuration from the cache file.
     *
     * @return true if loading was successful, false otherwise.
     */
    @SneakyThrows
    private static boolean loadFromCache(Client client) {
        Path cacheFilePath = WORKING_DIRECTORY.resolve(getCacheFileName(client));
        if (!Files.exists(cacheFilePath)) {
            return false;
        }

        PacketCache cache = gson.fromJson(Files.readString(cacheFilePath), PacketCache.class);
        loadedCacheFileName = cacheFilePath.getFileName().toString();

        if (cache.isUsingClient()) {
            packetMethods = new PacketMethods(null, true);
            log.info("Loaded addNode config from cache: usingClientAddNode=true");
            return true;
        }

        // Load method from class
        String[] parts = cache.getMethodName().split("\\.");
        if (parts.length < 2) {
            log.warn("Cache file {} contains invalid method name: {}. Discarding.", cacheFilePath.getFileName(), cache.getMethodName());
            return false;
        }

        String className = parts[0];
        String methodName = parts[1];
        Class<?> addNodeClass = client.getClass().getClassLoader().loadClass(className);

        // Find the method. We must check parameters to get the right one.
        for (Method method : addNodeClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) &&
                    method.getParameterCount() > 0 &&
                    method.getParameterTypes()[0].getSimpleName().equals(ObfuscatedNames.packetWriterClassName)) {

                packetMethods = new PacketMethods(method, false);
                log.info("Loaded addNode config from cache: Method={}", method);
                return true;
            }
        }

        log.warn("Cache file points to method {} but it was not found. Discarding cache.", cache.getMethodName());
        return false;
    }

    /**
     * Performs the full analysis by downloading, extracting, decompiling,
     * and parsing the client code.
     */
    @SneakyThrows
    private static void analyzeClient(Client client, String runeliteVersion) {
        String doActionClassName = ObfuscatedNames.doActionClassName;
        String doActionMethodName = ObfuscatedNames.doActionMethodName;

        if (!Files.exists(WORKING_DIRECTORY)) {
            Files.createDirectories(WORKING_DIRECTORY);
        }

        // Define paths for temporary analysis files
        Path injectedClientJarPath = WORKING_DIRECTORY.resolve("injected.jar");
        Path targetClassFilePath = WORKING_DIRECTORY.resolve("doAction.class");
        Path decompiledSourcePath = WORKING_DIRECTORY.resolve("decompiled.txt");

        downloadInjectedClient(runeliteVersion, injectedClientJarPath);
        extractClassFile(injectedClientJarPath, doActionClassName, targetClassFilePath);

        // 3. Decompile the target method
        decompileMethod(targetClassFilePath, doActionMethodName, decompiledSourcePath);

        // 4. Parse the decompiled code to find the most frequent method call
        String packetMethodName = findMostFrequentMethodCall(decompiledSourcePath);

        if (packetMethodName == null) {
            throw new RuntimeException("Failed to find most frequent method call in decompiled code.");
        }

        log.info("Analysis complete. Most frequent method call: {}", packetMethodName);

        // 5. Set the public API fields based on the result
        boolean usingClient = packetMethodName.contains("client");
        Method addNodeMethod = null;

        if (!usingClient) {
            String[] parts = packetMethodName.split("\\.");
            String className = parts[0];
            String methodName = parts[1];
            Class<?> addNodeClass = client.getClass().getClassLoader().loadClass(className);

            for (Method method : addNodeClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                        method.getParameterCount() > 0 &&
                        method.getParameterTypes()[0].getSimpleName().equals(ObfuscatedNames.packetWriterClassName)) {
                    addNodeMethod = method;
                    break;
                }
            }
            if (addNodeMethod == null) {
                throw new RuntimeException("Analysis found method " + packetMethodName + " but it could not be located via reflection.");
            }
        }

        packetMethods = new PacketMethods(addNodeMethod, usingClient);

        // 6. Save the result to the cache for next time
        saveToCache(client, packetMethodName);
    }

    /**
     * Downloads the RuneLite injected client JAR.
     */
    @SneakyThrows
    private static void downloadInjectedClient(String version, Path destination) {
        if (version.contains("SNAPSHOT")) {
            String snapshotVersion = version;
            version = version.replace("-SNAPSHOT", "");
            log.info("Treating SNAPSHOT version {} as base version: {}", snapshotVersion, version);
        }

        URL injectedURL = getInjectedURL(version);
        log.info("Downloading injected client from {}", injectedURL);
        try (InputStream clientStream = injectedURL.openStream()) {
            Files.copy(clientStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns the URL for the injected (patched) RuneLite client for a given Version.
     * @param version The RuneLite semantic version to retrieve the URL for.
     * @return A URL object to the JAR file for the RuneLite injected client.
     * @throws MalformedURLException Malformed URL exception
     */
    private static URL getInjectedURL(String version) throws MalformedURLException {
        String[] versionSplits = version.split("\\.");
        int length = versionSplits.length;
        if (!((length > 0 && Integer.parseInt(versionSplits[0]) > 1) ||
                (length > 1 && Integer.parseInt(versionSplits[1]) > 10) ||
                (length > 2 && Integer.parseInt(versionSplits[2]) > 34))) {
            throw new UnsupportedOperationException("Unsupported RuneLite version: " + version);
        }

        String url = "https://repo.runelite.net/net/runelite/injected-client/" + version + "/injected-client-" + version + ".jar";
        return new URL(url);
    }

    /**
     * Extracts a single .class file from a .jar file.
     */
    @SneakyThrows
    private static void extractClassFile(Path jarPath, String className, Path destination) {
        log.info("Extracting {}.class from {}", className, jarPath.getFileName());
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.entries().asIterator().forEachRemaining(jarEntry -> {
                if (jarEntry.getName().equals(className + ".class")) {
                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Extraction complete.");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e); // Propagate exception
                    }
                }
            });
        }
    }

    /**
     * Decompiles a single method from a .class file and saves the output to a text file.
     */
    @SneakyThrows
    private static void decompileMethod(Path classFilePath, String methodName, Path destination) {
        log.info("Decompiling {}.{}... this may take a second.", classFilePath.getFileName(), methodName);

        // Redirect System.out to our output file
        try (OutputStream decompilationOutputStream = Files.newOutputStream(destination);
             PrintStream printStream = new PrintStream(decompilationOutputStream)) {

            PrintStream originalOut = System.out;
            System.setOut(printStream);
            try {
                // Run the decompiler
                Main.main(new String[]{classFilePath.toAbsolutePath().toString(), "--methodname", methodName});
            } finally {
                // Restore System.out
                System.setOut(originalOut);
            }
        }
        log.info("Decompilation complete. Output saved to {}.", destination.getFileName());
    }

    /**
     * Parses the decompiled source code to find the most frequently called method.
     * This is the core heuristic of the analysis.
     *
     * @return The name of the most frequent method (e.g., "client.af" or "bw.a")
     */
    @SneakyThrows
    private static String findMostFrequentMethodCall(Path decompiledSourcePath) {
        List<String> methodCalls = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(decompiledSourcePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Use regex to find all method calls on the line
                Matcher matcher = METHOD_CALL_PATTERN.matcher(line);
                while (matcher.find()) {
                    String match = matcher.group(1) + "." + matcher.group(2);
                    methodCalls.add(match);
                }
            }
        }

        // Group by call signature and count occurrences
        Map<String, Long> callCounts = methodCalls.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        // Find the one with the highest count
        Optional<Map.Entry<String, Long>> maxEntry = callCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (maxEntry.isPresent()) {
            return maxEntry.get().getKey();
        }

        log.error("Could not find any method calls in decompiled source.");
        return null;
    }

    /**
     * Saves the results of the analysis to the cache file.
     */
    @SneakyThrows
    private static void saveToCache(Client client, String methodName) {
        String cacheFileName = getCacheFileName(client);
        Path cacheFilePath = WORKING_DIRECTORY.resolve(cacheFileName);

        PacketCache cache = new PacketCache(packetMethods.usingClientAddNode, methodName);
        String output = gson.toJson(cache);
        Files.write(cacheFilePath, output.getBytes(StandardCharsets.UTF_8));
        loadedCacheFileName = cacheFileName;
        log.info("Client packet data cached successfully: {}", cacheFileName);
    }

    /**
     * Cleans up temporary analysis files.
     */
    @SneakyThrows
    private static void cleanupStaleFiles() {
        List<Path> staleFilePaths = new ArrayList<>();
        staleFilePaths.add(WORKING_DIRECTORY.resolve("injected.jar"));
        staleFilePaths.add(WORKING_DIRECTORY.resolve("doAction.class"));
        staleFilePaths.add(WORKING_DIRECTORY.resolve("decompiled.txt"));

        for (Path path : staleFilePaths) {
            Files.deleteIfExists(path);
        }
        log.debug("Cleaned up stale analysis files.");
    }

    /**
     * Generates the cache file name for the current client version.
     */
    private static String getCacheFileName(Client client) {
        return RuneLiteProperties.getVersion() + "-" + client.getRevision() + ".json";
    }
}