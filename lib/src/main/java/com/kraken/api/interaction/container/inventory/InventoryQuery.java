package com.kraken.api.interaction.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InventoryQuery extends AbstractQuery<InventoryEntity, InventoryQuery> {

    public InventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<InventoryEntity>> source() {
        Stream<InventoryEntity> emptyStream = Stream.empty();
        ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

        ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
        if(container == null) return () -> emptyStream;

        Widget inventory = ctx.getClient().getWidget(149, 0);
        if(inventory == null) return () -> emptyStream;

        Widget[] inventoryWidgets = inventory.getDynamicChildren();

        List<InventoryEntity> inventoryEntities = new ArrayList<>();
        for (int i = 0; i < container.getItems().length; i++) {
            final Item item = container.getItems()[i];
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition itemComposition = ctx.runOnClientThreadOptional(() -> ctx.getClient().getItemDefinition(item.getId()))
                    .orElse(null);

            if (itemComposition == null) continue;

            Widget widget = null;
            if (i < inventoryWidgets.length) {
                widget = inventoryWidgets[i];
            }

            inventoryEntities.add(new InventoryEntity(ctx, new ContainerItem(item, itemComposition, i, ctx, widget, null)));
        }

        return inventoryEntities::stream;
    }
}