package com.kraken.api.query.container.bank;

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
        return () -> {
            List<BankItemWidget> bankItems = ctx.runOnClientThread(() -> {
                if (lastUpdateTick < ctx.getClient().getTickCount()) {
                    List<BankItemWidget> items = new ArrayList<>();
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

                            ItemComposition comp = itemDefs.get(item.getId());
                            if (comp.getPlaceholderTemplateId() == 14401) {
                                i++;
                                continue;
                            }

                            if(comp.getName().equalsIgnoreCase("Bank filler")) {
                                i++;
                                continue;
                            }

                            items.add(new BankItemWidget(comp.getName(), item.getId(), item.getQuantity(), i, ctx));
                        } catch (NullPointerException | ExecutionException ex) {
                            log.error("exception thrown while attempting to get items from bank:", ex);
                        }
                        i++;
                    }
                    lastUpdateTick = ctx.getClient().getTickCount();
                    return items;
                }
                return Collections.emptyList();
            });

            return bankItems.stream().map(i -> new BankEntity(ctx, i));
        };
    }
}