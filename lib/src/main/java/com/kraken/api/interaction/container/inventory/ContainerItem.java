package com.kraken.api.interaction.container.inventory;

import com.kraken.api.Context;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Represents an item stored in an item container (either the inventory or Bank).
 */
@Slf4j
@Getter
@AllArgsConstructor
public class ContainerItem {

    @Setter
    private int quantity;
    private int id;
    private int slot;

    @Nullable
    private Widget widget;

    private List<String> equipmentActions = new ArrayList<>();
    private String name;
    private String[] inventoryActions;
    private boolean isStackable;
    private boolean isNoted;
    private boolean isTradeable;
    private ItemComposition itemComposition;
    private Context context;

    /**
     * Wearable action indexes for items that can be equipped.
     * These correspond to the ParamID values for wearable actions in the ItemComposition.
     */
    private int[] wearableActionIndexes = new int[]{
            ParamID.OC_ITEM_OP1,
            ParamID.OC_ITEM_OP2,
            ParamID.OC_ITEM_OP3,
            ParamID.OC_ITEM_OP4,
            ParamID.OC_ITEM_OP5,
            ParamID.OC_ITEM_OP6,
            ParamID.OC_ITEM_OP7,
            ParamID.OC_ITEM_OP8
    };

    public ContainerItem(Item item, ItemComposition itemComposition, int slot, Context context, Widget widget) {
        this.id = item.getId();
        this.widget = widget;
        this.quantity = item.getQuantity();
        this.slot = slot;
        this.name = itemComposition.getName();
        this.isStackable = itemComposition.isStackable();
        this.isNoted = itemComposition.getNote() == 799;
        this.context = context;
        if (this.isNoted) {
            context.runOnClientThreadOptional(() ->
                    context.getClient().getItemDefinition(itemComposition.getLinkedNoteId())
            ).ifPresent(itemDefinition -> this.isTradeable = itemDefinition.isTradeable());
        } else {
            this.isTradeable = itemComposition.isTradeable();
        }

        this.inventoryActions = itemComposition.getInventoryActions();
        this.itemComposition = itemComposition;

        context.runOnClientThreadOptional(() -> {
            addEquipmentActions(itemComposition);
            return true;
        });
    }

    /**
     * Private constructor for creating ItemModel from cached data.
     * ItemComposition will be loaded lazily when needed.
     */
    private ContainerItem(int id, int quantity, int slot, Context context) {
        this.id = id;
        this.quantity = quantity;
        this.slot = slot;
        this.context = context;


        // Initialize with defaults - will be loaded lazily
        this.name = null;
        this.isStackable = false;
        this.isNoted = false;
        this.isTradeable = false;
        this.inventoryActions = new String[0];
        this.itemComposition = null;
        this.equipmentActions = new ArrayList<>();
    }

    /**
     * Lazy loads the ItemComposition if not already loaded.
     * This ensures we can work with cached items while minimizing performance impact.
     */
    private void ensureCompositionLoaded() {
        if (itemComposition == null && id > 0) {
            itemComposition = context.runOnClientThreadOptional(() -> context.getClient().getItemDefinition(id)).orElse(null);
            if (itemComposition != null) {
                this.name = itemComposition.getName();
                this.isStackable = itemComposition.isStackable();
                this.isNoted = itemComposition.getNote() == 799;
                if (this.isNoted) {
                    context.runOnClientThreadOptional(() ->
                            context.getClient().getItemDefinition(itemComposition.getLinkedNoteId())).ifPresent(itemDefinition -> this.isTradeable = itemDefinition.isTradeable());
                } else {
                    this.isTradeable = itemComposition.isTradeable();
                }
                this.inventoryActions = itemComposition.getInventoryActions();
                context.runOnClientThreadOptional(() -> {
                    addEquipmentActions(itemComposition);
                    return true;
                });
            }
        }
    }

    /**
     * Returns the rectangle bounds of an inventory item
     * @param context API Context
     * @param client RuneLite client instance
     * @return Rectangle bounds for the bank item.
     */
    public Rectangle getBounds(Context context, Client client) {
        int[] containerIds = {
                9764864, // Inventory
                983043, // Bank Inventory
                17563648, // Bank, Pin Inventory
                19726336, // Shop Inventory
                30605312, // GE Inventory
                12582935, // Deposit Box Inventory
        };

        Widget inventory = context.runOnClientThreadOptional(() -> {
            for (int id : containerIds) {
                final Widget widget = client.getWidget(id);
                if (widget != null && widget.getDynamicChildren() != null && !widget.isHidden()) return widget;
            }
            return null;
        }).orElse(null);

        if (inventory == null) return null;

        return Arrays.stream(inventory.getDynamicChildren())
                .filter(widget -> widget.getIndex() == getSlot())
                .findFirst().map(Widget::getBounds).orElse(null);
    }

    /**
     * Gets the item name, loading composition if needed.
     * @return String the name of the inventory item
     */
    public String getName() {
        if (name == null) {
            ensureCompositionLoaded();
        }
        return name != null ? name : "Unknown Item";
    }

    /**
     * Gets whether the item is stackable, loading composition if needed.
     * @return boolean true if the item is stackable and false otherwise
     */
    public boolean isStackable() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isStackable;
    }

    /**
     * Gets whether the item is noted, loading composition if needed.
     * @return true if the item is noted and false otherwise.
     */
    public boolean isNoted() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isNoted;
    }

    /**
     * Gets whether the item is tradeable, loading composition if needed.
     * @return True if the item is tradeable and false otherwise
     */
    public boolean isTradeable() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return isTradeable;
    }

    /**
     * Gets the inventory actions, loading composition if needed.
     * @return The inventory actions for an item. i.e "Wield", "Examine", "Drop", "Use"
     */
    public String[] getInventoryActions() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return inventoryActions;
    }
    /**
     * Gets the equipment actions, loading composition if needed.
     * This returns a list of actions that can be performed on the item when equipped.
     * @return The list of actions on the equipment
     */
    public List<String> getEquipmentActions() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return equipmentActions;
    }

    /**
     * Gets the item composition, loading it if needed.
     * @return The items composition object
     */
    public ItemComposition getItemComposition() {
        if (itemComposition == null) {
            ensureCompositionLoaded();
        }
        return itemComposition;
    }

    /**
     * True if the item is food. This returns jugs of wine as food and will not return rock cakes as food.
     * @return True if the item is food and false otherwise.
     */
    public boolean isFood() {
        if (isNoted()) return false;

        String lowerName = getName().toLowerCase();
        boolean isEdible = Arrays.stream(getInventoryActions()).anyMatch(action -> action != null && action.equalsIgnoreCase("eat"));
        return (isEdible || lowerName.contains("jug of wine")) && !lowerName.contains("rock cake");
    }

    private void addEquipmentActions(ItemComposition itemComposition) {
        for (int i = 0; i < wearableActionIndexes.length; i++) {
            try {
                String value = itemComposition.getStringValue(wearableActionIndexes[i]);
                this.equipmentActions.add(value);
            } catch (Exception ex) {
                this.equipmentActions.add("");
                log.warn("Failed to get wearable action for index {} on item {}: {}", wearableActionIndexes[i], id, ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * The high alchemy price for the item
     * @return The amount of GP received when this item is high alched for gold.
     */
    public int getHaPrice() {
        return itemComposition.getHaPrice();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContainerItem other = (ContainerItem) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContainerItem {\n");
        sb.append("\tid: ").append(id).append("\n");
        sb.append("\tname: '").append(getName()).append("'\n");
        sb.append("\tquantity: ").append(quantity).append("\n");
        sb.append("\tslot: ").append(slot).append("\n");
        sb.append("\tisStackable: ").append(isStackable()).append("\n");
        sb.append("\tisNoted: ").append(isNoted()).append("\n");
        sb.append("\tisTradeable: ").append(isTradeable()).append("\n");
        sb.append("\tisFood: ").append(isFood()).append("\n");

        // Actions
        String[] invActions = getInventoryActions();
        if (invActions != null && invActions.length > 0) {
            sb.append("\tinventoryActions: [");
            for (int i = 0; i < invActions.length; i++) {
                if (invActions[i] != null && !invActions[i].isEmpty()) {
                    if (i > 0) sb.append(", ");
                    sb.append("'").append(invActions[i]).append("'");
                }
            }
            sb.append("]\n");
        }

        // Equipment actions
        if (!equipmentActions.isEmpty()) {
            sb.append("\tequipmentActions: [");
            boolean first = true;
            for (String action : equipmentActions) {
                if (action != null && !action.isEmpty()) {
                    if (!first) sb.append(", ");
                    sb.append("'").append(action).append("'");
                    first = false;
                }
            }
            sb.append("]\n");
        }

        // Composition status
        sb.append("\tcompositionLoaded: ").append(itemComposition != null).append("\n");

        sb.append("}");
        return sb.toString();
    }

    private static <T> Predicate<ContainerItem> matches(T[] values, BiPredicate<ContainerItem, T> biPredicate) {
        return item -> Arrays.stream(values).filter(Objects::nonNull).anyMatch(value -> biPredicate.test(item, value));
    }

    public static Predicate<ContainerItem> matches(boolean exact, String... names) {
        return matches(names, exact ? (item, name) -> item.getName().equalsIgnoreCase(name) :
                (item, name) -> item.getName().toLowerCase().contains(name.toLowerCase()));
    }

    public static Predicate<ContainerItem> matches(int... ids) {
        return item -> Arrays.stream(ids).anyMatch(id -> item.getId() == id);
    }

    public static Predicate<ContainerItem> matches(EquipmentInventorySlot... slots) {
        return matches(slots, (item, slot) -> item.getSlot() == slot.getSlotIdx());
    }
}
