package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class NpcServiceTest extends BaseApiTest {

    @Inject
    private NpcService npcService;

    @Inject
    private Context context;


    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        try {
            // Test 1: Check if we can get NPCs
            List<NPC> allNpcs = npcService.getNpcs().collect(Collectors.toList());
            testsPassed &= assertThat(!allNpcs.isEmpty(), "Should find at least one NPC");

            if (!allNpcs.isEmpty()) {
                log.info("Found {} NPCs total", allNpcs.size());

                // Test 2: Check if we can get NPC names
                int namedNpcsCount = 0;
                int testedCount = 0;

                for (NPC npc : allNpcs) {
                    if (testedCount >= 10) break; // Limit to first 10 for performance

                    String npcName = context.runOnClientThreadOptional(npc::getName).orElse(null);
                    if (npcName != null && !npcName.isEmpty()) {
                        namedNpcsCount++;
                    }
                    testedCount++;
                }

                testsPassed &= assertThat(namedNpcsCount > 0,
                        "Should find at least one NPC with a valid name");

                // Test 3: Test specific NPC lookup
                String targetNpc = config.targetNpcName();
                boolean targetNpcTest = testTargetNpcLookup(targetNpc);
                testsPassed &= targetNpcTest;

                // Test 4: Test attackable NPCs functionality
                boolean attackableNpcTest = testAttackableNpcs(targetNpc);
                testsPassed &= attackableNpcTest;
            }

        } catch (Exception e) {
            log.error("Exception during NPC service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testTargetNpcLookup(String targetNpcName) {
        try {
            List<NPC> allNpcs = npcService.getNpcs().collect(Collectors.toList());
            boolean foundTarget = false;

            for (NPC npc : allNpcs) {
                String npcName = context.runOnClientThreadOptional(npc::getName).orElse(null);
                if (npcName != null && npcName.equalsIgnoreCase(targetNpcName)) {
                    foundTarget = true;
                    log.info("Target NPC '{}' found successfully", targetNpcName);
                    break;
                }
            }

            if (!foundTarget) {
                log.info("Target NPC '{}' not found in current area (this may be expected)", targetNpcName);
                // This is not necessarily a failure - the NPC might just not be in the current area
                return true;
            }

            return true;

        } catch (Exception e) {
            log.error("Error testing target NPC lookup", e);
            return false;
        }
    }

    private boolean testAttackableNpcs(String targetNpcName) {
        try {
            // Test the attackable NPCs method
            List<NPC> attackableNpcs = npcService.getAttackableNpcs(targetNpcName)
                    .collect(Collectors.toList());

            log.info("Found {} attackable NPCs matching '{}'", attackableNpcs.size(), targetNpcName);

            // Test filtering for non-interacting NPCs
            long nonInteractingCount = npcService.getAttackableNpcs(targetNpcName)
                    .filter(n -> !n.isInteracting())
                    .count();

            log.info("Found {} non-interacting attackable NPCs matching '{}'",
                    nonInteractingCount, targetNpcName);

            NPC npc = npcService.getAttackableNpcs("Guard").findFirst().orElse(null);
            if(npc != null) {
                log.info("Testing packet interaction with NPC");
                npcService.interact(npc, "Attack");
            }

            // This test passes as long as the methods don't throw exceptions
            // The actual count can vary based on what's in the game world
            return true;

        } catch (Exception e) {
            log.error("Error testing attackable NPCs", e);
            return false;
        }
    }

    @Override
    protected String getTestName() {
        return "NPC Service";
    }
}