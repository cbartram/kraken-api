package com.kraken.api.query.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.NPCPackets;
import com.kraken.api.service.ui.UIService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
@Deprecated
public class NpcService extends AbstractService {

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private NPCPackets npcPackets;

    /**
     * Retrieves a stream of NPCs filtered by a given condition.
     *
     * <p>This method filters NPCs based on the specified predicate, allowing for flexible
     * selection of NPCs based on various attributes such as name, interaction status, health, etc.</p>
     *
     * @param predicate A {@link Predicate} that defines the filtering condition for NPCs.
     * @return A sorted {@link Stream} of {@link NPC} objects that match the given predicate.
     */
    public Stream<NPC> getNpcs(Predicate<NPC> predicate) {
        Optional<List<NPC>> npcs = context.runOnClientThreadOptional(() -> context.getClient().getTopLevelWorldView().npcs().stream()
                .filter(Objects::nonNull)
                .filter(x -> x.getName() != null)
                .filter(predicate)
                .sorted(Comparator.comparingInt(value -> value.getLocalLocation().distanceTo(context.getClient().getLocalPlayer().getLocalLocation())))
                .collect(Collectors.toList()));

        return npcs.map(Collection::stream).orElse(null);
    }

    /**
     * Interacts with a given NPC given the NPC's name and an action using packets.
     * @param name NPC name
     * @param actions Action to take: "Attack"
     * @return True if the interaction was successful and false otherwise
     */
    public boolean interact(String name, String... actions) {
        return interact(n -> {
            String npcName = context.runOnClientThreadOptional(n::getName).orElse(null);
            return npcName != null && npcName.equalsIgnoreCase(name);
        }, actions);
    }

    /**
     * Interacts with a given NPC given the NPC's id and an action using packets.
     * @param id NPC id
     * @param actions Action to take: "Attack"
     * @return True if the interaction was successful and false otherwise
     */
    public boolean interact(int id, String... actions) {
        return interact(n -> n.getId() == id, actions);
    }

    /**
     * Interacts with the first NPC which matches the passed predicate using the given action with packets.
     * @param predicate Predicate to filter NPC's
     * @param actions Action to take: "Attack"
     * @return True if the interaction was successful and false otherwise
     */
    public boolean interact(Predicate<NPC> predicate, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        NPC npc = getNpcs(predicate).findFirst().orElse(null);
        if(npc == null) return false;

        Point clickingPoint = uiService.getClickbox(npc);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        npcPackets.queueNPCAction(npc, actions);
        return true;
    }

    /**
     * Interacts with a given NPC given the NPC's index and an action using packets.
     * @param index NPC index
     * @param actions Action to take: "Attack"
     * @return True if the interaction was successful and false otherwise
     */
    public boolean interactIndex(int index, String... actions) {
        return interact(n -> n.getIndex() == index, actions);
    }

    /**
     * Interacts with a given NPC given the NPC and multiple actions using packets.
     * @param npc The NPC to interact with
     * @param actions Action to take: "Attack"
     * @return True if the interaction was successful and false otherwise
     */
    public boolean interact(NPC npc, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        if (npc == null) {
            return false;
        }

        NPCComposition comp = context.runOnClientThreadOptional(npc::getComposition).orElse(null);
        if (comp == null) {
            return false;
        }

        Point clickingPoint = uiService.getClickbox(npc);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        npcPackets.queueNPCAction(npc, actions);
        return true;
    }

    /**
     * Retrieves a stream of all NPCs in the game world.
     *
     * <p>This method is a shorthand for {@link #getNpcs(Predicate)} that retrieves all
     * NPCs without applying any filtering conditions.</p>
     *
     * @return A sorted {@link Stream} of all {@link NPC} objects in the game world.
     */
    public Stream<NPC> getNpcs() {
        return getNpcs(npc -> true);
    }

    /**
     * Retrieves a stream of NPCs filtered by name.
     *
     * <p>This method searches for NPCs with a specified name and filters them based on
     * whether the match should be exact or allow partial matches.</p>
     *
     * <p>Filtering behavior:</p>
     * <ul>
     *   <li>If {@code exact} is {@code true}, the NPC name must match exactly (case insensitive).</li>
     *   <li>If {@code exact} is {@code false}, the NPC name must contain the given name (case insensitive).</li>
     * </ul>
     *
     * @param name  The name of the NPC to search for.
     * @param exact {@code true} to match the name exactly, {@code false} to allow partial matches.
     * @return A {@link Stream} of {@link NPC} objects that match the given name criteria.
     */
    public Stream<NPC> getNpcs(String name, boolean exact) {
        if (name == null || name.isEmpty()) return Stream.empty();
        return getNpcs(npc -> {
            String npcName = npc.getName();
            if (npcName == null || npcName.isEmpty()) return false;
            return exact ? npcName.equalsIgnoreCase(name) : npcName.toLowerCase().contains(name.toLowerCase());
        });
    }

    /**
     * Retrieves a stream of NPCs filtered by partial name match.
     *
     * <p>This method is a shorthand for {@link #getNpcs(String, boolean)} with partial matching enabled.</p>
     *
     * @param name The name of the NPC to search for.
     * @return A {@link Stream} of {@link NPC} objects whose names contain the given string.
     */
    public Stream<NPC> getNpcs(String name) {
        return getNpcs(name, false);
    }

    /**
     * Retrieves a stream of NPCs filtered by their ID.
     *
     * @param id The unique identifier of the NPC to search for.
     * @return A {@link Stream} of {@link NPC} objects that match the given NPC ID.
     */
    public Stream<NPC> getNpcs(int id) {
        return getNpcs().filter(x -> x.getId() == id);
    }

    /**
     * Retrieves a stream of attackable NPCs.
     *
     * <p>This method filters NPCs based on the following conditions:</p>
     * <ul>
     *   <li>The NPC has a combat level greater than 0.</li>
     *   <li>The NPC is not dead.</li>
     *   <li>The NPC is not currently interacting with another entity unless the player is in a multi-combat area.</li>
     * </ul>
     *
     * <p>The resulting stream of NPCs is sorted by proximity to the player, with closer NPCs appearing first.</p>
     *
     * @return A sorted {@link Stream} of {@link NPC} objects that the player can attack.
     */
    public Stream<NPC> getAttackableNpcs() {
        return getNpcs(npc -> npc.getCombatLevel() > 0 && !npc.isDead())
                .filter(npc -> !npc.isInteracting())
                .sorted(Comparator.comparingInt(value ->
                        value.getLocalLocation().distanceTo(
                                context.getClient().getLocalPlayer().getLocalLocation())));
    }

    /**
     * Retrieves a stream of attackable NPCs based on specified criteria.
     *
     * <p>This method filters NPCs based on the following conditions:</p>
     * <ul>
     *   <li>The NPC has a combat level greater than 0.</li>
     *   <li>The NPC is not dead.</li>
     *   <li>If {@code reachable} is {@code true}, the NPC must be reachable from the player's current location.</li>
     *   <li>The NPC is either not interacting with any entity or is interacting with the local player.</li>
     * </ul>
     *
     * <p>The resulting stream of NPCs is sorted by proximity to the player, with closer NPCs appearing first.</p>
     *
     * @param reachable If {@code true}, only include NPCs that are reachable from the player's current location.
     *                  If {@code false}, include all NPCs matching the other criteria regardless of reachability.
     * @return A sorted {@link Stream} of {@link NPC} objects that the player can attack.
     */
    public Stream<NPC> getAttackableNpcs(boolean reachable) {
        return getNpcs(npc -> npc.getCombatLevel() > 0
                && !npc.isDead()
                && (!reachable || context.getClient().getLocalPlayer().getWorldLocation().distanceTo(npc.getWorldLocation()) < Integer.MAX_VALUE)
                && (!npc.isInteracting() || Objects.equals(npc.getInteracting(), context.getClient().getLocalPlayer())))
                .sorted(Comparator.comparingInt(value ->
                        value.getLocalLocation().distanceTo(
                                context.getClient().getLocalPlayer().getLocalLocation())));
    }

    /**
     * Retrieves a stream of attackable NPCs filtered by name.
     *
     * <p>This method first filters NPCs based on attackable criteria, then applies name filtering:</p>
     * <ul>
     *   <li>The NPC must meet the conditions defined in {@link #getAttackableNpcs()}.</li>
     *   <li>If {@code exact} is {@code true}, the NPC name must match exactly (case insensitive).</li>
     *   <li>If {@code exact} is {@code false}, the NPC name must contain the given name (case insensitive).</li>
     * </ul>
     *
     * @param name  The name of the NPC to search for.
     * @param exact {@code true} to match the name exactly, {@code false} to allow partial matches.
     * @return A sorted {@link Stream} of {@link NPC} objects that match the given name and are attackable.
     */
    public Stream<NPC> getAttackableNpcs(String name, boolean exact) {
        if (name == null || name.isEmpty()) return Stream.empty();
        return getAttackableNpcs().filter(npc -> {
            Optional<String> npcName = context.runOnClientThreadOptional(npc::getName);
            return npcName.map(s -> exact ? s.equalsIgnoreCase(name) : s.toLowerCase().contains(name.toLowerCase())).orElse(false);
        });
    }

    public Stream<NPC> getAttackableNpcs(String name) {
        return getAttackableNpcs(name, false);
    }


    /**
     * Checks if this NPC is within a specified distance from the player.
     * Uses client thread for safe access to player location.
     *
     * @param maxDistance Maximum distance in tiles
     * @param npc The npc to check distance for
     * @return true if within distance, false otherwise
     */
    public boolean isWithinDistanceFromPlayer(NPC npc, int maxDistance) {
        return context.runOnClientThreadOptional(() -> npc.getLocalLocation().distanceTo(
                context.getClient().getLocalPlayer().getLocalLocation()) <= maxDistance).orElse(false);
    }

    /**
     * Gets the distance from this NPC to the player.
     * Uses client thread for safe access to player location.
     * @param npc The npc to check distance for
     *
     * @return Distance in tiles
     */
    public int getDistanceFromPlayer(NPC npc) {
        return context.runOnClientThreadOptional(() -> npc.getLocalLocation().distanceTo(
                context.getClient().getLocalPlayer().getLocalLocation())).orElse(Integer.MAX_VALUE);
    }

    /**
     * Checks if this NPC is within a specified distance from a given location.
     *
     * @param npc The npc to check within distance
     * @param anchor The anchor point
     * @param maxDistance Maximum distance in tiles
     * @return true if within distance, false otherwise
     */
    public boolean isWithinDistance(NPC npc, WorldPoint anchor, int maxDistance) {
        if (anchor == null) return false;
        return npc.getWorldLocation().distanceTo(anchor) <= maxDistance;
    }

    /**
     * Checks if this NPC is currently interacting with the player.
     * Uses client thread for safe access to player reference.
     * @param npc The npc to check if interacting with player
     * @return true if interacting with player, false otherwise
     */
    public boolean isInteractingWithPlayer(NPC npc) {
        return context.runOnClientThreadOptional(() -> npc.getInteracting() == context.getClient().getLocalPlayer()).orElse(false);
    }

    /**
     * Checks if this NPC is currently moving.
     * @param npc The npc to check if currently moving
     * @return true if moving, false if idle
     */
    public boolean isMoving(NPC npc) {
        return context.runOnClientThreadOptional(() ->
                npc.getPoseAnimation() != npc.getIdlePoseAnimation()
        ).orElse(false);
    }

    /**
     * Gets the health percentage of this NPC.
     * @param npc The npc health to check
     * @return Health percentage (0-100), or -1 if unknown
     */
    public double getHealthPercentage(NPC npc) {
        int ratio = npc.getHealthRatio();
        int scale = npc.getHealthScale();

        if (scale == 0) return -1;
        return (double) ratio / (double) scale * 100.0;
    }

    /**
     * Retrieves the corresponding {@link MenuAction} for a given interaction index.
     *
     * <p>This method determines which {@link MenuAction} should be used based on the provided
     * menu index. It follows this order:</p>
     * <ul>
     *   <li>If a widget is currently selected, {@link MenuAction#WIDGET_TARGET_ON_NPC} is used.</li>
     *   <li>If {@code index} is 0, the first NPC menu option is used.</li>
     *   <li>If {@code index} is 1, the second NPC menu option is used.</li>
     *   <li>If {@code index} is 2, the third NPC menu option is used.</li>
     *   <li>If {@code index} is 3, the fourth NPC menu option is used.</li>
     *   <li>If {@code index} is 4, the fifth NPC menu option is used.</li>
     * </ul>
     *
     * @param index The menu index corresponding to the NPC interaction option (0-4).
     * @return The corresponding {@link MenuAction}, or {@code null} if the index is invalid.
     */
    @Nullable
    private MenuAction getMenuAction(int index) {
        if (client.isWidgetSelected()) {
            return MenuAction.WIDGET_TARGET_ON_NPC;
        }

        switch (index) {
            case 0:
                return MenuAction.NPC_FIRST_OPTION;
            case 1:
                return MenuAction.NPC_SECOND_OPTION;
            case 2:
                return MenuAction.NPC_THIRD_OPTION;
            case 3:
                return MenuAction.NPC_FOURTH_OPTION;
            case 4:
                return MenuAction.NPC_FIFTH_OPTION;
            default:
                return null;
        }
    }

    /**
     * Returns the head icon for an NPC if it exists. A head icon would be a prayer the NPC is prayer:
     * i.e. Hunlleff's prayers or Nex's deflect melee.
     * @param npc NPC to check
     * @return HeadIcon
     */
    @SneakyThrows
    public static HeadIcon getHeadIcon(final NPC npc) {
        if (npc.getOverheadSpriteIds() == null) {
            log.info("Npc has no overhead sprites.");
            return null;
        }

        for (int i = 0; i < npc.getOverheadSpriteIds().length; i++) {
            int overheadSpriteId = npc.getOverheadSpriteIds()[i];
            if (overheadSpriteId == -1) continue;
            return HeadIcon.values()[overheadSpriteId];
        }

        log.debug("Found overheadSpriteIds: {} but failed to find valid overhead prayer.", Arrays.toString(npc.getOverheadSpriteIds()));
        return null;
    }
}
