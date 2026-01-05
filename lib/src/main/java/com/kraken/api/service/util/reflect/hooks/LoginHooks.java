package com.kraken.api.service.util.reflect.hooks;

import com.kraken.api.service.util.reflect.hooks.model.FieldHook;
import com.kraken.api.service.util.reflect.hooks.model.MethodHook;
import lombok.Value;

@Value
public class LoginHooks {
    MethodHook setLoginIndex;
    FieldHook session;
    FieldHook accountId;
    FieldHook displayName;
    FieldHook accountCheck;
    FieldHook jagexAccountType;
    FieldHook legacyAccountType;
}
