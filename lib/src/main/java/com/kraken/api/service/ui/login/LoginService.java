package com.kraken.api.service.ui.login;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

@Slf4j
@Singleton
public class LoginService {

    private final AuthHooks hooks;
    private final Client client;
    private final ClientThread clientThread;

    private static final String AUTH_HOOKS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/reflection_hooks.json";
    private static final Path CREDENTIALS_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("credentials.properties");

    @Inject
    public LoginService(Client client, ClientThread clientThread, Gson gson) {
        this.client = client;
        this.clientThread = clientThread;
        this.hooks = loadAuthHooks(gson);

        if(this.hooks == null) {
            log.warn("Failed to load the auth hooks json file. Subsequent calls to login() will fail.");
        }
    }

    private AuthHooks loadAuthHooks(Gson gson) {
        try (okhttp3.Response response = new okhttp3.OkHttpClient().newCall(new okhttp3.Request.Builder().url(AUTH_HOOKS_URL).build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return gson.fromJson(response.body().charStream(), AuthHooks.class);
            }
        } catch (java.io.IOException e) {
            log.error("Failed to load auth hooks: ", e);
        }
        return null;
    }

    // [setLoginIndex method remains unchanged, omitted for brevity]
    private void setLoginIndex(int index) {
        if(hooks == null) { log.info("No Auth Hooks found"); return; }
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

    public Profile loadProfileFromCredentials() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(CREDENTIALS_FILE.toFile())) {
            props.load(fis);
        } catch (java.io.IOException e) {
            log.error("Failed to load credentials from {}", CREDENTIALS_FILE, e);
            return null;
        }

        String sessionId = props.getProperty("JX_SESSION_ID");
        String characterId = props.getProperty("JX_CHARACTER_ID");
        String characterName = props.getProperty("JX_DISPLAY_NAME");

        if (sessionId == null || characterId == null || characterName == null) {
            return null;
        }

        return Profile.builder()
                .isJagexAccount(true)
                .characterId(characterId)
                .sessionId(sessionId)
                .characterName(characterName)
                .build();
    }

    /**
     * Loads the Jagex account credentials (session id, character id, and display name from ~/.runelite/credentials.properties)
     * and logs into the client. This method does NOT support legacy accounts. Use {@link #loginWithLegacyAccount} to
     * login using legacy accounts.
     */
    public void login() {
        loginWithJagexAccount(loadProfileFromCredentials(), true);
    }

    /**
     * Logs into a Jagex account using a provided profile
     * @param profile Profile to use to login to the client
     * @param doLogin True if the login should actually occur. When this is false the fields will be set for logging in, but
     *                no login will actually occur into the game client. This is useful when switching accounts but not logging in.
     */
    public void loginWithJagexAccount(Profile profile, boolean doLogin) {
        if(profile == null) {
            log.error("The passed profile is null.");
            return;
        }
        applyLoginState(AccountType.JAGEX, profile, doLogin);
    }

    /**
     * Logs into the game client using a legacy (username/password) account.
     * @param username The username to login with
     * @param password The password to login with
     * @param doLogin True if the login should actually occur. When this is false the fields will be set for logging in, but
     *                no login will actually occur into the game client. This is useful when switching accounts but not logging in.
     */
    public void loginWithLegacyAccount(String username, String password, boolean doLogin) {
        Profile legacyProfile = Profile.builder()
                .username(username)
                .password(password)
                .isJagexAccount(false)
                .build();

        applyLoginState(AccountType.LEGACY, legacyProfile, doLogin);
    }

    /**
     * Unified method to inject login data via reflection.
     */
    private void applyLoginState(AccountType type, Profile profile, boolean doLogin) {
        if (hooks == null) {
            log.info("No Auth Hooks found");
            return;
        }

        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                return;
            }

            if (type == AccountType.JAGEX) {
                client.setUsername("");
                client.setPassword("");
            } else {
                client.setUsername(profile.getUsername());
                client.setPassword(profile.getPassword());
            }

            boolean success = true;

            try {
                setLoginIndex(type.getLoginIndex());
            } catch (Exception e) {
                log.error("Failed to set login index: ", e);
                success = false;
            }

            String targetSession = (type == AccountType.JAGEX) ? profile.getSessionId() : null;
            String targetAccountId = (type == AccountType.JAGEX) ? profile.getCharacterId() : null;
            String targetDisplayName = (type == AccountType.JAGEX) ? profile.getCharacterName() : null;

            success &= injectField(hooks.getJxSessionClassName(), hooks.getJxSessionFieldName(), targetSession, "session");
            success &= injectField(hooks.getJxAccountIdClassName(), hooks.getJxAccountIdFieldName(), targetAccountId, "account ID");
            success &= injectField(hooks.getJxDisplayNameClassName(), hooks.getJxDisplayNameFieldName(), targetDisplayName, "display name");


            try {
                // Determine which hook class/field contains the source object we need (Legacy vs Jagex value)
                String sourceClassName = (type == AccountType.JAGEX) ? hooks.getJxJagexValueClassName() : hooks.getJxLegacyValueClassName();
                String sourceFieldName = (type == AccountType.JAGEX) ? hooks.getJxJagexValueFieldName() : hooks.getJxLegacyValueFieldName();

                // Get the static object from the source
                Class<?> sourceClass = Class.forName(sourceClassName, true, client.getClass().getClassLoader());
                Field sourceField = sourceClass.getDeclaredField(sourceFieldName);
                sourceField.setAccessible(true);
                Object accountTypeObject = sourceField.get(null);
                sourceField.setAccessible(false);

                // Inject it into the client
                Class<?> clientClass = client.getClass();
                Field jxAccountCheckField = clientClass.getDeclaredField(hooks.getJxAccountCheckFieldName());
                jxAccountCheckField.setAccessible(true);
                jxAccountCheckField.set(null, accountTypeObject);
                jxAccountCheckField.setAccessible(false);
            } catch (Exception e) {
                log.error("Failed to set account type check field: ", e);
                success = false;
            }

            // 5. Trigger login
            if (success && doLogin) {
                client.setGameState(GameState.LOGGING_IN);
            }
        });
    }

    /**
     * Helper to reduce reflection boilerplate for a simple static field setting
     */
    private boolean injectField(String className, String fieldName, Object value, String logName) {
        try {
            Class<?> clazz = Class.forName(className, true, client.getClass().getClassLoader());
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
            return true;
        } catch (Exception e) {
            log.error("Failed to set login {}:", logName, e);
            return false;
        }
    }
}