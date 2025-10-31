package com.kraken.api.core.packet;

import java.lang.reflect.Field;

/**
 * A static utility class that uses reflection to interact with the client's
 * obfuscated buffer objects (e.g., PacketBuffer). This class provides a stable
 * API to get/set the buffer's underlying byte array and its current offset,
 * and to write data using the client's specific (and obfuscated) methods.
 */
public class BufferUtils {

    /**
     * Reflectively sets the 'offset' (current write position) on a buffer instance.
     *
     * @param bufferInstance The obfuscated buffer object (e.g., PacketBuffer).
     * @param offset         The new offset value to set.
     */
    public static void setOffset(Object bufferInstance, int offset) {
        try {
            // Find the field using its obfuscated name
            Field offsetField = bufferInstance.getClass().getField(ObfuscatedNames.bufferOffsetField);
            offsetField.setAccessible(true); // Allow modification
            offsetField.setInt(bufferInstance, offset); // Set the new value
            offsetField.setAccessible(false); // Restore access control
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reflectively gets the 'offset' (current write position) from a buffer instance.
     *
     * @param bufferInstance The obfuscated buffer object.
     * @return The current offset, or -1 if retrieval fails.
     */
    public static int getOffset(Object bufferInstance) {
        try {
            Field offsetField = bufferInstance.getClass().getField(ObfuscatedNames.bufferOffsetField);
            offsetField.setAccessible(true);
            int offset = offsetField.getInt(bufferInstance); // Get the current value
            offsetField.setAccessible(false);
            return offset;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Reflectively sets the underlying 'array' (the byte[]) on a buffer instance.
     *
     * @param bufferInstance The obfuscated buffer object.
     * @param array          The new byte[] to set as the buffer's data.
     */
    public static void setArray(Object bufferInstance, byte[] array) {
        try {
            Field arrayField = bufferInstance.getClass().getField(ObfuscatedNames.bufferArrayField);
            arrayField.setAccessible(true);
            arrayField.set(bufferInstance, array); // Set the new array reference
            arrayField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reflectively gets the underlying 'array' (the byte[]) from a buffer instance.
     *
     * @param bufferInstance The obfuscated buffer object.
     * @return The buffer's byte[] data, or null if retrieval fails.
     */
    public static byte[] getArray(Object bufferInstance) {
        try {
            Field arrayField = bufferInstance.getClass().getField(ObfuscatedNames.bufferArrayField);
            arrayField.setAccessible(true);
            byte[] array = (byte[]) arrayField.get(bufferInstance); // Get the array reference
            arrayField.setAccessible(false);
            return array;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Writes a single integer value to the buffer using a specified "write description".
     * This method handles the client's obfuscated write types (add, subtract, etc.)
     * and the complex index calculation.
     *
     * @param writeDescription A string (e.g., "a128", "s", "v") defining the write operation.
     * @param value            The integer value to write.
     * @param bufferInstance   The obfuscated buffer object.
     */
    public static void writeValue(String writeDescription, int value, Object bufferInstance) {
        // Parse magnitude from description (e.g., "s128" -> 128). Default to 0 if 'v' (variable).
        int writeTypeMagnitude = writeDescription.contains("v") ? 0 : Integer.parseInt(writeDescription.substring(1).trim());

        // Get the buffer's current data
        byte[] arr = getArray(bufferInstance);

        // Calculate the next logical offset and update the buffer
        int index = nextIndex(getOffset(bufferInstance));
        setOffset(bufferInstance, index);

        // Calculate the *actual* byte array index using the obfuscated multiplier.
        // This is a key part of the client's buffer obfuscation.
        index = index * Integer.parseInt(ObfuscatedNames.indexMultiplier) - 1;

        // Perform the write operation based on the first character of the description
        switch (writeDescription.charAt(0)) {
            case 's': // Subtract
                setArray(bufferInstance, writeSub(writeTypeMagnitude, value, arr, index));
                break;
            case 'a': // Add
                setArray(bufferInstance, writeAdd(writeTypeMagnitude, value, arr, index));
                break;
            case 'r': // Right-shift
                setArray(bufferInstance, writeRightShifted(writeTypeMagnitude, value, arr, index));
                break;
            case 'v': // Variable / Raw
                setArray(bufferInstance, writeVar(value, arr, index));
                break;
        }
    }

    /**
     * Writes a string to the buffer, encoded in CP1252, followed by a
     * single null (0) byte terminator.
     *
     * @param val            The string to write.
     * @param bufferInstance The obfuscated buffer object.
     */
    public static void writeStringCp1252NullTerminated(String val, Object bufferInstance) {
        byte[] arr = getArray(bufferInstance);

        int offset = getOffset(bufferInstance);
        int indexMultiplier = Integer.parseInt(ObfuscatedNames.indexMultiplier);
        // The offset multiplier is also an obfuscated value, often a large negative number.
        int offsetMultiplier = (int) Long.parseLong(ObfuscatedNames.offsetMultiplier);

        // Calculate the real starting index in the byte array
        int realIndex = offset * indexMultiplier;

        // Encode the string directly into the buffer's array
        int bytesWritten = encodeStringCp1252(val, 0, val.length(), arr, realIndex);

        // Advance the *logical* offset based on bytes written
        offset += bytesWritten * offsetMultiplier;
        // Advance the offset one more time for the null terminator
        offset += offsetMultiplier;

        // Calculate the *actual* index for the null terminator
        int nullTerminatorIndex = offset * indexMultiplier - 1;

        // Write the null terminator
        arr[nullTerminatorIndex] = 0;

        // Update the buffer's state
        setOffset(bufferInstance, offset);
        setArray(bufferInstance, arr);
    }

    /**
     * Writes a string to the buffer, encoded in CP1252, with a null (0)
     * byte *before* and *after* the string.
     *
     * @param val            The string to write.
     * @param bufferInstance The obfuscated buffer object.
     */
    public static void writeStringCp1252NullCircumfixed(String val, Object bufferInstance) {
        byte[] arr = getArray(bufferInstance);

        int offset = getOffset(bufferInstance);
        int indexMultiplier = Integer.parseInt(ObfuscatedNames.indexMultiplier);
        int offsetMultiplier = (int) Long.parseLong(ObfuscatedNames.offsetMultiplier);

        // Advance offset and write the *leading* null byte
        offset += offsetMultiplier;
        int leadingNullIndex = offset * indexMultiplier - 1;
        arr[leadingNullIndex] = 0;

        // Calculate the start index for the string content
        int stringWriteIndex = offset * indexMultiplier;
        // Write the string and get the number of bytes written
        int bytesWritten = encodeStringCp1252(val, 0, val.length(), arr, stringWriteIndex);
        // Advance the offset for the string content
        offset += bytesWritten * offsetMultiplier;

        // Advance offset and write the *trailing* null byte
        offset += offsetMultiplier;
        int trailingNullIndex = offset * indexMultiplier - 1;
        arr[trailingNullIndex] = 0;

        // Update the buffer's state
        setOffset(bufferInstance, offset);
        setArray(bufferInstance, arr);
    }

    /**
     * Writes a byte by subtracting the value from a base (subValue).
     */
    static byte[] writeSub(int subValue, int value, byte[] arr, int index) {
        arr[index] = (byte) (subValue - value);
        return arr;
    }

    /**
     * Writes a byte by adding the value to a base (addValue).
     */
    static byte[] writeAdd(int addValue, int value, byte[] arr, int index) {
        arr[index] = (byte) (addValue + value);
        return arr;
    }

    /**
     * Writes a byte by right-shifting the value. (e.g., to get the high byte of a short).
     */
    static byte[] writeRightShifted(int shiftAmount, int value, byte[] arr, int index) {
        arr[index] = (byte) (value >> shiftAmount);
        return arr;
    }

    /**
     * Writes the raw byte value directly.
     */
    static byte[] writeVar(int value, byte[] arr, int index) {
        arr[index] = (byte) (value);
        return arr;
    }

    /**
     * Calculates the *next logical offset* by adding the obfuscated offset multiplier.
     *
     * @param offset The current logical offset.
     * @return The next logical offset.
     */
    static public int nextIndex(int offset) {
        offset += (int) Long.parseLong(ObfuscatedNames.offsetMultiplier);
        return offset;
    }

    /**
     * Encodes a Java String (CharSequence) into a byte array using the CP1252 character set.
     * This is the standard string encoding used by OSRS, which includes special
     * characters like €, ‚, ƒ, „, etc., that are not in standard ASCII.
     *
     * @param data             The string data to encode.
     * @param startIndex       The starting character index from the string.
     * @param endIndex         The ending character index from the string.
     * @param output           The destination byte array.
     * @param outputStartIndex The starting index in the byte array to write to.
     * @return The total number of bytes written.
     */
    public static int encodeStringCp1252(CharSequence data, int startIndex, int endIndex, byte[] output, int outputStartIndex) {
        int var5 = endIndex - startIndex;

        for(int var6 = 0; var6 < var5; ++var6) {
            char var7 = data.charAt(var6 + startIndex);
            // Standard ASCII range (fast path)
            if((var7 > 0 && var7 < 128) || (var7 >= 160 && var7 <= 255)) {
                output[var6 + outputStartIndex] = (byte)var7;
            }
            // Manual mapping for non-standard characters
            else if(var7 == 8364) { // €
                output[var6 + outputStartIndex] = -128;
            } else if(var7 == 8218) { // ‚
                output[var6 + outputStartIndex] = -126;
            } else if(var7 == 402) { // ƒ
                output[var6 + outputStartIndex] = -125;
            } else if(var7 == 8222) { // „
                output[var6 + outputStartIndex] = -124;
            } else if(var7 == 8230) { // …
                output[var6 + outputStartIndex] = -123;
            } else if(var7 == 8224) { // †
                output[var6 + outputStartIndex] = -122;
            } else if(var7 == 8225) { // ‡
                output[var6 + outputStartIndex] = -121;
            } else if(var7 == 710) { // ˆ
                output[var6 + outputStartIndex] = -120;
            } else if(var7 == 8240) { // ‰
                output[var6 + outputStartIndex] = -119;
            } else if(var7 == 352) { // Š
                output[var6 + outputStartIndex] = -118;
            } else if(var7 == 8249) { // ‹
                output[var6 + outputStartIndex] = -117;
            } else if(var7 == 338) { // Œ
                output[var6 + outputStartIndex] = -116;
            } else if(var7 == 381) { // Ž
                output[var6 + outputStartIndex] = -114;
            } else if(var7 == 8216) { // ‘
                output[var6 + outputStartIndex] = -111;
            } else if(var7 == 8217) { // ’
                output[var6 + outputStartIndex] = -110;
            } else if(var7 == 8220) { // “
                output[var6 + outputStartIndex] = -109;
            } else if(var7 == 8221) { // ”
                output[var6 + outputStartIndex] = -108;
            } else if(var7 == 8226) { // •
                output[var6 + outputStartIndex] = -107;
            } else if(var7 == 8211) { // –
                output[var6 + outputStartIndex] = -106;
            } else if(var7 == 8212) { // —
                output[var6 + outputStartIndex] = -105;
            } else if(var7 == 732) { // ˜
                output[var6 + outputStartIndex] = -104;
            } else if(var7 == 8482) { // ™
                output[var6 + outputStartIndex] = -103;
            } else if(var7 == 353) { // š
                output[var6 + outputStartIndex] = -102;
            } else if(var7 == 8250) { // ›
                output[var6 + outputStartIndex] = -101;
            } else if(var7 == 339) { // œ
                output[var6 + outputStartIndex] = -100;
            } else if(var7 == 382) { // ž
                output[var6 + outputStartIndex] = -98;
            } else if(var7 == 376) { // Ÿ
                output[var6 + outputStartIndex] = -97;
            } else { // Replace unmappable characters with '?'
                output[var6 + outputStartIndex] = 63;
            }
        }

        return var5; // Return the number of bytes written
    }
}