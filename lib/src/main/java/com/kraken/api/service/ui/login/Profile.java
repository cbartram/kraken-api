package com.kraken.api.service.ui.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Profile {

    @Builder.Default
    private String identifier = "";

    @Builder.Default
    private boolean isJagexAccount = true;

    @Builder.Default
    private String username = "";

    @Builder.Default
    private String password = "";

    @Builder.Default
    private String characterName = "";

    @Builder.Default
    private String sessionId = "";

    @Builder.Default
    private String characterId = "";

    @Builder.Default
    private String bankPin = "";
}
