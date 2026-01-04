package com.kraken.api;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LoginIndexSetterFinder {

    // Configuration: The specific obfuscated field we are looking for
    private static final String TARGET_CLASS = "bv";
    private static final String TARGET_FIELD = "cv";

    public static void main(String[] args) throws IOException {
        File gamepack = new File("C:\\Users\\cbart\\Downloads\\gamepack_2533491.jar"); // Point this to your jar
        if (!gamepack.exists()) {
            System.err.println("Gamepack not found!");
            return;
        }

        try (JarFile jar = new JarFile(gamepack)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;

                try (InputStream is = jar.getInputStream(entry)) {
                    ClassReader cr = new ClassReader(is);
                    ClassNode classNode = new ClassNode();
                    cr.accept(classNode, 0);

                    findSetterMethod(classNode);
                }
            }
        }
    }

    private static void findSetterMethod(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            // 1. Filter: Must be Static
            if ((method.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }

            // 2. Filter: Check arguments (Must act like: set(int index, int/byte/short garbage))
            Type[] argTypes = Type.getArgumentTypes(method.desc);

            // Expecting exactly 2 arguments
            if (argTypes.length != 2) {
                continue;
            }

            // First argument must be an INT (the value to set)
            if (argTypes[0].getSort() != Type.INT) {
                continue;
            }

            // Second argument is the garbage value (Opaque Predicate)
            // In OSRS, this is usually INT, BYTE, or SHORT.
            int secondArgSort = argTypes[1].getSort();
            if (secondArgSort != Type.INT && secondArgSort != Type.BYTE && secondArgSort != Type.SHORT) {
                continue;
            }

            // 3. Instruction Scan: Look for PUTSTATIC targeting bv.cv
            if (scansForFieldWrite(method, TARGET_CLASS, TARGET_FIELD)) {
                System.out.println("FOUND MATCH!");
                System.out.println("Class:  " + classNode.name);
                System.out.println("Method: " + method.name);
                System.out.println("Desc:   " + method.desc);

                // Optional: Attempt to extract the garbage value comparator
                Integer garbage = extractGarbageValue(method);
                if (garbage != null) {
                    System.out.println("Inferred Garbage Value: " + garbage);
                }
                System.out.println("--------------------------------------------------");
            }
        }
    }

    private static boolean scansForFieldWrite(MethodNode method, String targetOwner, String targetName) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == Opcodes.PUTSTATIC) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (fieldInsn.owner.equals(targetOwner) && fieldInsn.name.equals(targetName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Heuristic to find the garbage value constant.
     * OSRS methods often start with: if (arg1 != 12345) { return; }
     */
    private static Integer extractGarbageValue(MethodNode method) {
        // Look at the first few instructions for a comparison involving the last argument
        int instructionLimit = 15;
        int count = 0;

        for (AbstractInsnNode insn : method.instructions) {
            if (count++ > instructionLimit) break;

            // Look for integer constants loaded immediately before a comparison
            if (insn instanceof IntInsnNode) { // BIPUSH, SIPUSH
                return ((IntInsnNode) insn).operand;
            } else if (insn instanceof LdcInsnNode) { // LDC (large ints)
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    return (Integer) cst;
                }
            }
        }
        return null;
    }
}
