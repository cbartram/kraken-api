package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.util.RandomUtils;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;

@Slf4j
public class EquipmentTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            if(!ctx.inventory().hasItems("Rune full helm", "Rune scimitar", "Rune platebody")) {
                log.info("Setting up inventory for equipment tests");
                if (!bankService.isOpen()) {
                    log.error("Cannot execute bank tests, bank is not open");
                    return false;
                }

                // Setup
                bankService.depositAll();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                bankService.depositAllEquipment();
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                bankService.setWithdrawMode(false);
                ctx.bank().withName("Rune Scimitar").first().withdraw(10);
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                ctx.bank().withName("Rune Platebody").first().withdraw(10);
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                ctx.bank().withName("Rune full helm").first().withdraw(1);
                Thread.sleep(RandomUtils.randomIntBetween(400, 900));
                bankService.close();
                Thread.sleep(RandomUtils.randomIntBetween(1200, 1600));
            } else {
                log.info("Inventory already setup for equipment tests.");
            }

            for(EquipmentEntity e : ctx.equipment().inInterface().list()) {
                log.info("Equipment: {}", e.getName());
            }

            if(!ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull()) {
                log.info("Equipment tests failed, BODY slot should not have an item but is non-null");
                testsPassed = false;
            }

            if(!ctx.equipment().inInventory().nameContains("scimi").first().wield()) {
                log.info("Equipment tests failed, could not wield scimitar");
                testsPassed = false;
            }

            if(!ctx.equipment().inInventory().nameContains("plate").first().wear()) {
                log.info("Equipment tests failed, could not wield platebody");
                testsPassed = false;
            }

            if(!ctx.equipment().inInventory().withId(1163).first().wear()) {
                log.info("Equipment tests failed, could not wield platelegs");
                testsPassed = false;
            }

            Thread.sleep(RandomUtils.randomIntBetween(1200, 1500));


            if(!ctx.equipment().isWearing("Rune Platebody")) {
                log.info("Equipment tests failed, isWearing returned false for Rune Platebody but platebody should be equipped.");
                testsPassed = false;
            }

            if(!ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.HEAD).remove()) {
                log.info("Equipment tests failed, could not remove HEAD slot.");
                testsPassed = false;
            }

            if(ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull()) {
                log.info("Equipment tests failed, BODY slot is null but should be wearing rune platebody");
                testsPassed = false;
            }
        } catch (Exception e) {
            log.error("failed to run equipment test", e);
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Equipment";
    }
}
