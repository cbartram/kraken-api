package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.SleepService;
import com.kraken.api.interaction.container.bank.BankService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BankServiceTest extends BaseApiTest {

    @Inject
    private BankService bankService;

    @Inject
    private SleepService sleepService;

    @Override
    protected boolean runTest() {
        boolean testPassed = true;

        if(bankService.isOpen()) {
            log.info("Bank is open.");
            bankService.depositAll();
            sleepService.sleep(120, 600);
            bankService.withdraw("Rune platebody", false, "Withdraw-1");
            sleepService.sleep(120, 600);
            bankService.withdraw("Rune scimitar", false, "Withdraw-1");
            sleepService.sleep(2000, 3000);
            bankService.depositOne("Rune platebody");
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
