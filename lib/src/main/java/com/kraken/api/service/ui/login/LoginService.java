package com.kraken.api.service.ui.login;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.service.util.reflect.ReflectionService;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import com.kraken.api.service.util.reflect.hooks.LoginHooks;
import com.kraken.api.service.util.reflect.hooks.loader.HookLoader;
import com.kraken.api.service.util.reflect.hooks.model.FieldHook;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
@Singleton
public class LoginService {

    private LoginHooks hooks;
    private final ReflectionService reflectionService;
    private final Client client;
    private final ClientThread clientThread;

    private static final Path CREDENTIALS_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("credentials.properties");

    @Inject
    public LoginService(ReflectionService reflectionService, Client client, ClientThread clientThread) {
        try {
            HookRegistry registry = HookLoader.load();
            this.hooks = registry.getLogin();
        } catch (Exception e) {
            log.error("Failed to load reflection hooks. Reflection operations like login state injection will not work: ", e);
            this.hooks = null;
        }
        this.reflectionService = reflectionService;
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Sets the login screen index using reflection.
     * Handles garbage value parameters automatically via ReflectionService.
     */
    private void setLoginIndex(int index) {
        log.debug("Setting login index to {}", index);
        reflectionService.invoke(hooks.getSetLoginIndex(), null, index);
    }

    /**
     * Loads profile credentials from the RuneLite credentials file.
     * @return Profile the character profile loaded from the credentials file.
     */
    public Profile loadProfileFromCredentials() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CREDENTIALS_FILE.toFile())) {
            props.load(fis);
        } catch (IOException e) {
            log.error("Failed to load credentials from {}", CREDENTIALS_FILE, e);
            return null;
        }

        String sessionId = props.getProperty("JX_SESSION_ID");
        String characterId = props.getProperty("JX_CHARACTER_ID");
        String characterName = props.getProperty("JX_DISPLAY_NAME");

        if (sessionId == null || characterId == null || characterName == null) {
            log.warn("Missing required Jagex account credentials in {}", CREDENTIALS_FILE);
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
     * Loads Jagex account credentials and logs into the client.
     * Does NOT support legacy accounts. Use {@link #loginWithLegacyAccount} instead.
     */
    public void login() {
        Profile profile = loadProfileFromCredentials();
        if (profile != null) {
            loginWithJagexAccount(profile, true);
        } else {
            log.error("Failed to load profile from credentials");
        }
    }

    /**
     * Logs into a Jagex account using the provided profile.
     *
     * @param profile Profile containing Jagex account credentials
     * @param doLogin If true, triggers actual login; if false, only sets fields
     */
    public void loginWithJagexAccount(Profile profile, boolean doLogin) {
        if (profile == null) {
            log.error("Cannot login with null profile");
            return;
        }
        applyLoginState(AccountType.JAGEX, profile, doLogin);
    }

    /**
     * Logs into the game client using legacy (username/password) credentials.
     *
     * @param username The username to login with
     * @param password The password to login with
     * @param doLogin If true, triggers actual login; if false, only sets fields
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
     * All reflection complexity is handled by ReflectionService.
     */
    private void applyLoginState(AccountType type, Profile profile, boolean doLogin) {
        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                log.warn("Cannot apply login state - not on login screen");
                return;
            }

            // Set username/password based on account type
            if (type == AccountType.JAGEX) {
                client.setUsername("");
                client.setPassword("");
            } else {
                client.setUsername(profile.getUsername());
                client.setPassword(profile.getPassword());
            }

            boolean success = true;

            // Set the login screen index
            try {
                setLoginIndex(type.getLoginIndex());
            } catch (Exception e) {
                log.error("Failed to set login index", e);
                success = false;
            }

            // Inject Jagex-specific fields (session, account ID, display name)
            if (type == AccountType.JAGEX) {
                success &= setFieldSafely(hooks.getSession(), profile.getSessionId(), "session ID");
                success &= setFieldSafely(hooks.getAccountId(), profile.getCharacterId(), "account ID");
                success &= setFieldSafely(hooks.getDisplayName(), profile.getCharacterName(), "display name");
            } else {
                // For legacy accounts, clear these fields
                success &= setFieldSafely(hooks.getSession(), null, "session ID");
                success &= setFieldSafely(hooks.getAccountId(), null, "account ID");
                success &= setFieldSafely(hooks.getDisplayName(), null, "display name");
            }

            // Set the account type check field
            success &= setAccountTypeCheck(type);

            // Trigger login if all injections succeeded
            if (success && doLogin) {
                log.info("Login state applied successfully, triggering login for {} account", type);
                client.setGameState(GameState.LOGGING_IN);
            } else if (!success) {
                log.error("Login state application failed, aborting login");
            }
        });
    }

    /**
     * Sets a static field value with error handling and logging.
     *
     * @return true if successful, false otherwise
     */
    private boolean setFieldSafely(FieldHook hook, Object value, String fieldDescription) {
        try {
            reflectionService.setFieldValue(hook, null, value);
            log.debug("Set {} successfully", fieldDescription);
            return true;
        } catch (Exception e) {
            log.error("Failed to set {}", fieldDescription, e);
            return false;
        }
    }

    /**
     * Sets the account type check field by reading from the appropriate source field
     * (Jagex vs Legacy) and copying it to the client.
     *
     * This handles the complex logic of:
     * 1. Reading the account type object from either jagexAccountType or legacyAccountType
     * 2. Writing it to the client's accountCheck field
     */
    private boolean setAccountTypeCheck(AccountType type) {
        try {
            // Determine which hook to read from based on account type
            FieldHook sourceHook = (type == AccountType.JAGEX)
                    ? hooks.getJagexAccountType()
                    : hooks.getLegacyAccountType();

            // Read the account type object from the static field
            Object accountTypeObject = reflectionService.getFieldValue(sourceHook, null);

            if (accountTypeObject == null) {
                log.error("Failed to read account type object from {}", sourceHook);
                return false;
            }

            // Write it to the client's account check field
            reflectionService.setFieldValue(hooks.getAccountCheck(), null, accountTypeObject);
            log.debug("Set account type check for {} account", type);
            return true;

        } catch (Exception e) {
            log.error("Failed to set account type check field", e);
            return false;
        }
    }
}