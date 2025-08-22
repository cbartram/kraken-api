package com.kraken.api.interaction.inventory;

import com.kraken.api.model.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryChanged {
    InventoryItem changedItem;
    int slot;
    InventoryUpdateType changeType;
}
