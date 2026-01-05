package com.kraken.api.service.util.reflect.hooks.loader;

import com.google.gson.*;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import com.kraken.api.service.util.reflect.hooks.LoginHooks;
import com.kraken.api.service.util.reflect.hooks.MouseHooks;
import com.kraken.api.service.util.reflect.hooks.model.FieldHook;
import com.kraken.api.service.util.reflect.hooks.model.MethodHook;

import java.lang.reflect.Type;

/**
 * Custom deserializer that directly creates domain hook objects from JSON.
 * Eliminates the need for intermediate DTO objects entirely.
 */
public class HookRegistryDeserializer implements JsonDeserializer<HookRegistry> {

    @Override
    public HookRegistry deserialize(JsonElement json, Type type,
                                    JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        return new HookRegistry(
                parseLoginHooks(root),
                parseMouseHooks(root)
        );
    }

    private LoginHooks parseLoginHooks(JsonObject root) {
        return new LoginHooks(
                new MethodHook(
                        getStr(root, "setLoginIndexMethodName"),
                        getStr(root, "setLoginIndexClassName"),
                        getIntOrNull(root, "setLoginIndexGarbageValue")
                ),
                new FieldHook(getStr(root, "jxSessionFieldName"), getStr(root, "jxSessionClassName")),
                new FieldHook(getStr(root, "jxAccountIdFieldName"), getStr(root, "jxAccountIdClassName")),
                new FieldHook(getStr(root, "jxDisplayNameFieldName"), getStr(root, "jxDisplayNameClassName")),
                new FieldHook(getStr(root, "jxAccountCheckFieldName"), getStr(root, "jxAccountCheckClassName")),
                new FieldHook(getStr(root, "jxJagexValueFieldName"), getStr(root, "jxJagexValueClassName")),
                new FieldHook(getStr(root, "jxLegacyValueFieldName"), getStr(root, "jxLegacyValueClassName"))
        );
    }

    private MouseHooks parseMouseHooks(JsonObject root) {
        return new MouseHooks(
                new FieldHook(getStr(root, "idleCyclesFieldName"), getStr(root, "idleCyclesClassName"))
        );
    }

    private String getStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null ? el.getAsString() : null;
    }

    private Integer getIntOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null ? el.getAsInt() : null;
    }
}
