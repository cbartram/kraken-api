package com.kraken.api.interaction.container.bank;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
public class BankQuery extends AbstractQuery<BankEntity, BankQuery> {

    private int lastUpdateTick = -1;
    private final LoadingCache<Integer, ItemComposition> itemDefs;
    private final ItemManager itemManager;

    public BankQuery(Context ctx, ItemManager itemManager) {
        super(ctx);
        this.itemManager = itemManager;
        this.itemDefs = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                       @Override
                       public ItemComposition load(Integer itemId) {
                           return ctx.runOnClientThread(() -> itemManager.getItemComposition(itemId));
                       }
                   }
            );
    }

    @Override
    protected Supplier<Stream<BankEntity>> source() {
        List<BankItemWidget> bankItems = new ArrayList<>();

        // TODO This is async so I wonder if whoever calls this supplier may not get any data if bankItems is empty and returns immediately...
        ctx.runOnClientThread(() -> {
            if (lastUpdateTick < ctx.getClient().getTickCount()) {
                int i = 0;
                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.BANK);
                if(container == null) {
                    return Collections.emptyList();
                }

                for (Item item : container.getItems()) {
                    try {
                        if (item == null) {
                            i++;
                            continue;
                        }

                        if (itemDefs.get(item.getId()).getPlaceholderTemplateId() == 14401) {
                            i++;
                            continue;
                        }

                        ItemComposition comp = itemManager.getItemComposition(item.getId());
                        if(comp.getName().equalsIgnoreCase("Bank filler")) {
                            i++;
                            continue;
                        }

                        itemDefs.put(item.getId(), comp);
                        bankItems.add(new BankItemWidget(itemDefs.get(item.getId()).getName(), item.getId(), item.getQuantity(), i, ctx));
                    } catch (NullPointerException | ExecutionException ex) {
                        log.error("exception thrown while attempting to get items from bank:", ex);
                    }
                    i++;
                }
                lastUpdateTick = ctx.getClient().getTickCount();
            }
            return bankItems;
        });

        return () -> bankItems.stream().map(i -> new BankEntity(ctx, i));
    }
}