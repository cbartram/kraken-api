package com.kraken.api.core.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import org.objectweb.asm.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@Slf4j
@Singleton
public class LoginHooksMethodLocator {

    Path WORKING_DIR = RuneLite.RUNELITE_DIR.toPath().resolve("kraken");

    // Constants to search for in System.getenv() calls
    private static final Set<String> TARGET_ENV_CONSTANTS = Set.of(
            "JX_SESSION_ID",
            "JX_CHARACTER_ID",
            "JX_DISPLAY_NAME",
            "JX_ACCESS_TOKEN",
            "JX_REFRESH_TOKEN"
    );

    // Storage for findings
    private static final Map<String, FieldMapping> fieldMappings = new LinkedHashMap<>();
    private static final Map<String, MethodMapping> methodMappings = new LinkedHashMap<>();

    public void decompileClient() {
        Path jarPath = WORKING_DIR.resolve("injected-" + RuneLiteProperties.getVersion() + ".jar");
        Path outputPath = WORKING_DIR.resolve("deobfuscated-" + RuneLiteProperties.getVersion() + ".jar");
        Path jsonOutputPath = WORKING_DIR.resolve("mappings-" + RuneLiteProperties.getVersion() + ".json");

        downloadInjectedClient(RuneLiteProperties.getVersion(), jarPath);
        removeNamedAnnotations(jarPath, outputPath);

        log.info("Deobfuscation complete. Output: {}", outputPath);

        // Print findings and write JSON
        printFindings();
        writeJsonMappings(jsonOutputPath);
    }

    private void printFindings() {
        if (fieldMappings.isEmpty() && methodMappings.isEmpty()) {
            log.warn("No System.getenv() calls with target constants found in the JAR");
            return;
        }

        log.info("========================================");
        log.info("SYSTEM.GETENV MAPPINGS FOUND:");
        log.info("========================================");

        for (Map.Entry<String, FieldMapping> entry : fieldMappings.entrySet()) {
            FieldMapping mapping = entry.getValue();
            log.info("Constant: '{}' -> Field: {}.{} (descriptor: {})",
                    entry.getKey(), mapping.className, mapping.fieldName, mapping.descriptor);
        }

        for (Map.Entry<String, MethodMapping> entry : methodMappings.entrySet()) {
            MethodMapping mapping = entry.getValue();
            log.info("Constant: '{}' -> Method: {}.{}{}",
                    entry.getKey(), mapping.className, mapping.methodName, mapping.descriptor);
        }

        log.info("========================================");
        log.info("Total field mappings: {}", fieldMappings.size());
        log.info("Total method mappings: {}", methodMappings.size());
        log.info("========================================");
    }

    @SneakyThrows
    private void writeJsonMappings(Path jsonPath) {
        Map<String, String> jsonOutput = new LinkedHashMap<>();

        // Convert to JSON format matching the example
        for (Map.Entry<String, FieldMapping> entry : fieldMappings.entrySet()) {
            String constant = entry.getKey();
            FieldMapping mapping = entry.getValue();

            String baseKey = constantToJsonKey(constant);
            jsonOutput.put(baseKey + "FieldName", mapping.fieldName);
            jsonOutput.put(baseKey + "ClassName", mapping.className);
        }

        for (Map.Entry<String, MethodMapping> entry : methodMappings.entrySet()) {
            String constant = entry.getKey();
            MethodMapping mapping = entry.getValue();

            String baseKey = constantToJsonKey(constant);
            jsonOutput.put(baseKey + "MethodName", mapping.methodName);
            jsonOutput.put(baseKey + "ClassName", mapping.className);
        }

        // Write JSON file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonOutput);

        Files.writeString(jsonPath, json);
        log.info("JSON mappings written to: {}", jsonPath);
        log.info("JSON content:\n{}", json);
    }

    /**
     * Converts constant name to JSON key prefix.
     * JX_SESSION_ID -> jxSession
     * JX_CHARACTER_ID -> jxCharacterId
     */
    private String constantToJsonKey(String constant) {
        // Remove JX_ prefix and convert to camelCase
        if (constant.startsWith("JX_")) {
            String withoutPrefix = constant.substring(3);
            String[] parts = withoutPrefix.split("_");
            StringBuilder result = new StringBuilder("jx");

            for (String part : parts) {
                if (!part.isEmpty()) {
                    result.append(part.charAt(0))
                            .append(part.substring(1).toLowerCase());
                }
            }
            return result.toString();
        }
        return constant.toLowerCase();
    }

    @SneakyThrows
    private static void removeNamedAnnotations(Path jarPath, Path destination) {
        // Clear previous findings
        fieldMappings.clear();
        methodMappings.clear();

        // Ensure parent directory exists
        Files.createDirectories(destination.getParent());

        try (JarFile jarFile = new JarFile(jarPath.toFile());
             JarOutputStream jarOutputStream = new JarOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(destination)))) {

            jarFile.stream().forEach(jarEntry -> {
                try {
                    JarEntry newEntry = new JarEntry(jarEntry.getName());

                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        if (jarEntry.getName().endsWith(".class")) {
                            byte[] processedClass = processClassFile(inputStream, jarEntry.getName());

                            jarOutputStream.putNextEntry(newEntry);
                            jarOutputStream.write(processedClass);
                            jarOutputStream.closeEntry();
                        } else {
                            jarOutputStream.putNextEntry(newEntry);
                            inputStream.transferTo(jarOutputStream);
                            jarOutputStream.closeEntry();
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static byte[] processClassFile(InputStream classInputStream, String className) throws IOException {
        ClassReader classReader = new ClassReader(classInputStream);
        ClassWriter classWriter = new ClassWriter(0);

        // Get simple class name (last part after /)
        String simpleClassName = className.contains("/")
                ? className.substring(className.lastIndexOf('/') + 1).replace(".class", "")
                : className.replace(".class", "");

        ClassVisitor classVisitor = new GetenvSearchClassVisitor(
                Opcodes.ASM9,
                classWriter,
                simpleClassName
        );

        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    /**
     * ASM ClassVisitor that searches for System.getenv() calls.
     */
    private static class GetenvSearchClassVisitor extends ClassVisitor {
        private final String className;

        public GetenvSearchClassVisitor(int api, ClassVisitor classVisitor, String className) {
            super(api, classVisitor);
            this.className = className;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
            return new FieldVisitor(Opcodes.ASM9, fv) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals("Ljavax/inject/Named;")) {
                        return null;
                    }
                    return super.visitAnnotation(descriptor, visible);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new GetenvSearchMethodVisitor(
                    Opcodes.ASM9,
                    mv,
                    className,
                    name,
                    descriptor
            );
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Ljavax/inject/Named;")) {
                return null;
            }
            return super.visitAnnotation(descriptor, visible);
        }
    }

    /**
     * ASM MethodVisitor that searches for System.getenv() calls followed by field assignments.
     */
    private static class GetenvSearchMethodVisitor extends MethodVisitor {
        private final String className;
        private final String methodName;
        private final String methodDescriptor;
        private String lastLdcConstant = null;

        public GetenvSearchMethodVisitor(int api, MethodVisitor methodVisitor,
                                         String className, String methodName,
                                         String methodDescriptor) {
            super(api, methodVisitor);
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        @Override
        public void visitLdcInsn(Object value) {
            // Track the last LDC constant
            if (value instanceof String) {
                lastLdcConstant = (String) value;
            }
            super.visitLdcInsn(value);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Check if this is a System.getenv() call
            if (owner.equals("java/lang/System") && name.equals("getenv") &&
                    descriptor.equals("(Ljava/lang/String;)Ljava/lang/String;")) {

                // Check if the last LDC was one of our target constants
                if (lastLdcConstant != null && TARGET_ENV_CONSTANTS.contains(lastLdcConstant)) {
                    // Special case for JX_DISPLAY_NAME - it might be used in a method call
                    if (lastLdcConstant.equals("JX_DISPLAY_NAME")) {
                        // Check if parent is a method invocation (we'll detect this in visitFieldInsn)
                        log.info("Found System.getenv('{}') in method {}.{}",
                                lastLdcConstant, className, methodName);
                    }
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // PUTSTATIC or PUTFIELD - field assignment
            if ((opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) &&
                    lastLdcConstant != null && TARGET_ENV_CONSTANTS.contains(lastLdcConstant)) {

                // Get simple owner name (last part after /)
                String ownerClassName = owner.contains("/")
                        ? owner.substring(owner.lastIndexOf('/') + 1)
                        : owner;

                FieldMapping mapping = new FieldMapping(ownerClassName, name, descriptor);
                fieldMappings.put(lastLdcConstant, mapping);

                log.info("Found System.getenv('{}') -> {}.{} ({})",
                        lastLdcConstant, ownerClassName, name, descriptor);

                // Reset after capturing
                lastLdcConstant = null;
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Ljavax/inject/Named;")) {
                return null;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            if (descriptor.equals("Ljavax/inject/Named;")) {
                return null;
            }
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }
    }

    /**
     * Data class for field mappings.
     */
    private static class FieldMapping {
        final String className;
        final String fieldName;
        final String descriptor;

        public FieldMapping(String className, String fieldName, String descriptor) {
            this.className = className;
            this.fieldName = fieldName;
            this.descriptor = descriptor;
        }
    }

    /**
     * Data class for method mappings.
     */
    private static class MethodMapping {
        final String className;
        final String methodName;
        final String descriptor;

        public MethodMapping(String className, String methodName, String descriptor) {
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }
    }

    @SneakyThrows
    private void downloadInjectedClient(String version, Path destination) {
        if (Files.exists(destination)) {
            log.info("Injected client already exists at {}, skipping download", destination);
            return;
        }

        Files.createDirectories(destination.getParent());

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
}