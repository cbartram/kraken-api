package com.kraken.api.example.tests;

import com.kraken.api.interaction.gameobject.GameObjectService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileObject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class GameObjectServiceTest extends BaseApiTest {

    @Inject
    private GameObjectService gameObjectService;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;
        log.info("Finding all game objects nearby");
        testsPassed &= !gameObjectService.all().isEmpty();

        // Find nearest bank & interact
        log.info("Finding nearest bank booth");
        TileObject bankBooth = gameObjectService.all((o) -> o.getId() == 10583, 20).stream().findFirst().orElse(null);
        if (bankBooth != null) {
            log.info("Found bank booth: {}, interacting", bankBooth.getId());
            testsPassed &= gameObjectService.interact(bankBooth, "Bank");
        } else {
            testsPassed = false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "GameObject Service";
    }
}
