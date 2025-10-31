package com.kraken.api.util;

import java.math.BigInteger;

public class MathUtils {

    /**
     * Computes the inverse mod given a value and bits to shift left by
     * @param val BigInteger value
     * @param bits int number of bits to shift left
     * @return Mod inverse of the value passed.
     */
    public static BigInteger modInverse(BigInteger val, int bits) {
        try {
            BigInteger shift = BigInteger.ONE.shiftLeft(bits);
            return val.modInverse(shift);
        } catch (ArithmeticException e) {
            return val;
        }
    }

    /**
     * Computes the inverse mod given a value and bits to shift left by
     * @param val BigInteger value
     * @return Mod inverse of the value passed.
     */
    public static long modInverse(long val) {
        return modInverse(BigInteger.valueOf(val), 64).longValue();
    }
}
