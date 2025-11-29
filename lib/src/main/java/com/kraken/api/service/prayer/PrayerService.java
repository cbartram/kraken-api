package com.kraken.api.service.prayer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

@Slf4j
@Singleton
public class PrayerService extends AbstractService {

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    /**
     * Wrapper method which turns a prayer on.
     * @param prayer The prayer to turn on.
     */
    public void activatePrayer(Prayer prayer) {
        toggle(prayer, true);
    }

    /**
     * Wrapper method which turns a prayer off.
     * @param prayer The prayer to turn off.
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean deactivatePrayer(Prayer prayer) {
        return toggle(prayer, false);
    }

    /**
     * Toggles a prayer on or off. This will use reflection based prayer toggles by default.
     * @param prayer The Prayer to toggle
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean toggle(Prayer prayer) {
        if(client.isPrayerActive(prayer)) {
            return toggle(prayer, false);
        } else {
            return toggle(prayer, true);
        }
    }

    /**
     * Toggles a prayer on or off.
     * @param prayer The Prayer to toggle
     * @param activate True if the prayer should be turned on and false if it should be turned off
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean toggle(Prayer prayer, boolean activate) {
        if (prayer == null) {
            return false;
        }

        InteractablePrayer prayerExtended = InteractablePrayer.of(prayer);
        if (prayerExtended == null) {
            return false;
        }

        // Check if prayer points is at 0 or level requirement not met
        if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0 ||
                client.getRealSkillLevel(Skill.PRAYER) < prayerExtended.getLevel()) {
            return false;
        }

        boolean currentlyActive = isActive(prayer);

        // Do nothing if already in desired state
        if (currentlyActive == activate) {
            return false;
        }

        Widget widget = context.getWidget(prayerExtended.getIndex());
        Point point = uiService.getClickbox(widget);
        mousePackets.queueClickPacket(point.getX(), point.getY());
        widgetPackets.queueWidgetActionPacket(prayerExtended.getIndex(), -1, -1, 1);
        return true;
    }

    /**
     * Returns true if the prayer is active and false otherwise.
     * @param prayer Prayer to check.
     * @return Boolean true if the prayer is active (on) and false otherwise.
     */
    public boolean isActive(Prayer prayer) {
        boolean basicPrayerActive = context.runOnClientThreadOptional(() -> client.isPrayerActive(prayer)).orElse(false);
        boolean inLMS = context.getVarbitValue(VarbitID.BR_INGAME) != 0;

        switch (prayer) {
            case DEADEYE:
                boolean deadeye = context.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
                return basicPrayerActive && (deadeye && !inLMS);
            case MYSTIC_VIGOUR:
                boolean mysticVigour = context.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
                return basicPrayerActive && (mysticVigour && !inLMS);
            default:
                return basicPrayerActive;
        }
    }

    public boolean deactivateAll(boolean keepPreserveOn, boolean onlyProtectionPrayers, int maxActions) {
        int actionsTaken = 0;
        for (Prayer prayer : Prayer.values()) {
            // Skip the Ruinous prayers
            if (prayer.name().contains("RP_")) {
                continue;
            }

            if (actionsTaken >= maxActions) {
                return false;
            }

            if (prayer == Prayer.PRESERVE && keepPreserveOn) {
                continue;
            }

            // If only protection prayers should be deactivated, check if this is a protection prayer
            if (onlyProtectionPrayers && !isProtectionPrayer(prayer)) {
                continue;
            }

            boolean deactivated = deactivatePrayer(prayer);
            if (deactivated) {
                actionsTaken++;
            }
        }
        return true;
    }

    public boolean deactivateAll(boolean keepPreserve) {
        return deactivateAll(keepPreserve, false, 3);
    }

    public void deactivateAll() {
        deactivateAll(false, false, 3);
    }

    public void deactivateProtectionPrayers() {
        deactivateAll(false, true, 3);
    }

    /**
     * Helper method to check if a prayer is a protection prayer.
     * @param prayer The prayer to check.
     * @return true if the prayer is a protection prayer (melee, range, or magic).
     */
    private boolean isProtectionPrayer(Prayer prayer) {
        return prayer == Prayer.PROTECT_FROM_MELEE ||
                prayer == Prayer.PROTECT_FROM_MISSILES ||
                prayer == Prayer.PROTECT_FROM_MAGIC;
    }

    public void oneTickFlick(boolean disableAll, Prayer... prayers) {
        // Check if there are any active prayers when you first start flicking
        int active = 0;
        for (Prayer prayer : Prayer.values()) {
            // Skip the Ruinous prayers
            if (prayer.name().contains("RP_")) {
                continue;
            }

            if (isActive(prayer)) {
                active++;
            }
        }

        // The way flicking works is you need to send a deactivation then activation within the same game tick
        if (active > 0) {
            if (disableAll) {
                deactivateAll(false, false, 4);
            } else {
                for (Prayer p : prayers) {
                    deactivatePrayer(p);
                }
            }
        }

        for (Prayer p2 : prayers) {
            activatePrayer(p2);
        }
    }

    /**
     * Checks if a given quick prayer is set
     * @param prayer The quick prayer to check
     * @return True if the quick prayer is set and false otherwise
     */
    public boolean isQuickPrayerSet(InteractablePrayer prayer) {
        int selectedQuickPrayersVarbit = context.getVarbitValue(4102);
        return (selectedQuickPrayersVarbit & (1 << prayer.getQuickPrayerIndex())) != 0;
    }

    /**
     * Checks if the player is out of prayer.
     * @return Returns true if the player is out of prayer and false if they still have prayer.
     */
    public boolean isOutOfPrayer() {
        return client.getBoostedSkillLevel(Skill.PRAYER) <= 0;
    }
}