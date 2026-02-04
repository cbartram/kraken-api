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
    private static LoginIndexMethodMapping loginIndexMethod = null;
    private static final List<LoginIndexCandidate> loginIndexCandidates = new ArrayList<>();

    public void decompileClient() {
        Path jarPath = WORKING_DIR.resolve("injected-" + RuneLiteProperties.getVersion() + ".jar");
        Path outputPath = WORKING_DIR.resolve("deobfuscated-" + RuneLiteProperties.getVersion() + ".jar");
        Path jsonOutputPath = WORKING_DIR.resolve("mappings-" + RuneLiteProperties.getVersion() + ".json");

        downloadInjectedClient(RuneLiteProperties.getVersion(), jarPath);
        removeNamedAnnotations(jarPath, outputPath);
        printFindings();
        writeJsonMappings(jsonOutputPath);
        log.info("Done.");
    }

    private void printFindings() {
        if (fieldMappings.isEmpty() && methodMappings.isEmpty() && loginIndexMethod == null) {
            log.warn("No mappings found in the JAR");
            return;
        }

        log.info("========================================");
        log.info("MAPPINGS FOUND:");
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
        log.info("LOGIN INDEX METHOD CANDIDATES:");
        log.info("========================================");

        // Sort candidates by score (highest first)
        loginIndexCandidates.sort((a, b) -> Integer.compare(b.score, a.score));

        for (int i = 0; i < loginIndexCandidates.size(); i++) {
            LoginIndexCandidate candidate = loginIndexCandidates.get(i);
            log.info("Candidate #{}: {}.{} (score: {}/10)",
                    i + 1, candidate.className, candidate.methodName, candidate.score);
            log.info("  Garbage Value: {}", candidate.garbageValue);
            log.info("  Descriptor: {}", candidate.descriptor);
            log.info("  Pattern Details:");
            log.info("    - Has field read (GETSTATIC): {}", candidate.hasFieldRead);
            log.info("    - Has field write (PUTSTATIC): {}", candidate.hasFieldWrite);
            log.info("    - Has multiplication (IMUL): {}", candidate.hasMultiplication);
            log.info("    - Has conditional jump: {}", candidate.hasConditionalJump);
            log.info("    - Has try-catch RuntimeException: {}", candidate.hasTryCatch);
            log.info("    - Field read count: {}", candidate.fieldReadCount);
            log.info("    - Field write count: {}", candidate.fieldWriteCount);
            log.info("    - Multiplication count: {}", candidate.multiplicationCount);
            log.info("    - Jump count: {}", candidate.jumpCount);
            log.info("    - Has comparison with var0 (first param): {}", candidate.hasVar0Comparison);
            log.info("    - Has comparison with var1 (second param): {}", candidate.hasVar1Comparison);
            log.info("    - Same field read/write: {}", candidate.sameFieldReadWrite);
            if (i == 0 && loginIndexMethod != null) {
                log.info("  >>> SELECTED AS BEST MATCH <<<");
            }
            log.info("");
        }

        if (loginIndexMethod != null) {
            log.info("Selected Login Index Method: {}.{} (garbage value: {})",
                    loginIndexMethod.className, loginIndexMethod.methodName, loginIndexMethod.garbageValue);
        } else if (!loginIndexCandidates.isEmpty()) {
            log.warn("No clear best match found. Review candidates above.");
        }

        log.info("========================================");
        log.info("Total field mappings: {}", fieldMappings.size());
        log.info("Total method mappings: {}", methodMappings.size());
        log.info("Login index candidates found: {}", loginIndexCandidates.size());
        log.info("========================================");
    }

    @SneakyThrows
    private void writeJsonMappings(Path jsonPath) {
        Map<String, Object> jsonOutput = new LinkedHashMap<>();

        // Add login index method mapping first
        if (loginIndexMethod != null) {
            jsonOutput.put("setLoginIndexGarbageValue", loginIndexMethod.garbageValue);
            jsonOutput.put("setLoginIndexMethodName", loginIndexMethod.methodName);
            jsonOutput.put("setLoginIndexClassName", loginIndexMethod.className);
        }

        // Convert field mappings to JSON format
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
        loginIndexMethod = null;
        loginIndexCandidates.clear();

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
     * ASM ClassVisitor that searches for System.getenv() calls and login index methods.
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
            return new CombinedMethodVisitor(
                    Opcodes.ASM9,
                    mv,
                    className,
                    name,
                    descriptor,
                    access
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
     * Combined MethodVisitor that searches for both System.getenv() calls and login index setter pattern.
     */
    private static class CombinedMethodVisitor extends MethodVisitor {
        private final String className;
        private final String methodName;
        private final String methodDescriptor;
        private final int access;

        // For getenv detection
        private String lastLdcConstant = null;

        // For login index pattern detection
        private final List<Instruction> instructions = new ArrayList<>();
        private int currentLine = 0;

        public CombinedMethodVisitor(int api, MethodVisitor methodVisitor,
                                     String className, String methodName,
                                     String methodDescriptor, int access) {
            super(api, methodVisitor);
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.access = access;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            currentLine = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitLdcInsn(Object value) {
            // Track the last LDC constant for getenv detection
            if (value instanceof String) {
                lastLdcConstant = (String) value;
            }

            // Track for login index pattern
            if (value instanceof Integer) {
                instructions.add(new LdcInstruction((Integer) value));
            }

            super.visitLdcInsn(value);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            instructions.add(new IntInstruction(opcode, operand));
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitInsn(int opcode) {
            instructions.add(new SimpleInstruction(opcode));
            super.visitInsn(opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            instructions.add(new VarInstruction(opcode, var));
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // For getenv detection
            if ((opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) &&
                    lastLdcConstant != null && TARGET_ENV_CONSTANTS.contains(lastLdcConstant)) {

                String ownerClassName = owner.contains("/")
                        ? owner.substring(owner.lastIndexOf('/') + 1)
                        : owner;

                FieldMapping mapping = new FieldMapping(ownerClassName, name, descriptor);
                fieldMappings.put(lastLdcConstant, mapping);

                log.info("Found System.getenv('{}') -> {}.{} ({})",
                        lastLdcConstant, ownerClassName, name, descriptor);

                lastLdcConstant = null;
            }

            // Track for login index pattern
            instructions.add(new FieldInstruction(opcode, owner, name, descriptor));

            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Check if this is a System.getenv() call
            if (owner.equals("java/lang/System") && name.equals("getenv") &&
                    descriptor.equals("(Ljava/lang/String;)Ljava/lang/String;")) {

                if (lastLdcConstant != null && TARGET_ENV_CONSTANTS.contains(lastLdcConstant)) {
                    if (lastLdcConstant.equals("JX_DISPLAY_NAME")) {
                        log.info("Found System.getenv('{}') in method {}.{}",
                                lastLdcConstant, className, methodName);
                    }
                }
            }

            instructions.add(new MethodInstruction(opcode, owner, name, descriptor));
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            instructions.add(new JumpInstruction(opcode));
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            instructions.add(new TryCatchInstruction(type));
            super.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public void visitEnd() {
            // After visiting all instructions, check if this matches the login index pattern
            checkLoginIndexPattern();
            super.visitEnd();
        }

        private void checkLoginIndexPattern() {
            // Must be static void method with 2 int parameters: (II)V or (IS)V or (IB)V
            if ((access & Opcodes.ACC_STATIC) == 0) {
                return;
            }

            if (!methodDescriptor.equals("(II)V") &&
                    !methodDescriptor.equals("(IS)V") &&
                    !methodDescriptor.equals("(IB)V")) {
                return;
            }

            // Initialize pattern tracking
            boolean hasFieldRead = false;
            boolean hasFieldWrite = false;
            boolean hasMultiplication = false;
            boolean hasConditionalJump = false;
            boolean hasTryCatch = false;
            boolean hasVar0Comparison = false;
            boolean hasVar1Comparison = false;
            boolean sameFieldReadWrite = false;

            int fieldReadCount = 0;
            int fieldWriteCount = 0;
            int multiplicationCount = 0;
            int jumpCount = 0;

            Integer potentialGarbageValue = null;
            String readFieldOwner = null;
            String readFieldName = null;
            String writeFieldOwner = null;
            String writeFieldName = null;

            // Check for try-catch RuntimeException
            hasTryCatch = instructions.stream()
                    .anyMatch(i -> i instanceof TryCatchInstruction &&
                            ((TryCatchInstruction) i).type != null &&
                            ((TryCatchInstruction) i).type.contains("RuntimeException"));

            for (int i = 0; i < instructions.size(); i++) {
                Instruction instr = instructions.get(i);

                // Check for GETSTATIC (field read)
                if (instr instanceof FieldInstruction &&
                        ((FieldInstruction) instr).opcode == Opcodes.GETSTATIC) {
                    hasFieldRead = true;
                    fieldReadCount++;
                    FieldInstruction fi = (FieldInstruction) instr;
                    if (readFieldOwner == null) {
                        readFieldOwner = fi.owner;
                        readFieldName = fi.name;
                    }
                }

                // Check for PUTSTATIC (field write)
                if (instr instanceof FieldInstruction &&
                        ((FieldInstruction) instr).opcode == Opcodes.PUTSTATIC) {
                    hasFieldWrite = true;
                    fieldWriteCount++;
                    FieldInstruction fi = (FieldInstruction) instr;
                    if (writeFieldOwner == null) {
                        writeFieldOwner = fi.owner;
                        writeFieldName = fi.name;
                    }
                }

                // Check for multiplication (IMUL)
                if (instr instanceof SimpleInstruction &&
                        ((SimpleInstruction) instr).opcode == Opcodes.IMUL) {
                    hasMultiplication = true;
                    multiplicationCount++;
                }

                // Check for conditional jumps
                if (instr instanceof JumpInstruction) {
                    int opcode = ((JumpInstruction) instr).opcode;
                    jumpCount++;
                    if (opcode == Opcodes.IF_ICMPNE || opcode == Opcodes.IF_ICMPEQ) {
                        hasConditionalJump = true;

                        // Check if previous instructions involve var0 or var1
                        if (i >= 2) {
                            Instruction prev1 = instructions.get(i - 1);
                            Instruction prev2 = instructions.get(i - 2);

                            if (prev1 instanceof VarInstruction) {
                                int varIndex = ((VarInstruction) prev1).var;
                                if (varIndex == 0) hasVar0Comparison = true;
                                if (varIndex == 1) hasVar1Comparison = true;
                            }
                            if (prev2 instanceof VarInstruction) {
                                int varIndex = ((VarInstruction) prev2).var;
                                if (varIndex == 0) hasVar0Comparison = true;
                                if (varIndex == 1) hasVar1Comparison = true;
                            }
                        }
                    }
                }

                // Look for LDC of a large integer (potential garbage value)
                if (instr instanceof LdcInstruction) {
                    int value = ((LdcInstruction) instr).value;
                    // Garbage values are typically large numbers
                    if (Math.abs(value) > 1000000) {
                        // Check if this is followed by a load of var1 and comparison
                        boolean followedByVar1Check = false;
                        if (i + 1 < instructions.size()) {
                            Instruction next1 = instructions.get(i + 1);
                            if (next1 instanceof VarInstruction &&
                                    ((VarInstruction) next1).var == 1 &&
                                    i + 2 < instructions.size()) {
                                Instruction next2 = instructions.get(i + 2);
                                if (next2 instanceof JumpInstruction) {
                                    followedByVar1Check = true;
                                    hasVar1Comparison = true;
                                }
                            } else if (next1 instanceof JumpInstruction) {
                                followedByVar1Check = true;
                            }
                        }

                        if (followedByVar1Check) {
                            potentialGarbageValue = value;
                        }
                    }
                }
            }

            // Check if same field is read and written
            if (readFieldOwner != null && writeFieldOwner != null &&
                    readFieldOwner.equals(writeFieldOwner) && readFieldName != null &&
                    readFieldName.equals(writeFieldName)) {
                sameFieldReadWrite = true;
            }

            // Calculate a score for this candidate (0-10)
            int score = 0;

            // Core requirements (must have these)
            if (!hasTryCatch || !hasFieldRead || !hasFieldWrite ||
                    !hasMultiplication || !hasConditionalJump || potentialGarbageValue == null) {
                // Not even a candidate if missing core requirements
                return;
            }

            // Scoring system
            if (hasTryCatch) score += 1;
            if (hasFieldRead) score += 1;
            if (hasFieldWrite) score += 1;
            if (hasMultiplication) score += 1;
            if (hasConditionalJump) score += 1;
            if (sameFieldReadWrite) score += 2; // Important: same field read/write
            if (hasVar0Comparison) score += 1; // Comparing first parameter
            if (hasVar1Comparison) score += 1; // Comparing second parameter (garbage value)

            // Penalize if too many operations (likely more complex method)
            if (fieldReadCount == 1 && fieldWriteCount == 1) score += 1; // Ideal case
            if (multiplicationCount == 2) score += 1; // Typical pattern has 2 multiplications

            // Create candidate
            LoginIndexCandidate candidate = new LoginIndexCandidate(
                    className, methodName, methodDescriptor, potentialGarbageValue, score,
                    hasFieldRead, hasFieldWrite, hasMultiplication, hasConditionalJump, hasTryCatch,
                    fieldReadCount, fieldWriteCount, multiplicationCount, jumpCount,
                    hasVar0Comparison, hasVar1Comparison, sameFieldReadWrite
            );

            loginIndexCandidates.add(candidate);

            // If this is a high-scoring candidate (8+), consider it the best match
            if (score >= 8 && (loginIndexMethod == null || score > getBestCandidateScore())) {
                loginIndexMethod = new LoginIndexMethodMapping(
                        className, methodName, potentialGarbageValue);

                log.info("Found high-scoring Login Index Method candidate: {}.{} with garbage value: {} (score: {}/10)",
                        className, methodName, potentialGarbageValue, score);
            }
        }

        private int getBestCandidateScore() {
            if (loginIndexMethod == null) return 0;
            for (LoginIndexCandidate c : loginIndexCandidates) {
                if (c.className.equals(loginIndexMethod.className) &&
                        c.methodName.equals(loginIndexMethod.methodName)) {
                    return c.score;
                }
            }
            return 0;
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

    // Instruction tracking classes
    private static abstract class Instruction {}

    private static class LdcInstruction extends Instruction {
        final int value;
        LdcInstruction(int value) { this.value = value; }
    }

    private static class IntInstruction extends Instruction {
        final int opcode;
        final int operand;
        IntInstruction(int opcode, int operand) {
            this.opcode = opcode;
            this.operand = operand;
        }
    }

    private static class SimpleInstruction extends Instruction {
        final int opcode;
        SimpleInstruction(int opcode) { this.opcode = opcode; }
    }

    private static class VarInstruction extends Instruction {
        final int opcode;
        final int var;
        VarInstruction(int opcode, int var) {
            this.opcode = opcode;
            this.var = var;
        }
    }

    private static class FieldInstruction extends Instruction {
        final int opcode;
        final String owner;
        final String name;
        final String descriptor;
        FieldInstruction(int opcode, String owner, String name, String descriptor) {
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    private static class MethodInstruction extends Instruction {
        final int opcode;
        final String owner;
        final String name;
        final String descriptor;
        MethodInstruction(int opcode, String owner, String name, String descriptor) {
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    private static class JumpInstruction extends Instruction {
        final int opcode;
        JumpInstruction(int opcode) { this.opcode = opcode; }
    }

    private static class TryCatchInstruction extends Instruction {
        final String type;
        TryCatchInstruction(String type) { this.type = type; }
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

    /**
     * Data class for login index method candidate with scoring details.
     */
    private static class LoginIndexCandidate {
        final String className;
        final String methodName;
        final String descriptor;
        final int garbageValue;
        final int score;
        final boolean hasFieldRead;
        final boolean hasFieldWrite;
        final boolean hasMultiplication;
        final boolean hasConditionalJump;
        final boolean hasTryCatch;
        final int fieldReadCount;
        final int fieldWriteCount;
        final int multiplicationCount;
        final int jumpCount;
        final boolean hasVar0Comparison;
        final boolean hasVar1Comparison;
        final boolean sameFieldReadWrite;

        public LoginIndexCandidate(String className, String methodName, String descriptor,
                                   int garbageValue, int score,
                                   boolean hasFieldRead, boolean hasFieldWrite,
                                   boolean hasMultiplication, boolean hasConditionalJump,
                                   boolean hasTryCatch, int fieldReadCount, int fieldWriteCount,
                                   int multiplicationCount, int jumpCount,
                                   boolean hasVar0Comparison, boolean hasVar1Comparison,
                                   boolean sameFieldReadWrite) {
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
            this.garbageValue = garbageValue;
            this.score = score;
            this.hasFieldRead = hasFieldRead;
            this.hasFieldWrite = hasFieldWrite;
            this.hasMultiplication = hasMultiplication;
            this.hasConditionalJump = hasConditionalJump;
            this.hasTryCatch = hasTryCatch;
            this.fieldReadCount = fieldReadCount;
            this.fieldWriteCount = fieldWriteCount;
            this.multiplicationCount = multiplicationCount;
            this.jumpCount = jumpCount;
            this.hasVar0Comparison = hasVar0Comparison;
            this.hasVar1Comparison = hasVar1Comparison;
            this.sameFieldReadWrite = sameFieldReadWrite;
        }
    }

    /**
     * Data class for login index method mapping.
     */
    private static class LoginIndexMethodMapping {
        final String className;
        final String methodName;
        final int garbageValue;

        public LoginIndexMethodMapping(String className, String methodName, int garbageValue) {
            this.className = className;
            this.methodName = methodName;
            this.garbageValue = garbageValue;
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