package com.kraken.api.service.ui.login;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    LEGACY(2),
    JAGEX(10);

    private final int loginIndex;
}
