package com.kraken.api.service.util;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Singleton
public class RandomService {

    private static final double GAUSS_CUTOFF = 4.0;
    private static final java.util.Random RANDOM = new java.util.Random();

    /**
     * Returns a non-zero random double value.
     * Ensures that the result is always greater than a very small number (1.0e-320),
     * preventing the generation of an exact zero.
     *
     * @return A non-zero random double value.
     */
    public static double nzRandom() {
        return Math.max(RANDOM.nextDouble(), 1.0e-320);
    }

    /**
     * Generates a random value based on a Gaussian (normal) distribution with a specified mean and standard deviation.
     *
     * @param mean The mean (center) value of the distribution.
     * @param dev  The standard deviation of the distribution.
     *
     * @return A random double value from the Gaussian distribution.
     */
    public static double gaussRand(double mean, double dev) {
        double len = dev * Math.sqrt(-2 * Math.log(nzRandom()));
        return mean + len * Math.cos(2 * Math.PI * RANDOM.nextDouble());
    }

    /**
     * Generates a random number within the given range using a truncated Gaussian distribution.
     * This ensures that the value is within the bounds of the left and right range.
     *
     * @param left   The minimum bound of the range.
     * @param right  The maximum bound of the range.
     * @param cutoff The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random double value within the specified range.
     */
    public static double truncatedGauss(double left, double right, double cutoff) {
        if (cutoff <= 0) {
            cutoff = GAUSS_CUTOFF;
        }

        double result;
        do {
            result = Math.abs(Math.sqrt(-2 * Math.log(nzRandom())) * Math.cos(2 * Math.PI * RANDOM.nextDouble()));
        } while (result >= cutoff);

        return result / cutoff * (right - left) + left;
    }

    /**
     * Generates a random long value within the given range using a truncated Gaussian distribution.
     *
     * @param left   The minimum bound of the range.
     * @param right  The maximum bound of the range.
     * @param cutoff The cutoff value to restrict extreme values.
     *
     * @return A random long value within the specified range.
     */
    public static long truncatedGauss(long left, long right, double cutoff) {
        return Math.round(truncatedGauss((double) left, (double) right, cutoff));
    }

    /**
     * Generates a random number skewed towards the specified mode within a specified range.
     * This allows for a biased distribution where the values tend to cluster around the mode.
     *
     * @param mode   The central value around which the distribution is skewed.
     * @param lo     The lower bound of the range.
     * @param hi     The upper bound of the range.
     * @param cutoff The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random double value skewed towards the mode.
     */
    public static double skewedRand(double mode, double lo, double hi, double cutoff) {
        if (cutoff <= 0) {
            cutoff = GAUSS_CUTOFF;
        }

        double top = lo;
        if (RANDOM.nextDouble() * (hi - lo) > mode - lo) {
            top = hi;
        }

        double result;
        do {
            result = Math.abs(Math.sqrt(-2 * Math.log(nzRandom())) * Math.cos(2 * Math.PI * RANDOM.nextDouble()));
        } while (result >= cutoff);

        return result / cutoff * (top - mode) + mode;
    }

    /**
     * Generates a random long value skewed towards the specified mode within a specified range.
     *
     * @param mode   The central value around which the distribution is skewed.
     * @param lo     The lower bound of the range.
     * @param hi     The upper bound of the range.
     * @param cutoff The cutoff value to restrict extreme values.
     *
     * @return A random long value skewed towards the mode.
     */
    public static long skewedRand(long mode, long lo, long hi, double cutoff) {
        return Math.round(skewedRand((double) mode, (double) lo, (double) hi, cutoff));
    }

    /**
     * Generates a random number within the specified range, biased towards the mean.
     * The distribution has a higher likelihood of generating numbers closer to the midpoint of the range.
     *
     * @param min    The minimum bound of the range.
     * @param max    The maximum bound of the range.
     * @param cutoff The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random double value within the specified range, biased towards the middle.
     */
    public static double normalRange(double min, double max, double cutoff) {
        if (cutoff <= 0) {
            cutoff = GAUSS_CUTOFF;
        }

        switch (RANDOM.nextInt(2)) {
            case 0:
                return (max + min) / 2.0 + truncatedGauss(0, (max - min) / 2, cutoff);
            case 1:
                return (max + min) / 2.0 - truncatedGauss(0, (max - min) / 2, cutoff);
            default:
                throw new IllegalStateException("Unexpected value: " + RANDOM.nextInt(2));
        }
    }

    /**
     * Generates a random long value within the specified range, biased towards the mean.
     *
     * @param min    The minimum bound of the range.
     * @param max    The maximum bound of the range.
     * @param cutoff The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random long value within the specified range, biased towards the middle.
     */
    public static long normalRange(long min, long max, double cutoff) {
        if (cutoff <= 0) {
            cutoff = GAUSS_CUTOFF;
        }

        switch (RANDOM.nextInt(2)) {
            case 0:
                return Math.round((max + min) / 2.0 + truncatedGauss(0, (max - min) / 2, cutoff));
            case 1:
                return Math.round((max + min) / 2.0 - truncatedGauss(0, (max - min) / 2, cutoff));
            default:
                throw new IllegalStateException("Unexpected value: " + RANDOM.nextInt(2));
        }
    }

    /**
     * Generates a random point on the screen, weighted around a central point (mean) within a maximum radius.
     * The point is selected to simulate human-like randomness in mouse movement or other actions.
     *
     * @param mean   The central point to weight the randomness around.
     * @param maxRad The maximum radius away from the central point.
     * @param cutoff The cutoff value for restricting extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random point near the central point, within the specified radius.
     */
    public static Point randomPoint(Point mean, int maxRad, double cutoff) {
        int x = (int) normalRange(mean.getX() - maxRad, mean.getX() + maxRad, cutoff);
        int y = (int) normalRange(mean.getY() - maxRad, mean.getY() + maxRad, cutoff);
        return new Point(x, y);
    }

    /**
     * Generates a random point within the bounds of the given rectangle, biased towards the center.
     * This method is useful for simulating human-like randomness in screen interactions.
     *
     * @param rect   The rectangular area within which to generate the random point.
     * @param cutoff The cutoff value for restricting extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0.
     *
     * @return A random point within the rectangle, biased towards the middle.
     */
    public static Point randomPoint(Rectangle rect, double cutoff) {
        double x1 = rect.getX();
        double y1 = rect.getY();
        double x2 = rect.getX() + rect.getWidth();
        double y2 = rect.getY() + rect.getHeight();
        double a = Math.atan2(rect.getHeight(), rect.getWidth());

        int x = (int) normalRange(x1 + 1, x2 - 1, cutoff);
        int y = (int) normalRange(y1 + 1, y2 - 1, cutoff);
        return rotatePoint(new Point(x, y), a, (x2 + x1) / 2 + RANDOM.nextDouble() - 0.5, (y2 + y1) / 2 + RANDOM.nextDouble() - 0.5);
    }

    /**
     * Generates a random point within the bounds of a rectangle, skewed towards a specified 'from' point.
     * Useful for simulating more human-like randomness in actions such as dragging or moving the mouse.
     *
     * @param from  The point to bias the random point generation towards.
     * @param rect  The rectangular area within which to generate the random point.
     * @param force A multiplier that defines how strongly the point should be skewed towards the 'from' point.
     *
     * @return A random point within the rectangle, skewed towards the 'from' point.
     */
    public static Point randomPointEx(Point from, Rectangle rect, double force) {
        Point p = from;
        p = new Point(Math.min(Math.max(p.getX(), (int) rect.getX()), (int) (rect.getX() + rect.getWidth())), Math.min(Math.max(p.getY(), (int) rect.getY()), (int) (rect.getY() + rect.getHeight())));

        Point c = new Point((int) (rect.getX() + rect.getWidth() / 2), (int) (rect.getY() + rect.getHeight() / 2));
        double r = Math.hypot(p.getX() - c.getX(), p.getY() - c.getY()) * force;
        double x = Math.atan2(c.getY() - p.getY(), c.getX() - p.getX());
        p = new Point((int) (p.getX() + Math.round(Math.cos(x) * r)), (int) (p.getY() + Math.round(Math.sin(x) * r)));

        int resultX = (int) skewedRand(p.getX(), (int) rect.getX(), (int) (rect.getX() + rect.getWidth()), GAUSS_CUTOFF);
        int resultY = (int) skewedRand(p.getY(), (int) rect.getY(), (int) (rect.getY() + rect.getHeight()), GAUSS_CUTOFF);
        return new Point(resultX, resultY);
    }

    /**
     * Simulates a dice roll using a fractional probability (e.g., 0.1 for 10%).
     *
     * @param fractionalChance A decimal between 0 and 1 representing the chance.
     *
     * @return True if the random number falls within the chance, false otherwise.
     */
    public static boolean diceFractional(double fractionalChance) {
        // Generate a random number between 0 and 1 and compare
        return RANDOM.nextDouble() < fractionalChance;
    }

    /**
     * Simulates a dice roll using a whole number percentage (e.g., 10 for 10%).
     *
     * @param percentageChance A whole number between 0 and 100 representing the chance.
     *
     * @return True if the random number falls within the chance, false otherwise.
     */
    public static boolean dicePercentage(double percentageChance) {
        // Generate a random number between 0 and 100 and compare
        return RANDOM.nextDouble() * 100 < percentageChance;
    }

    /**
     * Simulates a wait with a random duration, biased towards the mean, left, or right of the given range.
     * This method is useful for introducing randomness in bot actions to reduce predictability.
     *
     * @param min    The minimum wait time in milliseconds.
     * @param max    The maximum wait time in milliseconds.
     * @param weight The direction of bias for the wait time (left, mean, or right skew).
     */
    public static void wait(double min, double max, EWaitDir weight) {
        switch (weight) {
            case wdLeft:
                systemWait(Math.round(truncatedGauss(min, max, 0)));
                break;
            case wdMean:
                systemWait(Math.round(normalRange(min, max, 0)));
                break;
            case wdRight:
                systemWait(Math.round(truncatedGauss(max, min, 0)));
                break;
        }
    }

    /**
     * Simulates a wait with a random duration, biased towards the left side of the given range.
     *
     * @param min The minimum wait time in milliseconds.
     * @param max The maximum wait time in milliseconds.
     */
    public static void wait(int min, int max) {
        wait(min, max, EWaitDir.wdLeft);
    }

    /**
     * Waits for a random duration based on a Gaussian distribution.
     * The wait time is calculated using a normal distribution with the specified mean and standard deviation.
     *
     * @param mean The mean wait time in milliseconds.
     * @param dev  The standard deviation of the wait time.
     */
    public static void waitEx(double mean, double dev) {
        long waitTime = Math.abs(Math.round(gaussRand(mean, dev)));
        systemWait(waitTime);
    }

    /**
     * Pauses the execution for a specified amount of time in milliseconds.
     * This method is a system-level wait used for simulating delays in bot actions.
     *
     * @param time The duration to wait in milliseconds.
     */
    private static void systemWait(long time) {
        log.info("Waiting for {} ms", time);
        SleepService.sleep(time);
    }

    /**
     * Rotates a given point around a specified origin by a certain angle.
     * This method is used for calculating rotated positions in 2D space.
     *
     * @param point   The point to be rotated.
     * @param angle   The angle to rotate the point, in radians.
     * @param originX The x-coordinate of the origin point.
     * @param originY The y-coordinate of the origin point.
     *
     * @return A new point representing the rotated coordinates.
     */
    private static Point rotatePoint(Point point, double angle, double originX, double originY) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double dx = point.getX() - originX;
        double dy = point.getY() - originY;
        int newX = (int) (cos * dx - sin * dy + originX);
        int newY = (int) (sin * dx + cos * dy + originY);
        return new Point(newX, newY);
    }


    /**
     * Generates a random integer between min (inclusive) and max (inclusive) with options
     * for skewing the distribution towards either the lower or higher bound.
     *
     * @param min         The minimum value (inclusive).
     * @param max         The maximum value (inclusive).
     * @param skewFactor  The skew factor. A value greater than 1 will skew the distribution towards the higher end,
     *                    while a value less than 1 will skew it towards the lower end. A value of 1 produces
     *                    a standard Gaussian distribution centered around the midpoint.
     * @param useGaussian If true, the method will use a Gaussian distribution instead of a uniform one.
     *
     * @return A random integer between min and max, possibly skewed based on the parameters.
     */
    public static int nextInt(int min, int max, double skewFactor, boolean useGaussian) {
        if (min > max) {
            throw new IllegalArgumentException("Min cannot be greater than Max.");
        }

        if (useGaussian) {
            // Generate a Gaussian (normal) distributed value
            double mean = (max + min) / 2.0; // Center around the middle
            double deviation = (max - min) / 6.0; // Ensure 99.7% of values are within [min, max]

            // Generate a random value using Gaussian distribution
            double gaussianRandom = mean + RANDOM.nextGaussian() * deviation;

            // Clamp the result to stay within the [min, max] range
            int result = (int) Math.round(Math.max(min, Math.min(max, gaussianRandom)));

            return result;
        } else {
            // Skewed random number generation
            double rawRandom = RANDOM.nextDouble();  // A random number between 0 and 1

            // Apply skew factor to bias the result
            if (skewFactor != 1) {
                rawRandom = Math.pow(rawRandom, skewFactor);
            }

            // Scale the result to the [min, max] range
            int result = (int) Math.round(min + rawRandom * (max - min));

            return result;
        }
    }

    /**
     * generate random number between min and max
     * @param min The minimum the random number can be
     * @param max The maximum the random number can be
     * @return A random number between the minimum and maximum
     */
    public static int between(final int min, final int max) {
        final int n = Math.abs(max - min);
        return Math.min(min, max) + (n == 0 ? 0 : new java.util.Random().nextInt(n));
    }

    /**
     * random gaussian
     * @param mean The mean
     * @param stddev The standard deviation
     * @return a random gaussian distribution
     */
    public static int randomGaussian(double mean, double stddev) {
        double u, v, s;
        do {
            u = 2.0 * ThreadLocalRandom.current().nextDouble() - 1.0;
            v = 2.0 * ThreadLocalRandom.current().nextDouble() - 1.0;
            s = u * u + v * v;
        } while (s >= 1 || s == 0);
        double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
        int value = (int) (mean + stddev * u * multiplier);
        if (value < 0)
        {
            value = 0;
        }
        return value;
    }

    enum EWaitDir {
        wdLeft, wdMean, wdRight
    }
}