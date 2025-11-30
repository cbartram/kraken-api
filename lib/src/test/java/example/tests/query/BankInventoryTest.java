package example.tests.query;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.util.RandomUtils;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BankInventoryTest extends BaseApiTest {

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
            bankService.setWithdrawMode(false);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            ctx.bank().withName("Swordfish").first().withdraw(10);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            ctx.bank().withName("Rune full helm").first().withdraw(1);
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));

            Map<String, String> map = new HashMap<>();
            map.put("Swordfish", "");
            map.put("Rune full helm", "");

            // Execute
            for(ContainerItem i : ctx.bankInventory().toRuneLite().collect(Collectors.toList())) {
               testsPassed &= map.containsKey(i.getName());
               log.info("Map contains: {} -> {}", i.getName(), map.containsKey(i.getName()));
            }

            ctx.bankInventory().withName("Swordfish").first().depositFive();
            Thread.sleep(RandomUtils.randomIntBetween(400, 900));
            ctx.bankInventory().nameContains("Rune").first().interact("Deposit-1");
        } catch (Exception e) {
            log.error("Exception during bank inventory test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Bank Inventory Test";
    }
}
