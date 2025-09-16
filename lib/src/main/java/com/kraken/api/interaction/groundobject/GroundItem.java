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
    private String key;
    private ItemComposition itemComposition;
    private TileObject tileObject;
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

    /**
     * Returns the High Alchemy price of the item multiplied by the quantity on the ground.
     * @return The High Alchemy price of the item multiplied by the quantity on the ground
     */
    public int getAlchemyPrice() {
        return haPrice * quantity;
    }

    /**
     * Returns the Grand Exchange price of the item multiplied by the quantity on the ground.
     * @return The Grand Exchange price of the item multiplied by the quantity on the ground
     */
    public int getGrandExchangePrice() {
        return gePrice * quantity;
    }

    /**
     * Returns a list of actions which can be performed on the Tile object. Generally this will be "Take" or "Examine".
     * @param item The item composition of the ground item to get actions for
     * @return A list of actions which can be performed on the tile object
     */
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
