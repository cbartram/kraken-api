package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
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
            if(!bankService.isOpen()) {
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

            Thread.sleep(RandomUtils.randomIntBetween(1200, 1600));
            log.info("Body: {}", ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).getName());
            testsPassed &= ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull();
            testsPassed &= ctx.equipment().inInventory().nameContains("scimi").first().wield();
            testsPassed &= ctx.equipment().inInventory().nameContains("plate").first().wear();
            testsPassed &= ctx.equipment().inInventory().withId(1163).first().wear();
            Thread.sleep(RandomUtils.randomIntBetween(1200, 1500));
            testsPassed &= ctx.equipment().isWearing("Rune Platebody");
            testsPassed &= ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.HEAD).remove();
            testsPassed &= !ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull();
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
