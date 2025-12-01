package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.gameval.VarbitID;

@Slf4j
public class BankEntity extends AbstractEntity<BankItemWidget> {

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

    @Override
    public int getId() {
        return raw.getItemId();
    }

    /**
     * Withdraws 1 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawOne() {
        return interact("Withdraw-1");
    }

    /**
     * Withdraws 5 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawFive() {
        return interact("Withdraw-5");
    }

    /**
     * Withdraws 10 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawTen() {
        return interact("Withdraw-10");
    }

    /**
     * Withdraws All of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawAll() {
        return interact("Withdraw-All");
    }

    /**
     * Withdraws All of this item, ensuring it comes out as Notes.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawAllNoted() {
        return ctx.runOnClientThread(() -> {
            ctx.getBankService().setWithdrawMode(true);
            return interact("Withdraw-All");
        });
    }

    /**
     * Withdraws a specific amount. Handles "Withdraw-X" logic including scripts.
     * Defaults to un-noted (Item) mode.
     * @param amount The amount of the item to withdraw
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdraw(int amount) {
        return withdraw(amount, false);
    }

    /**
     * Withdraws a specific amount with explicit Note mode selection.
     * @param amount The amount of the item to withdraw: 1, 5, or 10. Any other value will withdraw X of that value
     * @param noted True if the items should be withdrawn as notes and false if they should be withdrawn as items
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdraw(int amount, boolean noted) {
        if (!ctx.isPacketsLoaded()) return false;

        if (amount == 1) return setModeAndInteract(noted, "Withdraw-1");
        if (amount == 5) return setModeAndInteract(noted, "Withdraw-5");
        if (amount == 10) return setModeAndInteract(noted, "Withdraw-10");

        return ctx.runOnClientThread(() -> {
            if (!ctx.getBankService().setWithdrawMode(noted)) return false;

            // If user is trying to withdraw 500 and the X value is already set to 500 then just queue the packet
            // for that menu option "Withdraw-500" instead of setting Withdraw-X and then setting 500 again.
            int quantitySet = ctx.getVarbitValue(VarbitID.BANK_REQUESTEDQUANTITY);
            if(quantitySet == amount) {
                Point pt = ctx.getInteractionManager().getUiService().getClickbox(raw);
                if(pt != null) {
                    ctx.getInteractionManager().getMousePackets().queueClickPacket(pt.getX(), pt.getY());
                    ctx.getInteractionManager().getWidgetPackets().queueWidgetAction(raw, "Withdraw-" + amount);
                    ctx.getInteractionManager().getWidgetPackets().queueResumeCount(amount);
                }
                return true;
            }

            // Need to queue an additional resume count action here to set the amount in the chatbox
            interact("Withdraw-X");
            ctx.getInteractionManager().getWidgetPackets().queueResumeCount(amount);

            ctx.getClient().setVarcStrValue(359, Integer.toString(amount)); // VarClientStr.INPUT_TEXT
            ctx.getClient().setVarcIntValue(5, 7); // VarClientInt.INPUT_TYPE, 7 = Bank Withdraw X Input
            ctx.getClient().runScript(681);
            // Update the client's memory of what "X" is
            ctx.getClient().setVarbit(VarbitID.BANK_REQUESTEDQUANTITY, amount);
            return true;
        });
    }

    /**
     * Sets the bank withdrawal mode and then interacts. Helper method for withdrawing 1, 5, or 10 items.
     * @return true if the withdrawal was successful and false otherwise
     */
    private boolean setModeAndInteract(boolean noted, String action) {
        return ctx.runOnClientThread(() -> {
            boolean withdrawModeSet = ctx.getBankService().setWithdrawMode(noted);
            if(!withdrawModeSet) return false;
            return interact(action);
        });
    }
}
