package com.kraken.api.query.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.inventory.ContainerItem;


public class BankInventoryEntity extends AbstractEntity<ContainerItem> {
    public BankInventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, true, action);
        return true;
    }

    /**
     * Deposits one of the given item from the players inventory into the bank.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositOne() {
        return deposit(1);
    }

    /**
     * Deposits five of the given items from the players inventory into the bank.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositFive() {
        return deposit(5);
    }

    /**
     * Deposits ten of the given items from the players inventory into the bank.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositTen() {
        return deposit(10);
    }

    /**
     * Deposits a set amount of the given item from the players inventory to the bank. If the amount is
     * not one of: 1, 5, or 10 then all of the given item will be deposited by default.
     * @param amount The amount of the item to deposit: 1, 5, 10, or any other integer for all of the item
     * @return True if the deposit was successful and false otherwise
     */
    public boolean deposit(int amount) {
        if(!ctx.getBankService().isOpen()) return false;
        switch(amount) {
            case 1:
                ctx.getInteractionManager().interact(raw, true, "Deposit-1");
                return true;
            case 5:
                ctx.getInteractionManager().interact(raw, true, "Deposit-5");
                return true;
            case 10:
                ctx.getInteractionManager().interact(raw, true, "Deposit-10");
                return true;
            default:
                ctx.getInteractionManager().interact(raw, true, "Deposit-All");
                return true;
        }
    }

    /**
     * Deposits all of the given item from the players inventory into the bank.
     * @return True if the deposit was successful and false otherwise.
     */
    public boolean depositAll() {
        return deposit(-1);
    }
}
