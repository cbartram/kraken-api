package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.ui.grandexchange.GrandExchangeService;
import com.kraken.api.service.ui.grandexchange.GrandExchangeSlot;
import com.kraken.api.service.util.SleepService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GrandExchangeServiceTest extends BaseApiTest {

    @Inject
    private Context ctx;

    @Inject
    private BankService bankService;

    @Inject
    private GrandExchangeService grandExchangeService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {

        // Setup
        NpcEntity banker = ctx.npcs().withAction("Bank").nearest();
        if(banker == null) {
            log.error("Cannot open bank to withdraw items to sell");
            return false;
        }

        if(!banker.interact("Bank")) {
            log.error("Failed to interact with bank to withdraw items to sell");
            return false;
        }

        SleepService.sleepUntil(bankService::isOpen, 5000);

        if(!bankService.isOpen()) {
            log.error("Cannot open bank, perhaps the pin is blocking interface?");
            return false;
        }

        BankEntity fireRune = ctx.bank().withName("Fire rune").first();
        if(fireRune == null) {
            log.error("No fire runes in the bank to withdraw");
            return false;
        }

        fireRune.withdraw(3);
        bankService.close();

        SleepService.sleepWhile(bankService::isOpen, 5000);

        // Execute by selling fire runes
        NpcEntity clerk = ctx.npcs().withAction("Exchange").nearest();
        if (clerk == null) {
            log.error("Could not find Grand Exchange Clerk");
            return false;
        }

        if (!clerk.interact("Exchange")) {
            log.error("Failed to interact with Grand Exchange Clerk");
            return false;
        }

        if (!SleepService.sleepUntil(grandExchangeService::isOpen, 5000)) {
            log.error("Grand Exchange interface did not open");
            return false;
        }

        GrandExchangeSlot slot = grandExchangeService.getFirstFreeSlot();
        if(slot == null) {
            log.error("No free slots to run the test.");
            return false;
        }

        GrandExchangeSlot fireRuneSellSlot = grandExchangeService.queueSellOrder(fireRune.getId(), 1);

        if(fireRuneSellSlot == null) {
            log.error("Failed to sell fire runes on the grand exchange");
            return false;
        }

        SleepService.sleepUntil(fireRuneSellSlot::isFulfilled, 20000);

        grandExchangeService.collect(fireRuneSellSlot, false);
        SleepService.sleepFor(2);

        // Buy fire runes

        return true;
    }

    @Override
    protected String getTestName() {
        return "Grand Exchange Service";
    }
}
