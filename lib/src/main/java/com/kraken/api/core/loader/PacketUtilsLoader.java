package com.kraken.api.core.loader;


import com.google.inject.Inject;
import com.kraken.api.core.loader.model.Artifact;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Loads the Packet Utils jar by identifying the latest version from the bootstrap.json file used to launch this plugin.
 * Packet utils is required in order to use packet functionality within plugins and provides a base for the Kraken API.
 * This jar should be loaded for both development work and production builds
 */
@Slf4j
public class PacketUtilsLoader {

    private static final String PACKET_UTILS_PREFIX = "com/example/PacketUtils";
    private static final String PLUGIN_BASE_CLASS_NAME = "net.runelite.client.plugins.Plugin";
    private static final String BOOTSTRAP_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/bootstrap.json";

    @Inject
    private PluginManager pluginManager;

    @Inject
    private BootstrapLoader bootstrap;

    @Getter
    public List<URL> localUrls;

    public void loadPacketUtils() {
        try {
            bootstrap.load(URI.create(BOOTSTRAP_URL));
            Artifact packetUtilsJar = bootstrap.getArtifact(a -> a.getName().contains("packet-utils-"));
            if(packetUtilsJar != null) {
                log.info("Pulled packet utils jar from bootstrap: {}", packetUtilsJar.getName());
                URL url = new URL(packetUtilsJar.getPath());
                try(ByteArrayClassLoader loader = loadPacketUtilsFromUrl(url)) {
                    for (String className : loader.getClassData().keySet()) {
                        try {
                            Class<?> clazz = loader.loadClass(className);
                            if (clazz.getSuperclass() != null) {
                                if (clazz.getSuperclass().getName().equals(PLUGIN_BASE_CLASS_NAME)) {
                                    log.info("Found packet-utils plugin class: {}", clazz.getSimpleName());
                                    startPlugin((Class<? extends Plugin>) clazz);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            log.error("class {} could not be found", className, e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to load packet utils from url: {}{}, Error = {}", url.getHost(), url.getPath(), e.getMessage());
                }
            } else {
                log.error("failed to find packet-utils jar file in bootstrap.json file. See error messages above");
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL exception while creating URL to load packet utils: {}", e.getMessage());
        }
    }

    /**
     * Reads an InputStream into a byte array using a 16KB buffer for efficiency.
     * Used to load individual class files from a JAR stream into memory.
     * @param is InputStream
     * @return byte[] An array of bytes.
     */
    private static byte[] readEntryBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    private void startPlugin(Class<? extends Plugin> pluginClass) {
        try {
            List<Plugin> plugins = pluginManager.loadPlugins(Collections.singletonList(pluginClass), null);
            if(!plugins.isEmpty()) {
                Plugin packetUtils = plugins.get(0);
                pluginManager.setPluginEnabled(packetUtils, true);
                pluginManager.startPlugin(packetUtils);
            } else {
                log.error("No plugins were loaded for class: {}", pluginClass.getName());
            }
        } catch (PluginInstantiationException e) {
            log.error("Failed to load packet utils plugin: {}", pluginClass.getName(), e);
        } catch (AssertionError e) {
            log.info("Assertion error while loading packet utils plugin: ", e);
        }
    }

    /**
     * Loads each class which matches the given package name within a JAR file from a public URL. The public URL must resolve
     * to a JAR file. This method will return a classloader which defines class from a byte array representation.
     * @param url A url enabling the JAR file to be downloaded.
     * @return ByteArrayClassLoader.
     */
    private ByteArrayClassLoader loadPacketUtilsFromUrl(final URL url) {
        Map<String, byte[]> classData = new HashMap<>();
        URLConnection connection;

        try {
            connection = url.openConnection();
        } catch (IOException e) {
            log.error("IOException thrown while attempting to open connection to url: {}{}, Error = {}", url.getHost(), url.getPath(), e.getMessage());
            return null;
        }

        try (JarInputStream jarStream = new JarInputStream(connection.getInputStream())) {
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName();

                // Load both classes and anonymous inner classes with $1 in the class name
                if (name.endsWith(".class") && name.startsWith(PACKET_UTILS_PREFIX)) {
                    byte[] classBytes = readEntryBytes(jarStream);
                    String className = name.substring(0, name.length() - 6)
                            .replace('/', '.');

                    log.debug("Queueing packet-utils class for load: {}", className);
                    classData.put(className, classBytes);
                }
            }
        }  catch (IOException e) {
            log.error("Failed to read jar classes into a byte array {}", e.getMessage());
        }

        return new ByteArrayClassLoader(classData);
    }
}




