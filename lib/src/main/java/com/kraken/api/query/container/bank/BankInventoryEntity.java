package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.bank.BankService;


public class BankInventoryEntity extends AbstractEntity<ContainerItem> {
    public BankInventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        ContainerItem raw = raw();
        return raw != null ? raw.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        ContainerItem raw = raw();

        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    @Override
    public int getId() {
        ContainerItem raw = raw();
        return raw != null ? raw.getId() : -1;
    }

    /**
     * Returns the quantity of the item in your inventory when the bank interface is open.
     * @return Int the quantity of the bank inventory entity (this will be the stack size of the item if its noted) or
     * the value of the item if it's coins.
     */
    public int count() {
        ContainerItem raw = raw();
        return raw != null ? raw.getQuantity() : -1;
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
        if(!ctx.getService(BankService.class).isOpen()) return false;
        ContainerItem raw = raw();

        switch(amount) {
            case 1:
                ctx.getInteractionManager().interact(raw, "Deposit-1");
                return true;
            case 5:
                ctx.getInteractionManager().interact(raw, "Deposit-5");
                return true;
            case 10:
                ctx.getInteractionManager().interact(raw, "Deposit-10");
                return true;
            default:
                ctx.getInteractionManager().interact(raw, "Deposit-All");
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


    /**
     * Attempts to wear an item in the inventory while the bank interface is open.
     * <p>
     * The action is typically used to equip wearable items
     * such as armor or accessories from the player's bank inventory.
     * <p>
     * Note: This method does not validate whether the "Wear" action is supported
     * for the current bank inventory entity, nor does it handle cases where this
     * action fails or is unavailable. Validation and exception handling should be
     * implemented as needed in upstream logic.
     *
     * @return {@code true} if the "Wear" action is invoked without encountering any
     * internal errors. The return value does not guarantee the success of the action
     * within the context of the game.
     */
    public boolean wear() {
        // TODO This doesn't validate that the wear action exists on this bank inventory entity.
        ctx.getInteractionManager().interact(raw, "wear");
        return true;
    }

    /**
     * Attempts to wield an item in the inventory while the bank interface is open {@code BankInventoryEntity}.
     *
     * <p>
     * This action is typically used to equip items such as weapons or tools
     * from the player's bank inventory.
     * <p>
     * <strong>Note:</strong> This method does not validate if the "Wield" action
     * is supported for the current bank inventory entity or if the action
     * is successful within the game. Additional validation and error handling
     * should be implemented in the calling logic as necessary.
     * </p>
     *
     * @return {@code true} if the "Wield" interaction is invoked successfully
     * without any internal client error. The return value does not guarantee
     * that the action completes as intended within the game.
     */
    public boolean wield() {
        // TODO This doesn't validate that the wield action exists on this bank inventory entity.
        ctx.getInteractionManager().interact(raw, "wield");
        return true;
    }
}
