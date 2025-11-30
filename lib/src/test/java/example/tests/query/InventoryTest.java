package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.util.RandomUtils;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InventoryTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {
            if(!bankService.isOpen()) {
                log.error("Cannot execute inventory tests, bank is not open");
                return false;
            }

            bankService.setWithdrawMode(false);
            ctx.bank().withName("Swordfish").first().withdraw(5);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            ctx.bank().withName("Lobster").first().withdraw(5);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));


            testsPassed &= ctx.inventory().food().count() > 0;
            testsPassed &= !ctx.inventory().isEmpty();
            testsPassed &= ctx.inventory().nameContains("Sword").count() > 0;
            testsPassed &= ctx.inventory().filter(entity -> entity.getName().equalsIgnoreCase("Swordfish")).first().interact("Drop");
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            testsPassed &= ctx.inventory().food().nameContains("Lobster").first().interact("Eat");
        } catch (Exception e) {
            log.error("Exception during inventory query test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Inventory Query";
    }
}

