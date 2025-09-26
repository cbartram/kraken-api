package com.kraken.api.sim.model;

import lombok.*;

import java.awt.*;

/**
 * NPC entity class used within the simulation. The follow params can be acquired from the NPC composition object
 * and are used to determine attack ranges and speed.
 * STAB_ATTACK(0),
 *   SLASH_ATTACK(1),
 *   CRUSH_ATTACK(2),
 *   MAGIC_ATTACK(3),
 *   RANGED_ATTACK(4),
 *   STAB_DEFENCE(5),
 *   SLASH_DEFENCE(6),
 *   CRUSH_DEFENCE(7),
 *   MAGIC_DEFENCE(8),
 *   RANGED_DEFENCE(9),
 *   MELEE_STRENGTH(10),
 *   PRAYER_BONUS(11),
 *   RANGED_STRENGTH(12),
 *   MAGIC_STRENGTH(65),
 *   ATTACK_RANGE(13),
 *   ATTACK_SPEED(14),
 *   STAT_REQUIRED_LEVEL_1(23),
 *   STAT_REQUIRED_TYPE_2(434),
 *   STAT_REQUIRED_LEVEL_2(436),
 *   STAT_REQUIRED_TYPE_3(435),
 *   STAT_REQUIRED_LEVEL_3(437),
 *   SARADOMIN(40),
 *   ZAMORAK(41),
 *   GUTHIX(42),
 *   DEMONBANE(128),
 *   COX_POTION(181),
 *   DEGRADES_IN_COMBAT(346),
 *   SLAYER_HELM(385),
 *   LAST_MAN_STANDING(403),
 *   POTION(468),
 *   SILVER_STRENGTH(518),
 *   SEED(709),
 *   AMMO(1563),
 *   DIZANAS_QUIVER(1910),
 *   ALWAYS_DROP_ID(46),
 *   DRAGONBANE(190),
 *   GOLEMBANE(1178),
 *   KALPHITE(1353),
 */
@Data
@RequiredArgsConstructor
public class SimNpc {
    @NonNull
    private Point position;

    @NonNull
    private Color color;

    private String name = "Unknown";
    private int size = 1;
    private AttackStyle attackStyle = AttackStyle.MELEE;
    private int attackRange = 1;
    private int attackSpeed = 4;
    private boolean canPathfind = false;
    private boolean isAggressive = false;
    private Point target;

    public SimNpc(@NonNull Point position, @NonNull Color color, String name) {
        this.position = position;
        this.color = color;
        this.name = name;

        if(this.name == null || this.name.isEmpty()) {
            this.name = "Unknown";
        }
    }
}

