package com.kraken.api.service.ui.login;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
@Singleton
public class LoginService {

    private AuthHooks hooks;
    private final Client client;
    private final ClientThread clientThread;

    @Inject
    public LoginService(Client client, ClientThread clientThread, Gson gson) {
        this.client = client;
        this.clientThread = clientThread;
        try (okhttp3.Response response = new okhttp3.OkHttpClient().newCall(new okhttp3.Request.Builder().url("https://minio.kraken-plugins.com/kraken-bootstrap-static/authHooks.json").build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                hooks = gson.fromJson(response.body().charStream(), AuthHooks.class);
            }
        } catch (java.io.IOException e) {
            log.error("Failed to load auth hooks: ", e);
        }
    }

    private void setLoginIndex(int index) {
        if(hooks == null) {
            log.info("No Auth Hooks found");
            return;
        }

        try {
            log.info("Setting login index {} via {}.{}", index, hooks.getSetLoginIndexClassName(), hooks.getSetLoginIndexMethodName());
            if (hooks.getSetLoginIndexGarbageValue() <= Byte.MAX_VALUE && hooks.getSetLoginIndexGarbageValue() >= Byte.MIN_VALUE) {
                Class<?> paramComposition = Class.forName(hooks.getSetLoginIndexClassName(), true, client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(hooks.getSetLoginIndexMethodName(), int.class, byte.class);

                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (byte) hooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);

            } else if (hooks.getSetLoginIndexGarbageValue() <= Short.MAX_VALUE && hooks.getSetLoginIndexGarbageValue() >= Short.MIN_VALUE) {
                Class<?> paramComposition = Class.forName(hooks.getSetLoginIndexClassName(), true, client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(hooks.getSetLoginIndexMethodName(), int.class, short.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (short) hooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);

            } else {
                Class<?> paramComposition = Class.forName(hooks.getSetLoginIndexClassName(), true, client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(hooks.getSetLoginIndexMethodName(), int.class, int.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, hooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            }

            log.info("Login index {} set successfully.", index);
        } catch (Exception e) {
            log.error("Failed to set login index {}", index, e);
        }
    }

    public void login(String sesionId, String characterId, String characterName) {
        loginWithJagexAccount(sesionId, characterId, characterName, true);
    }

    public void loginWithJagexAccount(String sessionId, String characterId, String characterName, boolean doLogin) {
        if(hooks == null) {
            log.info("No Auth Hooks found");
            return;
        }

        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                return;
            }

            client.setUsername("");
            client.setPassword("");

            boolean sessionInjected = false;
            boolean accountIdInjected = false;
            boolean displayNameInjected = false;

            try {
                setLoginIndex(10);
            } catch (Exception ignored) {}

            try {
                Class<?> jxSessionClass = Class.forName(hooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(hooks.getJxSessionFieldName());

                jxSessionField.setAccessible(true);
                jxSessionField.set(null, sessionId);
                jxSessionField.setAccessible(false);

                sessionInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login session:", e);
            }

            try {
                Class<?> jxAccountIdClass = Class.forName(hooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(hooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, characterId);
                jxAccountIdField.setAccessible(false);
                accountIdInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login account ID:", e);
            }

            try {
                Class<?> jxDisplayNameClass = Class.forName(hooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(hooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, characterName);
                jxDisplayNameField.setAccessible(false);

                displayNameInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login display name:", e);
            }

            if (sessionInjected && accountIdInjected && displayNameInjected && doLogin) {
                client.setGameState(GameState.LOGGING_IN);
            }
        });
    }
}
