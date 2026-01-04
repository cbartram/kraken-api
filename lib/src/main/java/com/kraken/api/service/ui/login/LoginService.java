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

    private static final String AUTH_HOOKS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/authHooks.json";
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

    /**
     * Loads a set of auth hooks from a remote json file. These auth hooks tell the service which classes and methods
     * to invoke via reflection.
     * @param gson Gson object for deserializing the json auth hooks
     * @return AuthHooks object or null if the object could not be loaded or deserialized.
     */
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

    /**
     * Sets a login index to tell the client if the account logging in is a legacy or jagex account.
     * @param index The index to set. 10 = jagex account 2 = legacy account.
     */
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

    /**
     * Loads a profile object from a set of credentials written via the {@code --insecure-write-credentials} option
     * passed to the RuneLite client. This credentials file is expected to be called {@code credentials.properties}
     * and will be located in ~/.runelite/credentials.properties on unix systems.
     * @return Profile the loaded profile object
     */
    public Profile loadProfileFromCredentials() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(CREDENTIALS_FILE.toFile())) {
            props.load(fis);
        } catch (java.io.IOException e) {
            log.error("Failed to load credentials from {}: ", CREDENTIALS_FILE, e);
            return null;
        }

        String sessionId = props.getProperty("JX_SESSION_ID");
        String characterId = props.getProperty("JX_CHARACTER_ID");
        String characterName = props.getProperty("JX_DISPLAY_NAME");

        if (sessionId == null || characterId == null || characterName == null) {
            log.error("Credentials file {} is missing one or more required properties (JX_SESSION_ID, JX_CHARACTER_ID, JX_DISPLAY_NAME).", CREDENTIALS_FILE);
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
     * Logs the current account specified in ~/.runelite/credentials.properties into the client.
     */
    public void login() {
        login(loadProfileFromCredentials());
    }

    /**
     * Wrapper around logging in with a Jagex account. This method passes true as the doLogin param
     * to actually perform the login in the client rather than just setting the fields via reflection.
     * @param profile The profile to login as
     */
    public void login(Profile profile) {
        if(profile == null) {
            log.error("The passed jagex profile is null. Ensure credentials.properties exists in the RuneLite home dir and --insecure-write-credentials is passed to the client.");
            return;
        }

        loginWithJagexAccount(profile, true);
    }

    /**
     * Logs in with a Jagex account given the session id, character id, and character name from a {@link Profile} object.
     * @param profile The profile to login as
     * @param doLogin True when the game state should be set to logging in to actually login to the client. When set
     *                to false, the fields for logging in under a specific profile will be set accordingly but the actual login
     *                will not occur.
     */
    public void loginWithJagexAccount(Profile profile, boolean doLogin) {
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
            } catch (Exception e) {
                log.error("Failed to set the login index to 10: ", e);
            }

            try {
                Class<?> jxSessionClass = Class.forName(hooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(hooks.getJxSessionFieldName());

                jxSessionField.setAccessible(true);
                jxSessionField.set(null, profile.getSessionId());
                jxSessionField.setAccessible(false);

                sessionInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login session:", e);
            }

            try {
                Class<?> jxAccountIdClass = Class.forName(hooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(hooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, profile.getCharacterId());
                jxAccountIdField.setAccessible(false);
                accountIdInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login account ID:", e);
            }

            try {
                Class<?> jxDisplayNameClass = Class.forName(hooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(hooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, profile.getCharacterName());
                jxDisplayNameField.setAccessible(false);

                displayNameInjected = true;
            } catch (Exception e) {
                log.error("Failed to set login display name:", e);
            }

            try {
                Class<?> jxAccountTypeClass = Class.forName(hooks.getJxAccountTypeClassName(), true, client.getClass().getClassLoader());
                Field jxAccountTypeField = jxAccountTypeClass.getDeclaredField(hooks.getJxAccountTypeFieldName());
                jxAccountTypeField.setAccessible(true);
                Object jxAccountTypeObject = jxAccountTypeField.get(null);
                jxAccountTypeField.setAccessible(false);


                Class<?> clientClass = client.getClass();
                Field jxAccountCheckField = clientClass.getDeclaredField(hooks.getJxAccountCheckFieldName());
                jxAccountCheckField.setAccessible(true);
                jxAccountCheckField.set(null, jxAccountTypeObject);
                jxAccountCheckField.setAccessible(false);
            } catch (Exception e) {
                log.error("Failed to set account type field: ", e);
            }

            if (sessionInjected && accountIdInjected && displayNameInjected && doLogin) {
                client.setGameState(GameState.LOGGING_IN);
            }
        });
    }
}
