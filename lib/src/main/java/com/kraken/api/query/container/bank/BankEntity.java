package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

import java.util.Optional;

public class BankEntity extends AbstractEntity<BankItemWidget> {

    private static final int WITHDRAW_QUANTITY_VARBIT = 3960;

    public BankEntity(Context ctx, BankItemWidget raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * Withdraws 1 of this item.
     */
    public boolean withdrawOne() {
        return interact("Withdraw-1");
    }

    /**
     * Withdraws 5 of this item.
     */
    public boolean withdrawFive() {
        return interact("Withdraw-5");
    }

    /**
     * Withdraws 10 of this item.
     */
    public boolean withdrawTen() {
        return interact("Withdraw-10");
    }

    /**
     * Withdraws All of this item.
     */
    public boolean withdrawAll() {
        return interact("Withdraw-All");
    }

    /**
     * Withdraws All of this item, ensuring it comes out as Notes.
     */
    public boolean withdrawAllNoted() {
        ctx.getBankService().setWithdrawMode(true);
        return interact("Withdraw-All");
    }

    /**
     * Withdraws a specific amount. Handles "Withdraw-X" logic including scripts.
     * Defaults to un-noted (Item) mode.
     */
    public boolean withdraw(int amount) {
        return withdraw(amount, false);
    }

    /**
     * Withdraws a specific amount with explicit Note mode selection.
     */
    public boolean withdraw(int amount, boolean noted) {
        if (!ctx.isPacketsLoaded()) return false;

        if (amount == 1) return setModeAndInteract(noted, "Withdraw-1");
        if (amount == 5) return setModeAndInteract(noted, "Withdraw-5");
        if (amount == 10) return setModeAndInteract(noted, "Withdraw-10");

        // 2. Handle Withdraw-X Logic
        return ctx.runOnClientThread(() -> {
            if (!ctx.getBankService().setWithdrawMode(noted)) return Optional.of(false);

            interact("Withdraw-X");

            ctx.getClient().setVarcStrValue(359, Integer.toString(amount)); // VarClientStr.INPUT_TEXT
            ctx.getClient().setVarcIntValue(5, 7); // VarClientInt.INPUT_TYPE, 7 = Bank Withdraw X Input
            ctx.getClient().runScript(681);
            // Update the client's memory of what "X" is
            ctx.getClient().setVarbit(WITHDRAW_QUANTITY_VARBIT, amount);

            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Sets the bank withdrawal mode and then interacts. Helper method for withdrawing 1, 5, or 10 items.
     */
    private boolean setModeAndInteract(boolean noted, String action) {
        return ctx.runOnClientThread(() -> {
            ctx.getBankService().setWithdrawMode(noted);
            return interact(action);
        });
    }
}
