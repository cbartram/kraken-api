package example.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.spell.SpellService;
import com.kraken.api.service.spell.Spells;
import com.kraken.api.util.RandomUtils;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpellServiceTest extends BaseApiTest {

    @Inject
    private SpellService spellService;

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest(Context ctx) {
        boolean testsPassed = true;

        try {

            // Setup
            if(!bankService.isOpen()) {
                log.error("Cannot execute spell service tests, bank is not open");
                return false;
            }

            bankService.depositAll();
            ctx.bank().withName("Mind rune").first().withdrawTen();
            Thread.sleep(RandomUtils.randomIntBetween(400, 1000));
            ctx.bank().withName("Fire rune").first().withdraw(50);
            Thread.sleep(RandomUtils.randomIntBetween(400, 1000));
            ctx.bank().withName("Air rune").first().withdrawFive();
            Thread.sleep(RandomUtils.randomIntBetween(400, 1000));
            boolean hasRunes = spellService.hasRequiredRunes(Spells.VARROCK_TELEPORT);
            if(hasRunes) {
                log.info("Spell Service tests failed, hasRequiredRunes returned true when player should not have VARROCK_TELEPORT runes");
                return false;
            }
            ctx.bank().withName("Law rune").first().withdrawOne();
            bankService.close();
            Thread.sleep(RandomUtils.randomIntBetween(400, 1000));
            boolean hasRunesTrue = spellService.hasRequiredRunes(Spells.VARROCK_TELEPORT);
            if(!hasRunesTrue) {
                log.info("Spell Service tests failed, hasRequiredRunes returned false when player should have VARROCK_TELEPORT runes");
                return false;
            }


            NpcEntity guard = ctx.npcs().nameContains("Guard").nearest();
            if(guard == null) {
                log.error("Spell Service tests failed, could not find a guard");
                return false;
            }

            // spellService.cast(Spells.FIRE_STRIKE);

            spellService.cast(Spells.VARROCK_TELEPORT);
        } catch (Exception e) {
            log.error("Exception during spell service test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Spell Service";
    }
}

