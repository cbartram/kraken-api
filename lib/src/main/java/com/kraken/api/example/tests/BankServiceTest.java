package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.bank.BankService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BankServiceTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Override
    protected boolean runTest() {
        boolean testPassed = true;

        if(bankService.isOpen()) {
            log.info("Bank is open.");
            bankService.depositAll();
            try {
                Thread.sleep(500); // Wait for the deposit action to complete
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bankService.withdraw("Swordfish", false, "Withdraw-1");
        } else {
            log.info("Bank is not open.");
            testPassed = false;
        }

        return testPassed;
    }

    @Override
    protected String getTestName() {
        return "BankService Test";
    }
}
