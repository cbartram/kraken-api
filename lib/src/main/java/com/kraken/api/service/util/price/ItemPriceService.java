package com.kraken.api.service.util.price;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class ItemPriceService {

    private static final String API_BASE = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    private final Map<Integer, ItemPrice> priceCache = new ConcurrentHashMap<>();

    @Inject
    public ItemPriceService(OkHttpClient okHttpClient, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    /**
     * Retrieves the price for a specific item.
     * <p>
     * 1. Checks the local cache.
     * 2. If missing, performs a non blocking network request for that specific item. This is safe to use on or off
     * the RuneLite client thread.
     * </p>
     * @param itemId The OSRS Item ID
     * @param userAgent A user agent sent to the OSRS Wiki to identify the application fetching data. This should NOT
     *                  be the basic java user agent or contain information about your plugins or client as it is sent to the Wiki and likely inspected.
     * @param callback A functional interface for consuming the result of the asynchronous API call
     * @throws RuntimeException if called on the main client thread (optional safety check you could add)
     */
    public void getItemPrice(int itemId, String userAgent, Consumer<ItemPrice> callback) {
        if (priceCache.containsKey(itemId)) {
            callback.accept(priceCache.get(itemId));
            return;
        }

        fetchSingleItemAsync(itemId, userAgent, callback);
    }

    public ItemPrice getItemPriceSync(int itemId, String userAgent) {
        if (priceCache.containsKey(itemId)) {
            return priceCache.get(itemId);
        }

        return fetchSingleItem(itemId, userAgent);
    }

    /**
     * Fetches the price information for a single item by performing a blocking network request to the API. NOTE:
     * This is safe to run in a {@link com.kraken.api.core.script.Script} loop since the loop runs on a non client thread
     * however, it is NOT safe to run this on the client thread (in methods which use @Subscribe for example). Use {@link #getItemPrice(int, String, Consumer)}
     * instead if you need to get an item price while on the client thread.
     * <p>
     * If the API call is successful, the item's price data is parsed, cached, and returned.
     * In case of an error (such as a network issue or unsuccessful response), this method logs the error
     * and returns {@code null}.
     * </p>
     *
     * @param itemId The unique identifier for the item whose price information is being fetched.
     * This is typically the OSRS Item ID.
     * @param userAgent A user agent sent to the OSRS Wiki to identify the application fetching data. This should NOT
     * be the basic java user agent or contain information about your plugins or client as it is sent to the Wiki and likely inspected.
     * @return An {@code ItemPrice} object representing the item's price data, or {@code null}
     * if the item has no trade data, the request fails, or a parsing error occurs.
     */

    private ItemPrice fetchSingleItem(int itemId, String userAgent) {
        HttpUrl url = HttpUrl.parse(API_BASE).newBuilder()
                .addQueryParameter("id", String.valueOf(itemId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Failed to lookup item {}: HTTP {}", itemId, response.code());
                return null;
            }

            if (response.body() == null) return null;
            String jsonString = response.body().string();
            parseAndCache(jsonString);
            return priceCache.get(itemId);
        } catch (IOException e) {
            log.error("Network error looking up item {}", itemId, e);
            return null;
        }
    }

    private void fetchSingleItemAsync(int itemId, String userAgent, Consumer<ItemPrice> callback) {
        HttpUrl url = HttpUrl.parse(API_BASE).newBuilder()
                .addQueryParameter("id", String.valueOf(itemId))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.warn("Failed to lookup item {}", itemId, e);
                callback.accept(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        log.warn("Failed to lookup item {}: HTTP {}", itemId, response.code());
                        callback.accept(null);
                        return;
                    }

                    String jsonString = response.body().string();
                    parseAndCache(jsonString);

                    ItemPrice price = priceCache.get(itemId);
                    if (price != null) {
                        callback.accept(price);
                    }
                } catch (IOException e) {
                    log.error("Error reading response", e);
                }
            }
        });
    }


    /**
     * Asynchronously fetches prices for ALL items to populate the cache.
     * Useful for plugin startup.
     * @param userAgent A user agent sent to the OSRS Wiki to identify the application fetching data. This should NOT
     * be the basic java user agent or contain information about your plugins or client as it is sent to the Wiki and likely inspected.
     */
    public void refreshAllPrices(String userAgent) {
        Request request = new Request.Builder()
                .url(API_BASE)
                .header("User-Agent", userAgent)
                .build();


        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                log.warn("Failed to bulk fetch prices: ", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    return;
                }

                try (var body = response.body()) {
                    if (body != null) {
                        parseAndCache(body.string());
                        log.debug("Bulk price refresh complete. Cache size: {}", priceCache.size());
                    }
                }
            }
        });
    }

    // Helper to keep parseAndCache logic the same
    private void parseAndCache(String jsonString) {
        try {
            JsonObject root = gson.fromJson(jsonString, JsonObject.class);
            if (!root.has("data")) return;

            JsonObject dataObject = root.getAsJsonObject("data");
            Type type = new TypeToken<Map<String, WikiPriceDTO>>() {}.getType();
            Map<String, WikiPriceDTO> parsedData = gson.fromJson(dataObject, type);

            if (parsedData == null) return;

            parsedData.forEach((idStr, dto) -> {
                try {
                    int id = Integer.parseInt(idStr);
                    ItemPrice price = ItemPrice.builder()
                            .itemId(id)
                            .high(dto.high)
                            .low(dto.low)
                            .highTimestamp(dto.highTime)
                            .lowTimestamp(dto.lowTime)
                            .build();

                    priceCache.put(id, price);
                } catch (NumberFormatException ignored) {}
            });
        } catch (JsonParseException e) {
            log.error("Error parsing price JSON", e);
        }
    }

    @Data
    private static class WikiPriceDTO {
        private int high;
        private long highTime;
        private int low;
        private long lowTime;
    }
}