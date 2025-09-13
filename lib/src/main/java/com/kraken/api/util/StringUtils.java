package com.kraken.api.util;

import java.util.ArrayList;
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

    public static String stripColTags(String source) {
        return source.replaceAll(COL_TAGS_REGEX, "");
    }

    public static String addColTags(String text) {
        if (text == null || text.isEmpty()) return text;
        return "<col=ff9040>" + text + "</col>";
    }
}
