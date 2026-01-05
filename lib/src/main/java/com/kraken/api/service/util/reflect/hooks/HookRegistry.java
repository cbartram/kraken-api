package com.kraken.api.service.util.reflect.hooks;

import lombok.Value;

/**
 * Immutable registry containing all reflection hooks.
 * This is the single source of truth that services inject.
 */
@Value
public class HookRegistry {
    LoginHooks login;
    MouseHooks mouse;
}