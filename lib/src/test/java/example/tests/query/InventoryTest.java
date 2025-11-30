package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
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

            // TODO Write the tests
            ctx.inventory().food().count();
            testsPassed &= false;
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

