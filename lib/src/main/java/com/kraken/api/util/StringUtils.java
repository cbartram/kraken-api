package com.kraken.api.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StringUtils {

    private static final String COL_TAGS_REGEX = "<col=[^>]*>";

    /**
     * Finds the index of a term in an array of terms, ignoring case.
     *
     * @param terms the array of terms to search
     * @param term  the term to find
     * @return the index of the term in the array, or -1 if not found
     */
    public static int getIndex(String[] terms, String term) {
        for (int i = 0; i < terms.length; i++) {
            if (terms[i] != null && (terms[i].equalsIgnoreCase(term))) return i;
        }
        return -1;
    }

    /**
     * Strips {@code <col=...>} tags from each string in the provided array.
     *
     * @param sourceList the array of strings to process
     * @return a new array with {@code <col=...>} tags removed
     */
    public static String[] stripColTags(String[] sourceList) {
        List<String> resultList = new ArrayList<>();

        for (String item : sourceList) {
            if (item != null) {
                resultList.add(stripColTags(item));
            } else {
                resultList.add(null);
            }
        }

        return resultList.toArray(String[]::new);
    }

    /**
     * Strips {@code <col=...>} tags from a single string.
     * @param source the string to process
     * @return the string with all color tags removed
     */
    public static String stripColTags(String source) {
        return source.replaceAll(COL_TAGS_REGEX, "");
    }

    /**
     * Wraps the provided text in a standard color tag {@code <col=ff9040>}.
     * @param text the text to wrap
     * @return the text wrapped in color tags, or the original text if null or empty
     */
    public static String addColTags(String text) {
        if (text == null || text.isEmpty()) return text;
        return "<col=ff9040>" + text + "</col>";
    }

    /**
     * Encrypts the given plaintext using AES/CBC/PKCS5Padding.
     * <p>
     * This method generates a random 16-byte IV, performs the encryption,
     * combines the IV and the ciphertext, and returns the result as a Base64 encoded string.
     * </p>
     * @param plaintext the text to encrypt
     * @param key A 32 byte base64 encoded key used to encrypt the string
     * @return a Base64 encoded string containing the IV followed by the encrypted bytes
     * @throws RuntimeException if the encryption process fails
     */
    public static String encrypt(String plaintext, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64 encoded string containing an IV and ciphertext.
     * <p>
     * This method expects the input string to be the result of the {@link #encrypt(String, String)} method.
     * It extracts the IV from the first 16 bytes and decrypts the remaining bytes.
     * </p>
     * @param base64IvAndCiphertext the Base64 encoded string containing the IV and encrypted data
     * @param key a 32 byte base64 encoded key for decrypting the string.
     * @return the decrypted plaintext string
     * @throws RuntimeException if the decryption process fails
     */
    public static String decrypt(String base64IvAndCiphertext, String key) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64IvAndCiphertext);
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            int ctLen = combined.length - iv.length;
            byte[] ct = new byte[ctLen];
            System.arraycopy(combined, iv.length, ct, 0, ctLen);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainBytes = cipher.doFinal(ct);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}