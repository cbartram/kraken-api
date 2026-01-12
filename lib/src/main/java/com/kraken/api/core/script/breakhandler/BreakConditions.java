package com.kraken.api.core.script.breakhandler;

import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.container.bank.BankInventoryEntity;
import com.kraken.api.service.bank.BankService;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.util.function.Supplier;

public class BreakConditions {

    /**
     * Breaks when a specific skill reaches a target level.
     * @param client RuneLite Client
     * @param skill The Skill to track levels for
     * @param targetLevel The target level for when the break should occur when reached.
     * @return BreakCondition
     */
    public static BreakCondition onLevelReached(Client client, Skill skill, int targetLevel) {
        return new BreakCondition() {
            private boolean triggered = false;

            @Override
            public boolean shouldBreak() {
                if (triggered) return false;
                int currentLevel = client.getRealSkillLevel(skill);
                if (currentLevel >= targetLevel) {
                    triggered = true;
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return String.format("Reached %s level %d", skill.getName(), targetLevel);
            }
        };
    }

    /**
     * Breaks when experience gained exceeds a threshold.
     * @param client RuneLite client
     * @param skill The target skill for exp tracking
     * @param expThreshold The amount of exp needed to trigger the break
     * @return BreakCondition
     */
    public static BreakCondition onExperienceGained(Client client, Skill skill, int expThreshold) {
        return new BreakCondition() {
            private final int startExp = client.getSkillExperience(skill);

            @Override
            public boolean shouldBreak() {
                int currentExp = client.getSkillExperience(skill);
                return (currentExp - startExp) >= expThreshold;
            }

            @Override
            public String getDescription() {
                return String.format("Gained %d+ experience in %s", expThreshold, skill.getName());
            }
        };
    }

    /**
     * A custom break condition which can include any logic
     * @param shouldBreakCheck Supplier which returns a boolean. When true a break will occur.
     * @param reason The plain text reason for the break
     * @return BreakCondition
     */
    public static BreakCondition customCondition(Supplier<Boolean> shouldBreakCheck, String reason) {
        return new BreakCondition() {
            @Override
            public boolean shouldBreak() {
                return shouldBreakCheck.get();
            }

            @Override
            public String getDescription() {
                return reason;
            }
        };
    }

    /**
     * Wraps a condition to ensure it only triggers once per session.
     * Useful for conditions like Level Reached or Material Depleted to prevent break loops.
     * @param condition The condition to wrap
     * @return A new BreakCondition that only returns true once.
     */
    public static BreakCondition runOnce(BreakCondition condition) {
        return new BreakCondition() {
            private boolean triggered = false;

            @Override
            public boolean shouldBreak() {
                // If we already triggered this break, ignore it forever
                if (triggered) {
                    return false;
                }

                // Check the underlying condition
                if (condition.shouldBreak()) {
                    triggered = true;
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return condition.getDescription();
            }
        };
    }

    /**
     * Breaks when items in the bank run out (e.g., materials).
     * @param ctx The API game context
     * @param itemId The item id to track. When this item is no longer present in the bank or your inventory, a break will be taken.
     * @return BreakCondition
     */
    public static BreakCondition onMaterialDepleted(Context ctx, int itemId) {
        return new BreakCondition() {
            @Override
            public boolean shouldBreak() {
               if(ctx.getService(BankService.class).isOpen()) {
                   // Player does not have item in the bank or in the inventory
                   BankEntity item = ctx.bank().withId(itemId).first();
                   BankInventoryEntity inv = ctx.bankInventory().withId(itemId).first();
                   return item == null && inv == null;
               }

               return false;
            }

            @Override
            public String getDescription() {
                return "Bank material with id " + itemId + " is depleted";
            }
        };
    }

    /**
     * Breaks at a specific time of day.
     * @param hourOfDay The hour 0-24 of the day when a break should initiate
     * @param minute The minute of the hour 0-60 when a break should initiate
     * @return BreakCondition
     */
    public static BreakCondition atSpecificTime(int hourOfDay, int minute) {
        return new BreakCondition() {
            private boolean triggered = false;

            @Override
            public boolean shouldBreak() {
                if (triggered) return false;

                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalTime targetTime = java.time.LocalTime.of(hourOfDay, minute);

                if (now.isAfter(targetTime) || now.equals(targetTime)) {
                    triggered = true;
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return String.format("Reached scheduled time %02d:%02d", hourOfDay, minute);
            }
        };
    }
}
