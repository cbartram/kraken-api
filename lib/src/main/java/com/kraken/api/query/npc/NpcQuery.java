package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NpcQuery extends AbstractQuery<NpcEntity, NpcQuery, NPC> {

    public NpcQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<NpcEntity>> source() {
        return () -> ctx.getClient().getTopLevelWorldView().npcs().stream()
                .filter(Objects::nonNull)
                .filter(n -> n.getName() != null && n.getId() != -1)
                .map(rawNpc -> new NpcEntity(ctx, rawNpc));
    }

    /**
     * Returns a stream of NPC's at a given world point.
     * @param location The world point to return
     * @return NpcQuery
     */
    public NpcQuery at(WorldPoint location) {
        return filter(n -> n.raw().getWorldLocation().equals(location));
    }

    /**
     * Returns Attackable NPC within the scene.
     * NPC's are considered attackable when:
     * - They are not dead
     * - Their menu options contain an "Attack" option
     * <p>
     * Combat level is not taken into consideration since there are many NPC's without a combat level that are attackable.
     * i.e. Yama's void flares
     * @return NpcQuery
     */
    public NpcQuery attackable() {
        return filter(npc -> {
            NPCComposition composition = npc.raw().getComposition();
            if(composition == null) return false;
            if(composition.getActions() == null || composition.getActions().length == 0) return false;

            List<String> actions = Arrays.stream(composition.getActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            return actions.contains("attack") && !npc.raw().isDead();
        });
    }

    /**
     * Filters for only NPC's which are reachable from the players current tile. The player must be able to
     * reach the NPC's true southwest tile in order for this to include the NPC in the resulting stream.
     * @return NpcQuery
     */
    public NpcQuery reachable() {
        return filter(npc -> npc.raw() != null && ctx.getTileService().isTileReachable(npc.raw().getWorldLocation()));
    }

    /**
     * Filters for NPCs that are not interacting with anyone (null interaction).
     * This covers "not interacting with me" AND "not interacting with others".
     * @return NpcQuery
     */
    public NpcQuery idle() {
        return filter(npc -> npc.raw().getInteracting() == null);
    }

    /**
     * Sorts the results by distance to the local player.
     * Usage: ctx.npcs().withName("Goblin").nearest().first();
     * @return NpcQuery
     */
    public NpcQuery nearest() {
        return sorted(Comparator.comparingInt(npc ->
                npc.raw().getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())
        ));
    }

    /**
     * Sorts the results by distance to the specified point.
     * Usage: ctx.npcs().withName("Goblin").nearestTo(new WorldPoint(2056, 8143, 0)).first();
     * @param location The World point to use as a distance anchor.
     * @return NpcQuery
     */
    public NpcQuery nearestTo(WorldPoint location) {
        return sorted(Comparator.comparingInt(npc ->
                npc.raw().getWorldLocation().distanceTo(location)
        ));
    }

    /**
     * Filters for NPCs within a specific distance.
     * @param distance The distance within which to search for NPC's.
     * @return NpcQuery
     */
    public NpcQuery within(int distance) {
        return filter(npc ->
                npc.raw().getWorldLocation().distanceTo(ctx.getClient().getLocalPlayer().getWorldLocation()) <= distance
        );
    }

    /**
     * Filters for NPC's within a specified area. The min WorldPoint should be the southwest tile of the area
     * and the max WorldPoint should be the northeast tile of the area.
     * @param min The southwest minimum world point of the area to check
     * @param max The northeast maximum world point of the area to check
     * @return NpcQuery
     */
    public NpcQuery withinArea(WorldPoint min, WorldPoint max) {
        int x1 = min.getX();
        int x2 = max.getX();
        int y1 = min.getY();
        int y2 = max.getY();

        return filter(npc -> {
            WorldPoint pt = npc.raw().getWorldLocation();
            int x3 = pt.getX();
            int y3 = pt.getY();

            if (x3 > Math.max(x1, x2) || x3 < Math.min(x1, x2)) {
                return false;
            }

            return y3 <= Math.max(y1, y2) && y3 >= Math.min(y1, y2);
        });
    }

    /**
     * Filters for NPCs that are interacting with the local player (e.g., attacking me).
     * @return NpcQuery
     */
    public NpcQuery interactingWithPlayer() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters for NPCs that are interacting with other players (not the local player).
     * @return NpcQuery
     */
    public NpcQuery interacting() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target != ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters the stream for NPCs which are interacting with a specified {@code Actor}
     * @param actor The actor to check NPC interactions against
     * @return NpcQuery
     */
    public NpcQuery interactingWith(Actor actor) {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == actor;
        });
    }

    /**
     * Filters for NPCs that are alive (health greater than 0).
     * @return NpcQuery
     */
    public NpcQuery alive() {
        return filter(npc -> !npc.raw().isDead());
    }
}