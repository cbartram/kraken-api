package com.kraken.api.interaction.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InventoryQuery extends AbstractQuery<InventoryEntity, InventoryQuery> {

    public InventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<InventoryEntity>> source() {
        return () -> {
            List<InventoryEntity> inventoryEntities = ctx.runOnClientThread(() -> {
                ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
                if(container == null) return Collections.emptyList();

                Widget inventory = ctx.getClient().getWidget(149, 0);
                if(inventory == null) return Collections.emptyList();

                Widget[] inventoryWidgets = inventory.getDynamicChildren();

                List<InventoryEntity> entities = new ArrayList<>();
                for (int i = 0; i < container.getItems().length; i++) {
                    final Item item = container.getItems()[i];
                    if (item.getId() == -1 || item.getId() == 6512) continue;

                    final ItemComposition itemComposition = ctx.getClient().getItemDefinition(item.getId());
                    if (itemComposition == null) continue;

                    Widget widget = null;
                    if (i < inventoryWidgets.length) {
                        widget = inventoryWidgets[i];
                    }

                    entities.add(new InventoryEntity(ctx, new ContainerItem(item, itemComposition, i, ctx, widget, null)));
                }

                return entities;
            });

            return inventoryEntities.stream();
        };
    }
}