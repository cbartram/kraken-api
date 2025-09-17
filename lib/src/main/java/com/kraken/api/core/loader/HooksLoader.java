package com.kraken.api.core.loader;


import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.reflect.ReflectionService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Slf4j
@Singleton
public class HooksLoader {
    // Upstream hooks are located at "https://raw.githubusercontent.com/OreoCupcakes/kotori-plugins-releases/master/hooks.json";
    // TODO Need to find out how these are actually generated so we don't depend on other plugin providers.
    private static final String HOOKS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/hooks.json";

    private final Client client;
    private final Gson gson;

    @Getter
    private Hooks hooks;

    @Inject
    public HooksLoader(final Client client, final Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    public Hooks loadHooks() {
        Hooks hooks = null;
        BufferedReader reader = null;
        for (int i = 1; i <= 15; i++) {
            try {
                URL url = new URL(HOOKS_URL);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                break;
            }
            catch (Exception e1) {
                log.error("Attempt #{}. Unable to establish a connection and download the hooks from the URL.", i, e1);
            }
        }

        if (reader == null) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(client.getCanvas(),
                            "<html>Connection error. Unable to download necessary game hooks data from the Internet." +
                                    "<br>Make sure you are connected to the Internet and your proxy or VPN isn't being flagged as suspicious." +
                                    "<br><div style='color:yellow'><b><u>If you are unable load the hooks, reflection based automation like prayer helper plugins will not work!</u></b></div></html>"
                            , "Kraken Client", JOptionPane.WARNING_MESSAGE));
            return hooks;
        }

        try {
            hooks = gson.fromJson(reader, Hooks.class);
            reader.close();
        } catch (Exception e) {
            log.error("Unable to parse Hooks.json into a Hooks object.", e);
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(client.getCanvas(),
                            "Error in parsing hook.json file","Kraken Client", JOptionPane.WARNING_MESSAGE));
        }
        this.hooks = hooks;
        return this.hooks;
    }

    public void setHooks() {
        if (this.hooks == null) {
            log.error("Attempt to set hooks before loading hooks. Ensure #loadHooks() is called before this method.");
            return;
        }

        for (HookInfo hookInfo : this.hooks.getH()) {
            String hookName = hookInfo.getN();
            switch (hookName)
            {
                case "invokeMenuAction":
                    ReflectionService.setInvokeMenuActionClassName(hookInfo.getC());
                    ReflectionService.setInvokeMenuActionMethodName(hookInfo.getP());
                    ReflectionService.setInvokeMenuActionJunkValue(hookInfo.getM());
                    break;
                case "setViewportWalking":
                    ReflectionService.setViewportWalkingClassName(hookInfo.getC());
                    ReflectionService.setViewportWalkingFieldName(hookInfo.getP());
                    break;
                case "setCheckClick":
                    ReflectionService.setCheckClickClassName(hookInfo.getC());
                    ReflectionService.setCheckClickFieldName(hookInfo.getP());
                    break;
                case "setSelectedSpellWidget":
                    ReflectionService.setSelectedSpellWidgetClassName(hookInfo.getC());
                    ReflectionService.setSelectedSpellWidgetFieldName(hookInfo.getP());
                    ReflectionService.setSelectedSpellWidgetMultiplier(getObfuscatedSetter(hookInfo.getM()));
                    break;
                case "setSelectedSpellChildIndex":
                    ReflectionService.setSelectedSpellChildIndexClassName(hookInfo.getC());
                    ReflectionService.setSelectedSpellChildIndexFieldName(hookInfo.getP());
                    ReflectionService.setSelectedSpellChildIndexMultiplier(getObfuscatedSetter(hookInfo.getM()));
                    break;
                case "setSelectedSpellItemId":
                    ReflectionService.setSelectedSpellItemIDClassName(hookInfo.getC());
                    ReflectionService.setSelectedSpellItemIDFieldName(hookInfo.getP());
                    ReflectionService.setSelectedSpellItemIDMultiplier(getObfuscatedSetter(hookInfo.getM()));
                    break;
                case "menuOptionsCount":
                    ReflectionService.setMenuOptionsCountClassName(hookInfo.getC());
                    ReflectionService.setMenuOptionsCountFieldName(hookInfo.getP());
                    ReflectionService.setMenuOptionsCountMultiplier(hookInfo.getM());
                    break;
                case "menuIdentifiersArray":
                    ReflectionService.setMenuIdentifiersClassName(hookInfo.getC());
                    ReflectionService.setMenuIdentifiersFieldName(hookInfo.getP());
                    break;
                case "menuItemIdsArray":
                    ReflectionService.setMenuItemIdsClassName(hookInfo.getC());
                    ReflectionService.setMenuItemIdsFieldName(hookInfo.getP());
                    break;
                case "menuOptionsArray":
                    ReflectionService.setMenuOptionsClassName(hookInfo.getC());
                    ReflectionService.setMenuOptionsFieldName(hookInfo.getP());
                    break;
                case "menuParam0Array":
                    ReflectionService.setMenuParam0ClassName(hookInfo.getC());
                    ReflectionService.setMenuParam0FieldName(hookInfo.getP());
                    break;
                case "menuParam1Array":
                    ReflectionService.setMenuParam1ClassName(hookInfo.getC());
                    ReflectionService.setMenuParam1FieldName(hookInfo.getP());
                    break;
                case "menuTargetsArray":
                    ReflectionService.setMenuTargetsClassName(hookInfo.getC());
                    ReflectionService.setMenuTargetsFieldName(hookInfo.getP());
                    break;
                case "menuTypesArray":
                    ReflectionService.setMenuTypesClassName(hookInfo.getC());
                    ReflectionService.setMenuTypesFieldName(hookInfo.getP());
                    break;
                case "setSelectedSceneTileX":
                    ReflectionService.setSceneSelectedXClassName(hookInfo.getC());
                    ReflectionService.setSceneSelectedXFieldName(hookInfo.getP());
                    break;
                case "setSelectedSceneTileY":
                    ReflectionService.setSceneSelectedYClassName(hookInfo.getC());
                    ReflectionService.setSceneSelectedYFieldName(hookInfo.getP());
                    break;
                case "isMoving":
                    ReflectionService.setActorPathLengthClassName(hookInfo.getC());
                    ReflectionService.setActorPathLengthFieldName(hookInfo.getP());
                    ReflectionService.setActorPathLengthMultiplier(hookInfo.getM());
                    break;
                case "getActorAnimationId":
                    ReflectionService.setActorAnimationIdClassName(hookInfo.getC());
                    ReflectionService.setActorAnimationIdFieldName(hookInfo.getP());
                    ReflectionService.setActorAnimationIdMultiplier(hookInfo.getM());
                    break;
                case "getActorAnimationObject":
                    ReflectionService.setActorAnimationObjectClassName(hookInfo.getC());
                    ReflectionService.setActorAnimationObjectFieldName(hookInfo.getP());
                    break;
            }
        }

        log.info("Hooks successfully loaded into client.");
    }

    private int getObfuscatedSetter(long a) {
        return (int) modInverse(a, 4294967296L);
    }

    private long modInverse(long a, long m) {
        // Normalize a to be within the range [0, m-1] if it's negative
        a = (a % m + m) % m;

        long[] result = extendedGCD(a, m);
        long gcd = result[0];

        // If gcd(a, m) != 1, there is no inverse
        if (gcd != 1) {
            log.error("Unable to find the modular multiplicative inverse of {}.", a);
            return -1;
        } else {
            // x is the modular multiplicative inverse, ensure it's positive
            return (result[1] % m + m) % m;
        }
    }

    private long[] extendedGCD(long a, long m) {
        if (m == 0) {
            return new long[]{a, 1, 0};
        }

        long[] result = extendedGCD(m, a % m);

        long gcd = result[0];
        long x1 = result[1];
        long y1 = result[2];

        long x = y1;
        long y = x1 - (a / m) * y1;

        return new long[]{gcd, x, y};
    }
}
