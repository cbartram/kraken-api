package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
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
    private GrandExchangeService grandExchangeService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
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

        grandExchangeService.collect(GrandExchangeSlot.SLOT_1, false);
        SleepService.sleepFor(1000); // Wait a bit after collecting

        return true;
    }

    @Override
    protected String getTestName() {
        return "Grand Exchange Service";
    }
}
