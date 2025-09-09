package com.kraken.api.util;

import lombok.Getter;

import java.util.Random;
import java.util.Set;

public class RandomUtils {

    public static final Random random = new Random();

    public static int randomIntBetween(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static int randomFromSet(Set<Integer> set) {
        if (set.isEmpty()) {
            throw new IllegalArgumentException("Set cannot be empty");
        }

        int randomIndex = random.nextInt(set.size());
        int currentIndex = 0;

        for (Integer value : set) {
            if (currentIndex == randomIndex) {
                return value;
            }
            currentIndex++;
        }

        // This should never be reached, but just in case
        throw new RuntimeException("Failed to select random element from set");
    }
}
