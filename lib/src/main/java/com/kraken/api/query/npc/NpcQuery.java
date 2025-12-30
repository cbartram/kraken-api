package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
                .filter(n -> ctx.runOnClientThread(() -> n.getName() != null && n.getId() != -1))
                .map(rawNpc -> new NpcEntity(ctx, rawNpc));
    }

    /**
     * Filters and returns an {@code NpcQuery} containing NPCs located at the specified world point.
     * <p>
     * This method applies a filter to include only NPCs whose world location matches the given {@code location}.
     *
     * @param location the {@code WorldPoint} representing the target location to filter NPCs by.
     * @return an {@code NpcQuery} containing NPCs at the specified {@code location}.
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
     * Filters the NPCs to include only those that are reachable based on their world location.
     * <p>
     * This method applies a filter to the NPC query, ensuring that each NPC's raw data existence
     * is validated and their world location is checked for reachability using the tile service.
     * </p>
     *
     * @return A {@literal @}NpcQuery containing only the NPCs that are reachable.
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
     * Retrieves the nearest NPC entity to the local player's current position.
     *
     * <p>
     * This method determines the NPC closest to the local player by comparing
     * the distances between each NPC's local location and the local player's local location.
     * The comparison is performed by sorting the NPCs based on their proximity,
     * and the first (closest) NPC is selected.
     * </p>
     *
     * <p>
     * The result is typically used to quickly identify and interact with the most
     * immediate NPC relative to the player's current position, which can assist
     * in various gameplay interactions.
     * </p>
     *
     * @return The {@code NpcEntity} nearest to the local player's current position,
     *         as determined based on the shortest distance in the local coordinate system.
     *         If no NPCs are available, the return value may be {@code null}.
     */
    public NpcEntity nearest() {
        return sorted(Comparator.comparingInt(npc ->
                npc.raw().getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())
        )).first();
    }

    /**
     * Sorts the current query results by determining the distance of each NPC's {@code WorldPoint} to a specified
     * {@code WorldPoint} location and arranging them in ascending order of proximity.
     *
     * <p>
     * This method calculates the distance between the provided {@code location} and each NPC's world location.
     * The resulting query will contain NPCs sorted such that those closest to the specified location appear first.
     * </p>
     *
     * @param location The {@link WorldPoint} to which NPCs' distances will be calculated.
     *                 This parameter defines the point of reference for sorting NPCs by proximity.
     * @return A {@code NpcQuery} containing NPCs sorted by their proximity to the specified {@code location}.
     */
    public NpcQuery nearestTo(WorldPoint location) {
        return sorted(Comparator.comparingInt(npc ->
                npc.raw().getWorldLocation().distanceTo(location)
        ));
    }

    /**
     * Sorts the NPC stream by distance from the local players' current location.
     * @return NpcQuery
     */
    public NpcQuery sortByDistance() {
        final WorldPoint playerLoc = ctx.players().local().raw().getWorldLocation();
        return sorted(Comparator.comparingInt(obj -> obj.raw().getWorldLocation().distanceTo(playerLoc)));
    }

    /**
     * Filters the NPCs in the query to include only those within a specified distance from the local player's position.
     * <p>
     * This method calculates the distance between each NPC's world location and the local player's current world
     * location, including only those NPCs with a distance less than or equal to the specified value.
     * </p>
     *
     * @param distance The maximum distance (in tiles) from the local player within which NPCs should be included.
     *                 This value must be greater than or equal to 0.
     * @return A filtered {@code NpcQuery} containing only the NPCs within the specified distance from the local player.
     */
    public NpcQuery within(int distance) {
        return filter(npc ->
                npc.raw().getWorldLocation().distanceTo(ctx.getClient().getLocalPlayer().getWorldLocation()) <= distance
        );
    }

    /**
     * Filters the query for NPCs that have a specific menu option available.
     * <p>
     * This method checks the list of actions associated with each NPC's composition.
     * If the specified {@code option} matches any of the available actions (case-insensitive),
     * the NPC will be included in the resulting query.
     * </p>
     *
     * @param action The menu option to check for, e.g., {@literal @}Attack, {@literal @}Talk-to, {@literal @}Use, etc...
     *               The input is case-insensitive.
     * @return A filtered {@code NpcQuery} containing only the NPCs that match the specified menu option.
     */
    public NpcQuery withAction(String action) {
        return filter(npc -> {
            NPCComposition composition = npc.raw().getComposition();
            if(composition == null) return false;

            List<String> actions = Arrays.stream(composition.getActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            return actions.contains(action.toLowerCase());
        });
    }

    /**
     * Filters the query to include only NPCs that are located within a specified rectangular area.
     * The area is defined by two corner points, {@code min} (lower-left) and {@code max} (upper-right),
     * in the world map grid.
     *
     * <p>
     * NPCs are included in the resulting query if their world point lies within the bounds created
     * by the two corner points. The bounds are inclusive of the edges. This allows querying
     * NPCs that exist within a specific area of interest.
     * </p>
     *
     * @param min The {@link WorldPoint} representing the lower-left corner of the area.
     *            This defines one bound of the rectangular query range.
     * @param max The {@link WorldPoint} representing the upper-right corner of the area.
     *            This defines the opposite bound of the rectangular query range.
     * @return A filtered {@code NpcQuery} containing only the NPCs located within the specified area.
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
     * Filters the query to include only NPCs that are currently interacting with the local player.
     * <p>
     * An NPC is considered to be interacting with the local player if the NPC's {@code interacting}
     * target is non-{@code null} and matches the client’s local player.
     * </p>
     *
     * <ul>
     *   <li>This method allows narrowing down the query to find NPCs that are actively engaged with
     *       the local player, whether by combat, dialogue, or other forms of interaction.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with the local player.
     */
    public NpcQuery interactingWithPlayer() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters the query to include only NPCs that are currently interacting with any entity other than the local player.
     * <p>
     * An NPC is considered to be interacting if its {@code interacting} target is non-{@code null} and does not match
     * the local player. This includes NPCs that are engaged with other players, NPCs, or other entities in any form
     * of interaction (e.g., combat, dialogue, etc.).
     * </p>
     *
     * <ul>
     *   <li>This method helps identify NPCs that are actively engaged in an interaction within the game world,
     *   excluding those interacting directly with the local player.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with entities other than
     * the local player.
     */
    public NpcQuery interacting() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target != ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters the query to include only NPCs that are currently interacting with the specified {@code actor}.
     * <p>
     * An NPC is considered to be interacting with the given actor if the NPC's {@code interacting} target
     * is non-{@code null} and matches the provided {@code actor}.
     * </p>
     * <ul>
     *   <li>This method is useful for identifying NPCs actively engaging with a specific actor, such as
     *   another player, NPC, or inanimate entity.</li>
     * </ul>
     *
     * @param actor The {@link Actor} that the NPCs being searched for should be interacting with.
     *              Passing {@code null} as the parameter is not allowed.
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with the specified actor.
     */
    public NpcQuery interactingWith(Actor actor) {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == actor;
        });
    }

    /**
     * Filters the query to include only NPCs that are currently alive.
     * <p>
     * An NPC is considered to be alive if its internal state indicates it is not dead.
     * This method applies a filter to exclude NPCs marked as dead from the result set.
     * </p>
     *
     * <ul>
     *   <li>NPCs included in the resulting query are capable of interaction or action
     *       within the game environment.</li>
     *   <li>This filter helps narrow the query to focus only on viable, active NPCs.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are alive.
     */
    public NpcQuery alive() {
        return filter(npc -> !npc.raw().isDead());
    }
}