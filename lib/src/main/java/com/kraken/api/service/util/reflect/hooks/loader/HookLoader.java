package com.kraken.api.service.util.reflect.hooks.loader;

import com.google.gson.GsonBuilder;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HookLoader {

    private static final String HOOKS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/reflection_hooks.json";

    public static HookRegistry load() {
        Request request = new Request.Builder().url(HOOKS_URL).build();
        com.google.gson.Gson gson = new GsonBuilder()
                .registerTypeAdapter(HookRegistry.class, new HookRegistryDeserializer())
                .create();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().charStream(), HookRegistry.class);
            } else {
                throw new RuntimeException("Failed to fetch hooks: HTTP " + response.code() + ", message: " + response.message());
            }
        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to download reflection hooks.", e);
        }
    }
}
