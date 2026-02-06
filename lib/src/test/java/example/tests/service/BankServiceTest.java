package example.tests.service;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.util.SleepService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BankServiceTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean open = bankService.isOpen();

        if(open) {
            log.error("Expected bank to be closed for BankServiceTest. Start test with the bank closed.");
            return false;
        }

        log.info("Opening the bank.");
        ctx.gameObjects().nameContains("Bank booth").nearest().interact("Bank");
        SleepService.sleepWhile(() -> bankService.isClosed(), 5000);

        log.info("Depositing all items");
        if(!bankService.depositAll()) {
            log.error("Failed to deposit all inventory items.");
            return false;
        }
        SleepService.sleepFor(3);
        log.info("Depositing all equipment");
        if(!bankService.depositAllEquipment()) {
            log.error("Failed to deposit all worn equipment");
            return false;
        }

        SleepService.sleepFor(3);
        log.info("Depositing all containers");
        if(!bankService.depositContainers()) {
            log.error("Failed to deposit containers (loot bag)");
            return false;
        }

        log.info("Setting withdraw mode to: NOTED");
        if(!bankService.setWithdrawMode(true)) {
            log.error("Failed to set withdraw mode to: NOTED");
            return false;
        }

        SleepService.sleepFor(3);
        log.info("Setting withdraw mode to: ITEM");
        if(!bankService.setWithdrawMode(false)) {
            log.error("Failed to set withdraw mode to: ITEM");
            return false;
        }

        if(!bankService.close()) {
            log.error("Failed to close bank.");
            return false;
        }

        return true;
    }

    @Override
    protected String getTestName() {
        return "Bank Service";
    }
}
