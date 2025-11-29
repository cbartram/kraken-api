package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class EquipmentQuery extends AbstractQuery<EquipmentEntity, EquipmentQuery> {

    private final HashMap<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();

    public EquipmentQuery(Context ctx) {
        super(ctx);
        equipmentSlotWidgetMapping.put(0, 15);
        equipmentSlotWidgetMapping.put(1, 16);
        equipmentSlotWidgetMapping.put(2, 17);
        equipmentSlotWidgetMapping.put(3, 18);
        equipmentSlotWidgetMapping.put(4, 19);
        equipmentSlotWidgetMapping.put(5, 20);
        equipmentSlotWidgetMapping.put(7, 21);
        equipmentSlotWidgetMapping.put(9, 22);
        equipmentSlotWidgetMapping.put(10, 23);
        equipmentSlotWidgetMapping.put(12, 24);
        equipmentSlotWidgetMapping.put(13, 25);
    }

    @Override
    protected Supplier<Stream<EquipmentEntity>> source() {
        return () -> {
            List<EquipmentEntity> equipmentEntities = ctx.runOnClientThread(() -> {
                ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
                if(container == null) return Collections.emptyList();

                Widget inventory = ctx.getClient().getWidget(149, 0);
                if(inventory == null) return Collections.emptyList();

                Widget[] inventoryWidgets = inventory.getDynamicChildren();

                List<EquipmentEntity> entities = new ArrayList<>();
                for (int i = 0; i < container.getItems().length; i++) {
                    final Item item = container.getItems()[i];
                    if (item.getId() == -1 || item.getId() == 6512) continue;

                    final ItemComposition itemComposition = ctx.getClient().getItemDefinition(item.getId());

                    List<String> actions = List.of(itemComposition.getInventoryActions());
                    if(!actions.contains("wield") && !actions.contains("wear")) continue;

                    Widget widget = null;
                    if (i < inventoryWidgets.length) {
                        widget = inventoryWidgets[i];
                    }

                    entities.add(new EquipmentEntity(ctx, widget));
                }

                // We also need to populate the widgets from the actual equipment interface as they can also be interacted
                // with to "remove" equipment.
                for(int i = 0; i < 13; i++) {
                    Widget widget = ctx.getClient().getWidget(WidgetInfo.EQUIPMENT.getGroupId(), equipmentSlotWidgetMapping.get(i));
                    // 13 slots in your equipment to wear, if no widget found then no gear in that slot
                    if (widget == null) continue;
                    entities.add(new EquipmentEntity(ctx, widget));
                }

                return entities;
            });

            return equipmentEntities.stream();
        };
    }
}
