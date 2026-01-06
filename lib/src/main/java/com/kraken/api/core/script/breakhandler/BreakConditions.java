package com.kraken.api.core.script.breakhandler;

import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.service.bank.BankService;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.util.function.Supplier;

public class BreakConditions {

    /**
     * Breaks when a specific skill reaches a target level.
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
     * Breaks when items in the bank run out (e.g., materials).
     */
    public static BreakCondition onBankEmpty(BankService bankService, Context ctx, int itemId) {
        return new BreakCondition() {
            @Override
            public boolean shouldBreak() {
               if(bankService.isOpen()) {
                   BankEntity item = ctx.bank().withId(itemId).first();
                   if(item == null) return true;
                   return item.count() == 0;
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
