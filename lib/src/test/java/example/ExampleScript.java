package example;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ExampleScript extends Script {

    @Inject
    private Context ctx;

    @Inject
    private SleepService sleepService;

    @Override
    public int loop() {
        log.info("Looping on tick: {}", ctx.getClient().getTickCount());

        if(ctx.getClient().getTickCount() % 25 == 0) {
            int sleepTicks = RandomService.between(5, 10);
            log.info("Sleeping for: {}", sleepTicks);
            sleepService.tick(sleepTicks);
            return 100;
        }


        if(ctx.getClient().getTickCount() % 50 == 0) {
            log.info("Sleeping for 2 game ticks (with return) starting on: {}", ctx.getClient().getTickCount());
            return 1200;
        }

        return 0;
    }
}
