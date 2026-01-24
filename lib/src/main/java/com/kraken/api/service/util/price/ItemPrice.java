package com.kraken.api.service.util.price;


import lombok.Builder;
import lombok.Data;

/**
 * Represents price data for an item in Old School RuneScape (OSRS), including high and low prices
 * along with their respective timestamps.
 *
 * <p>This class is typically used as a data model for interfacing with price-related APIs or
 * services operating within the OSRS ecosystem.</p>
 *
 * <h3>Attributes:</h3>
 * <ul>
 *   <li><strong>itemId:</strong> The unique identifier for the item.</li>
 *   <li><strong>high:</strong> The highest price recorded for the item.</li>
 *   <li><strong>low:</strong> The lowest price recorded for the item.</li>
 *   <li><strong>highTimestamp:</strong> The timestamp (as a Unix epoch) when the high price was last recorded.</li>
 *   <li><strong>lowTimestamp:</strong> The timestamp (as a Unix epoch) when the low price was last recorded.</li>
 * </ul>
 *
 * <p>The {@code ItemPrice} class leverages Lombok's {@code {@literal @}Data} and {@code {@literal @}Builder}
 * annotations, which provide boilerplate code generation for getters, setters, builder pattern, and more.</p>
 *
 * <p>This object is immutable when using the builder pattern, ensuring consistent and thread-safe
 * data access across concurrent operations.</p>
 */
@Data
@Builder
public class ItemPrice {
    private int itemId;
    private int high;
    private int low;
    private long highTimestamp;
    private long lowTimestamp;
}

