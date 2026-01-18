package com.kraken.api.query.player;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class WildernessInfo {

    @Getter(AccessLevel.NONE)
    private final int combatLevel;

    private final int level;
    private final int minAttackableCombatLevel;
    private final int maxAttackableCombatLevel;

    public WildernessInfo(int level,  int combatLevel) {
        this.level = level;
        this.combatLevel = combatLevel;
        this.minAttackableCombatLevel = this.combatLevel - this.level;
        this.maxAttackableCombatLevel = this.combatLevel + this.level;
    }
}
