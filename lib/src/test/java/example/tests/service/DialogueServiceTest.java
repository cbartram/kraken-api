package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.ui.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DialogueServiceTest extends BaseApiTest {

    @Inject
    private DialogueService dialogueService;

    @Inject
    private SleepService sleepService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // Setup by talking to the banker
        if(dialogueService.isDialoguePresent()) {
            log.error("Dialogue is already present before test started.");
            return false;
        }

        NpcEntity npc = ctx.npcs().within(10).withName("Banker").nearest();
        if(npc != null) {
            npc.interact("Talk-to");
        }

        log.info("Sleeping...");
        sleepService.sleep(3000, 5000);

        if(!dialogueService.isDialoguePresent()) {
            log.error("Dialogue is not present after talking to the banker.");
            return false;
        }

        if(!dialogueService.getDialogueText().equals("Good day, how may I help you?")) {
            log.error("Dialogue text is not correct after talking to the banker, got: {}", dialogueService.getDialogueText());
            return false;
        }

        boolean success = dialogueService.continueDialogue();
        if(!success) {
            log.error("Dialogue failed to continue after talking to the banker.");
            return false;
        }

        sleepService.sleep(1000, 3000);

        if(dialogueService.getDialogueText().equals("Good day, how may I help you?")) {
            log.error("Dialogue text is not correct after continuing the dialogue, got: {}", dialogueService.getDialogueText());
            return false;
        }

        // Selects "What is this place?"
        dialogueService.selectOption(5);
        sleepService.sleep(1000, 3000);

        // --- Step 1: Player asks "What is this place?" ---
        if (!dialogueService.getDialogueHeader().equals("Player")) {
            log.error("Expected Player dialogue header, got: {}", dialogueService.getDialogueHeader());
            return false;
        }

        if (!dialogueService.getDialogueText().equals("What is this place?")) {
            log.error("Expected 'What is this place?', got: {}", dialogueService.getDialogueText());
            return false;
        }

        dialogueService.continueDialogue();
        sleepService.sleep(3000, 5000);

        // --- Step 2: Banker explains "This is a branch..." ---
        // Note: checking 'contains' is safer for long strings to avoid issues with punctuation/spacing
        if (!dialogueService.getDialogueText().contains("This is a branch of the Bank of Gielinor")) {
            log.error("Expected \"Bank of Gielinor\" text, got: {}", dialogueService.getDialogueText());
            return false;
        }

        dialogueService.continueDialogue();
        sleepService.sleep(3000, 5000);

        // --- Step 3: Player asks "And what do you do?" ---
        if (!dialogueService.getDialogueHeader().equals("Player")) {
            log.error("Expected Player dialogue header for second question, got: {}", dialogueService.getDialogueHeader());
            return false;
        }
        if (!dialogueService.getDialogueText().equals("And what do you do?")) {
            log.error("Expected 'And what do you do?', got: {}", dialogueService.getDialogueText());
            return false;
        }

        dialogueService.continueDialogue();
        sleepService.sleep(3000, 5000);

        // --- Step 4: Banker explains "We will look after..." ---
        if (!dialogueService.getDialogueText().contains("We will look after your items and money")) {
            log.error("Expected explanation of services, got: {}", dialogueService.getDialogueText());
            return false;
        }

        dialogueService.continueDialogue();
        sleepService.sleep(3000, 5000);

        // --- Step 5: Verify Dialogue Closed ---
        if (dialogueService.isDialoguePresent()) {
            log.error("Dialogue should have ended, but is still present with text: {}", dialogueService.getDialogueText());
            return false;
        }

        return true;
    }

    @Override
    protected String getTestName() {
        return "Dialogue Service";
    }
}
