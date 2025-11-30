package example.tests.query;


import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.bank.BankService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BankTest extends BaseApiTest {

    @Inject
    BankService bankService;

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {
            if(!bankService.isOpen()) {
                log.error("Cannot execute bank tests, bank is not open");
                return false;
            }

            // Test for substring contains with platelegs, platebody, plateskirt etc...
            long plateCount = ctx.bank().nameContains("plate").stream().count();
            testsPassed &= plateCount > 0;
            log.info("Items with name: \"plate\" exist: {}", testsPassed);
            testsPassed &= !ctx.bank().withName("Rune Platebody").first().isNull();
            log.info("Rune Platebody exists: {}", testsPassed);
            testsPassed &= ctx.bank().withName("Rune Platelegs").first().withdraw(1, true);
            testsPassed &= ctx.bank().withName("Rune full helm").first().withdraw(1, false);
            testsPassed &= ctx.bank().withId(373).first().withdraw(4, false); // swordfish
        } catch (Exception e) {
            log.error("Exception during bank query test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Bank Query";
    }
}
