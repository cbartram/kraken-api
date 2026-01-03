package com.kraken.api.service.ui.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthHooks {
    public int setLoginIndexGarbageValue;
    public String setLoginIndexMethodName;
    public String setLoginIndexClassName;

    public String jxSessionFieldName;
    public String jxSessionClassName;

    public String jxAccountIdFieldName;
    public String jxAccountIdClassName;

    public String jxDisplayNameFieldName;
    public String jxDisplayNameClassName;

    public String jxAccountCheckFieldName;
    public String jxAccountCheckClassName;

    public String jxLegacyValueFieldName;
    public String jxLegacyValueClassName;


    /**
     * Loads a json file containing the client authorization hook fields from
     * @return
     */
    public static AuthHooks load() {
        try (okhttp3.Response response = new okhttp3.OkHttpClient().newCall(new okhttp3.Request.Builder().url("https://minio.kraken-plugins.com/kraken-bootstrap-static/authHooks.json").build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return com.krakenplugins.profile.ProfilePlugin.GSON.fromJson(response.body().charStream(), AuthHooks.class);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}