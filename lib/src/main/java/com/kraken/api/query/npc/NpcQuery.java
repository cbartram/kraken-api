package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Actor;
import net.runelite.api.NPC;

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
                .filter(x -> x.getName() != null)
                .sorted(Comparator.comparingInt(value -> value.getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())))
                .map(rawNpc -> new NpcEntity(ctx, rawNpc));
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
            List<String> actions = Arrays.stream(npc.raw().getComposition().getActions()).map(String::toLowerCase).collect(Collectors.toList());
            return actions.contains("attack") && !npc.raw().isDead();
        });
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
     * Filters for NPCs within a specific distance.
     * @param distance The distance within which to search for NPC's.
     * @return NpcQuery
     */
    public NpcQuery withinDistance(int distance) {
        return filter(npc ->
                npc.raw().getWorldLocation().distanceTo(ctx.getClient().getLocalPlayer().getWorldLocation()) <= distance
        );
    }

    /**
     * Filters NPCs by their ID.
     * @param ids The ids of NPC's to filter for
     * @return NpcQuery
     */
    public NpcQuery withId(int... ids) {
        Set<Integer> idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet());
        return filter(npc -> idSet.contains(npc.raw().getId()));
    }

    /**
     * Filters NPCs by Name (Case insensitive).
     * @param names The names of NPC's with which to filter for
     * @return NpcQuery
     */
    public NpcQuery withName(String... names) {
        return filter(npc -> {
            String npcName = npc.getName();
            if (npcName == null) return false;
            for (String n : names) {
                if (n.equalsIgnoreCase(npcName)) return true;
            }
            return false;
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
     * Filters for NPCs that are alive (health greater than 0).
     * @return NpcQuery
     */
    public NpcQuery alive() {
        return filter(npc -> !npc.raw().isDead());
    }
}