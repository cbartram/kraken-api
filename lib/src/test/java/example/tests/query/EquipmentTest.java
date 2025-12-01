package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.util.RandomUtils;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.widgets.Widget;

import java.util.stream.Collectors;

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
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));

            // TODO NPE here
            for(String name : ctx.equipment().inInventory().toRuneLite().map(Widget::getName).collect(Collectors.toList())) {
                log.info("Equipment name: {}", name);
            }

            testsPassed &= ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull();
            testsPassed &= ctx.equipment().inInventory().nameContains("scimitar").first().wield();
            testsPassed &= ctx.equipment().inInventory().nameContains("Plate").first().wear();
            testsPassed &= ctx.equipment().inInventory().withId(1163).first().wear();
            Thread.sleep(RandomUtils.randomIntBetween(1200, 1500));
            testsPassed &= ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.HEAD).remove();
            testsPassed &= !ctx.equipment().inInterface().inSlot(EquipmentInventorySlot.BODY).isNull();
        } catch (Exception e) {
            log.error("failed to run equipment test", e);
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Bank Inventory Test";
    }
}
