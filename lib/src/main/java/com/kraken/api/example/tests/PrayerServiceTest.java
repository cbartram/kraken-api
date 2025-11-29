package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.kraken.api.service.prayer.PrayerService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;

@Slf4j
public class PrayerServiceTest extends BaseApiTest {

    @Inject
    private PrayerService prayer;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        try {
            boolean prayerPacketTest = testPrayerPacket();
            testsPassed &= prayerPacketTest;

            boolean testIsActive = testIsActive();
            testsPassed &= testIsActive;

        } catch (Exception e) {
            log.error("Exception during player service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testIsActive() {
        try {
            prayer.toggle(Prayer.PROTECT_FROM_MELEE, true);
            Thread.sleep(1000);
            return prayer.isActive(Prayer.PROTECT_FROM_MELEE);
        } catch (Exception e) {
            log.error("Error testing prayer isActive", e);
            return false;
        }
    }

    private boolean testPrayerPacket() {
        try {
            return prayer.toggle(Prayer.PROTECT_ITEM, true);
        } catch (Exception e) {
            log.error("Error testing prayer via packet", e);
            return false;
        }
    }

    @Override
    protected String getTestName() {
        return "Prayer Service";
    }
}

