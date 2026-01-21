package example.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.prayer.PrayerService;
import com.kraken.api.service.util.SleepService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;

@Slf4j
public class PrayerServiceTest extends BaseApiTest {

    @Inject
    private PrayerService prayer;

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {
            boolean prayerPacketTest = testPrayerPacket();
            testsPassed &= prayerPacketTest;

            boolean testIsActive = testIsActive(Prayer.PROTECT_FROM_MELEE);
            testsPassed &= testIsActive;

            boolean testRange = testIsActive(Prayer.PROTECT_FROM_MISSILES);
            testsPassed &= testRange;

            boolean testMagic = testIsActive(Prayer.PROTECT_FROM_MAGIC);
            testsPassed &= testMagic;

            SleepService.sleep(1000, 3000);
            log.info("Deactivating protection prayers");
            prayer.deactivateProtectionPrayers();

            SleepService.sleep(1000, 3000);
            log.info("Deactivating all prayers");
            prayer.deactivateAll();

        } catch (Exception e) {
            log.error("Exception during prayer service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testIsActive(Prayer p) {
        try {
            prayer.toggle(p, true);
            SleepService.sleep(3000, 5000);
            return prayer.isActive(p);
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

