package com.kraken.api.core.loader;


import com.google.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

@Slf4j
public class PacketUtilsLoader {

    private static final String PACKET_UTILS_PREFIX = "com.example.PacketUtils";
    private static final String PLUGIN_BASE_CLASS_NAME = "net.runelite.client.plugins.Plugin";

    @Inject
    private PluginManager pluginManager;

    @Getter
    public List<URL> localUrls;

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


    public void registerPacketUtilsPlugin(Class<? extends Plugin> pluginClass) {

    }


    /**
     * Loads the packet utils jar from a specified directory into memory.
     *
     * @param path The path to the directory containing JAR files
     * @return A ClassLoader that includes all loaded JAR files
     */
    public List<Class<? extends Plugin>> loadPacketUtilsFromDir(String path) {
        List<Class<? extends Plugin>> pluginClasses = new ArrayList<>();

        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        File[] jarFiles = folder.listFiles((dir, name) -> name.endsWith(".jar") && !name.toLowerCase().contains("kraken-client-"));
        if (jarFiles == null) {
            return new ArrayList<>();
        }

        // Create a single ClassLoader for all JARs
        try {
            // Convert all jar files to URLs
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
            }

            try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
                for (File jarFile : jarFiles) {
                    try (JarFile jar = new JarFile(jarFile)) {
                        Enumeration<JarEntry> entries = jar.entries();

                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();

                            if (entryName.endsWith(".class")) {
                                String className = entryName
                                        .replace('/', '.')
                                        .replace(".class", "");

                                // Filter by the desired prefix
                                if (className.startsWith(PACKET_UTILS_PREFIX)) {
                                    log.debug("Loading class: {}", className);
                                    try {
                                        Class<?> clazz = classLoader.loadClass(className);
                                        if (clazz.getSuperclass() != null) {
                                            if (clazz.getSuperclass().getName().equals(PLUGIN_BASE_CLASS_NAME)) {
                                                log.debug("Plugin Class located: {}", className);
                                                pluginClasses.add((Class<? extends Plugin>) clazz);
                                            }
                                        }
                                    } catch (ClassNotFoundException e) {
                                        log.error("Failed to load class for class name: {}", className, e);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("Exception thrown while attempting to read jar file: {}. Error = {}",
                                jarFile.getName(), e.getMessage(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to create ClassLoader", e);
        }

        return pluginClasses;
    }

    /**
     * Loads each class which matches the given package name within a JAR file from a public URL. The public URL must resolve
     * to a JAR file. This method will return a classloader which defines class from a byte array representation.
     * @param packageName String the name of the package to filter for within the JAR file. JAR files often contain many classes
     *                    that are not the plugin classes i.e. dependencies, metadata, etc...
     * @param url A Pre signed S3 url enabling the JAR file to be downloaded.
     * @return ByteArrayClassLoader.
     */
    public ByteArrayClassLoader loadPacketUtilsFromUrl(final String packageName, final URL url) {
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
                if (name.endsWith(".class") && name.startsWith(packageName)) {
                    byte[] classBytes = readEntryBytes(jarStream);
                    String className = name.substring(0, name.length() - 6)
                            .replace('/', '.');

                    log.debug("Adding plugin class: {}", className);
                    classData.put(className, classBytes);
                }
            }
        }  catch (IOException e) {
            log.error("Failed to read jar classes into a byte array {}", e.getMessage());
        }

        return new ByteArrayClassLoader(classData);
    }
}




