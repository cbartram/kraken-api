package com.kraken.api.interaction.groundobject;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Data
@Getter
@Builder
public class GroundItem {
    @Nullable
    private Instant spawnTime;

    private int id;
    private int itemId;
    private String name;
    private int quantity;
    private WorldPoint location;
    private int height;
    private int haPrice;
    private int gePrice;
    private int offset;
    private boolean tradeable;
    private int ownership;
    private boolean isPrivate;
    private boolean stackable;
    private Duration despawnTime;
    private Duration visibleTime;

    public int getAlchemyPrice() {
        return haPrice * quantity;
    }

    public int getGrandExchangePrice() {
        return gePrice * quantity;
    }

    @SneakyThrows
    public static String[] getGroundItemActions(ItemComposition item) {
        List<Field> fields = Arrays.stream(item.getClass().getFields()).filter(x -> x.getType().isArray()).collect(Collectors.toList());
        for (Field field : fields) {
            if (field.getType().getComponentType().getName().equals("java.lang.String")) {
                String[] actions = (String[]) field.get(item);
                if (Arrays.stream(actions).anyMatch(x -> x != null && x.equalsIgnoreCase("take"))) {
                    field.setAccessible(true);
                    return actions;
                }
            }
        }
        return new String[]{};
    }
}
