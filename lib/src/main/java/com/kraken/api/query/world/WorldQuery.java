package com.kraken.api.query.world;


import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.World;
import net.runelite.api.WorldType;
import net.runelite.client.RuneLite;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
public class WorldQuery extends AbstractQuery<WorldEntity, WorldQuery, World> {

    public WorldQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<WorldEntity>> source() {
        return () -> {
            WorldService worldService = RuneLite.getInjector().getInstance(WorldService.class);
            WorldResult worlds = worldService.getWorlds();

            if (worlds == null) return Stream.empty();
            return worlds.getWorlds().stream().map(w -> {
                final World rsWorld = ctx.getClient().createWorld();
                rsWorld.setActivity(w.getActivity());
                rsWorld.setAddress(w.getAddress());
                rsWorld.setId(w.getId());
                rsWorld.setPlayerCount(w.getPlayers());
                rsWorld.setLocation(w.getLocation());
                rsWorld.setTypes(WorldUtil.toWorldTypes(w.getTypes()));

                // Store a reference to the net.runelite.http.api.World instance because it contains region information
                // as well
                return new WorldEntity(ctx, rsWorld, w);
            });
        };
    }

    @Override
    public WorldQuery withName(String name) {
        int worldNum;
        try {
            worldNum = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            log.error("Failed to parse world number from name: {}, expected name to be the world number.", name);
            return this;
        }

        int finalWorldNum = worldNum;
        return filter(w -> w.getId() == finalWorldNum);
    }

    @Override
    public WorldQuery nameContains(String search) {
        // For worlds with id = 341 and search = "3", it should include all worlds 300 to 399
        // if search is "39" it should include world 39 and 390-399, etc...
        try {
            int searchNum = Integer.parseInt(search);
            return filter(w -> String.valueOf(w.getId()).contains(search) ||
                    (searchNum >= 0 && searchNum <= 9 && w.getId() / 100 == searchNum) || // e.g., search "3" matches 3xx worlds
                    (searchNum >= 10 && searchNum <= 99 && w.getId() / 10 == searchNum) || // e.g., search "39" matches 39x worlds
                    (searchNum >= 100 && searchNum <= 999 && w.getId() == searchNum) // e.g., search "341" matches 341
            );
        } catch (NumberFormatException e) {
            // If search is not a number, fall back to activity name contains
            return filter(w -> w.raw().getActivity().toLowerCase().contains(search.toLowerCase()));
        }
    }

    /**
     * Filters the current query to include only free-to-play worlds.
     * <p>
     * This method modifies the query to exclude any worlds classified as {@code MEMBERS}-only.
     * Free-to-play worlds are worlds that do not have the {@code WorldType.MEMBERS} tag.
     * </p>
     * @return A {@code WorldQuery} object filtered to include only free-to-play worlds.
     */
    public WorldQuery freeToPlay() {
        return filter(w -> !w.raw().getTypes().contains(WorldType.MEMBERS));
    }

    /**
     * Filters the current query to include only members-only worlds.
     * <p>
     * This method modifies the query to include only worlds classified as {@literal @WorldType.MEMBERS}.
     * Members-only worlds are worlds that require an active membership subscription to access.
     * </p>
     *
     * @return A {@code WorldQuery} object filtered to include only members-only worlds.
     */
    public WorldQuery members() {
        return filter(w -> w.raw().getTypes().contains(WorldType.MEMBERS));
    }

    /**
     * Filters the current query to include only worlds that match any of the specified {@code WorldType} values.
     * <p>
     * This method modifies the query to include worlds where their list of types contains at least one of the provided
     * {@code WorldType} values. If the world's types are {@code null}, it will be excluded from the filtered results.
     * </p>
     *
     * @param types An array of {@code WorldType} values to filter worlds by.
     *              Only worlds containing one or more of the specified types will be included in the results.
     * @return A {@code WorldQuery} object filtered to include only worlds matching the specified types.
     */
    public WorldQuery withTypes(WorldType... types) {
        return filter(w -> w.raw().getTypes() != null && w.raw().getTypes().stream().anyMatch(t -> ArrayUtils.contains(types, t)));
    }


    /**
     * Filters the query to exclude worlds that contain any of the specified types.
     *
     * <p>This method ensures that the resulting query does not include any worlds
     * where the types match those provided in the {@code types} parameter.</p>
     *
     * @param types an array of {@literal @}WorldType that specifies the types to exclude from the query.
     *              If {@code null} or empty, no types will be excluded.
     * @return a {@literal @}WorldQuery instance with the applied filter to exclude the specified types.
     */
    public WorldQuery withOutTypes(WorldType... types) {
        return filter(w -> w.raw().getTypes() != null && w.raw().getTypes().stream().noneMatch(t -> ArrayUtils.contains(types, t)));
    }

    /**
     * Filters the current query to include only worlds with a skill total requirement
     * less than or equal to the specified value.
     *
     * <p>
     * This method modifies the query to include only worlds that:
     * <ul>
     *   <li>Have the {@literal @WorldType.SKILL_TOTAL} tag in their list of types.</li>
     *   <li>Have a skill total requirement, determined by numeric parsing of the
     *       world&apos;s activity description.</li>
     * </ul>
     * If these conditions are not met for a world, it is excluded from the results.
     * </p>
     *
     * @param total The maximum skill total value (inclusive) to include in the filtered results.
     *              Worlds with a skill total requirement greater than this value will be excluded.
     * @return A {@code WorldQuery} object filtered to include only worlds with a skill total
     *         requirement less than or equal to the specified total.
     */
    public WorldQuery onlySkillTotal(int total) {
        return filter(w -> {
            if (!w.raw().getTypes().contains(WorldType.SKILL_TOTAL) || w.raw().getActivity() == null) {
                return false;
            }

            int minLevel = 0;
            try {
                minLevel = Integer.parseInt(w.raw().getActivity().replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ignored) {}

            return minLevel <= total;
        });
    }

    /**
     * Filters the current query to include only worlds with valid skill total level requirements
     * that the local player's total skill level satisfies.
     *
     * <p>
     * This method modifies the query to include only worlds meeting the following conditions:
     * <ul>
     *   <li>The world has the {@literal @WorldType.SKILL_TOTAL} tag in its list of types.</li>
     *   <li>The world has a valid numerical skill total requirement, determined by extracting and
     *       parsing the numeric value from the world's activity description.</li>
     *   <li>The player's total skill level is greater than or equal to the world's skill total requirement.</li>
     * </ul>
     * Worlds that do not meet these conditions, or those with invalid or missing activity information,
     * are excluded from the results.
     * </p>
     *
     * @return A {@code WorldQuery} object filtered to include only worlds with skill total requirements
     *         that are valid and met by the local player's total skill level.
     */
    public WorldQuery onlyValidSkillTotal() {
        int totalLevel = ctx.players().local().totalSkillLevel();
        return filter(w -> {
            boolean isSkillTotal = w.raw().getTypes().contains(WorldType.SKILL_TOTAL);
            if (isSkillTotal) {
                if (w.raw().getActivity() == null) return false;
                try {
                    int minLevel = Integer.parseInt(w.raw().getActivity().replaceAll("[^0-9.]", ""));
                    return minLevel <= totalLevel;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            // Note: this excludes normal worlds without a skill total level requirement.
            return false;
        });
    }

    /**
     * Filters the current query to include only worlds associated with the specified activity.
     * <p>
     * This method modifies the query to match worlds where the activity description (if not {@code null})
     * contains the provided activity string, after sanitizing and performing a case-insensitive comparison.
     * </p>
     *
     * @param activity The activity string to filter worlds by. This value is case-insensitive and
     *                 will be sanitized before matching.
     * @return A {@code WorldQuery} object filtered to include only worlds matching the specified activity.
     */
    public WorldQuery withActivity(String activity) {
        return filter(w -> w.raw().getActivity() != null && w.raw().getActivity().toLowerCase().contains(activity.toLowerCase()));
    }

    /**
     * Filters the current query to include only worlds that belong to the specified regions.
     * <p>
     * This method modifies the query to match worlds where their associated region matches
     * any of the provided {@code WorldRegion} values. If the world's region is {@code null},
     * it will be excluded from the filtered results.
     * </p>
     *
     * @param region An array of {@code WorldRegion} values to filter worlds by. Only worlds belonging
     *               to one or more of the specified regions will be included in the results.
     * @return A {@code WorldQuery} object filtered to include only worlds belonging
     *         to the specified regions.
     */
    public WorldQuery inRegion(WorldRegion... region) {
        return filter(w -> w.getHttpPackageWorld().getRegion() != null && ArrayUtils.contains(region, w.getHttpPackageWorld().getRegion()));
    }

    /**
     * Filters the current query to include only worlds with a player count within the specified range.
     * <p>
     * This method modifies the query to include only worlds where the player count falls between
     * the given minimum and maximum values (inclusive). Worlds with player counts outside this range
     * will be excluded from the results.
     * </p>
     *
     * @param min The minimum player count (inclusive) to include in the filtered results.
     * @param max The maximum player count (inclusive) to include in the filtered results.
     * @return A {@code WorldQuery} object filtered to include only worlds with player counts
     *         within the specified range.
     */
    public WorldQuery withPlayerCountBetween(int min, int max) {
        return filter(w -> w.raw().getPlayerCount() >= min && w.raw().getPlayerCount() <= max);
    }

    /**
     * Filters the current query to include only worlds with a player count less than the specified value.
     * <p>
     * This method modifies the query to exclude any worlds where the player count is greater than or equal
     * to the provided {@code count} value.
     * </p>
     *
     * @param count The maximum player count (exclusive) to include in the filtered results.
     *              Only worlds with a player count less than this value will be included.
     * @return A {@code WorldQuery} object filtered to include only worlds with a player count
     *         less than the specified value.
     */
    public WorldQuery withPlayerCountLessThan(int count) {
        return filter(w -> w.raw().getPlayerCount() < count);
    }

    /**
     * Sorts the current query's worlds by their player counts in descending order.
     * <p>
     * This method modifies the query to sort the worlds such that the worlds with higher
     * player counts appear earlier in the resulting sequence. The comparison is based on
     * the raw player count value retrieved for each world.
     * </p>
     *
     * @return A {@code WorldQuery} object sorted by player counts in descending order.
     */
    public WorldQuery sortedByPlayersDesc() {
        return sorted((w1, w2) -> Integer.compare(w2.raw().getPlayerCount(), w1.raw().getPlayerCount()));
    }

    /**
     * Sorts the current query's worlds by their world numbers in descending order.
     * <p>
     * This method modifies the query to sort the worlds such that the worlds with higher
     * world numbers appear earlier in the resulting sequence. The comparison is based on
     * the {@literal @getId} method of each world, which retrieves the world's numerical ID.
     * </p>
     *
     * @return A {@code WorldQuery} object sorted by world numbers in descending order.
     */
    public WorldQuery sortedByWorldNumberDesc() {
        return sorted((w1, w2) -> Integer.compare(w2.getId(), w1.getId()));
    }

    /**
     * Sorts the query results in ascending order based on the world number (ID).
     * <p>
     * This method arranges all queried World objects such that the objects with
     * lower world numbers (IDs) appear before those with higher world numbers.
     *
     * <p><b>Note:</b> The comparison uses the {@literal Integer.compare} method
     * to determine the ordering of the world numbers.
     *
     * @return A {@code WorldQuery} object representing the current state of the query,
     *         sorted by world number in ascending order.
     */
    public WorldQuery sortByWorldNumberAsc() {
        return sorted((w1, w2) -> Integer.compare(w1.getId(), w2.getId()));
    }

    /**
     * Retrieves a query for worlds that exclude the {@literal SKILL_TOTAL} type.
     * <p>
     * This method creates a query to filter out worlds that are categorized as
     * {@literal SKILL_TOTAL}, allowing only other world types to be included in the result.
     *
     * @return a {@code WorldQuery} instance that excludes worlds of type {@literal SKILL_TOTAL}.
     */
    public WorldQuery nonSkillTotalWorlds() {
        return withOutTypes(WorldType.SKILL_TOTAL);
    }

    /**
     * Filters the query to exclude worlds of the {@code PVP} type.
     * <p>
     * This method refines the results to only include worlds that are not categorized
     * as {@code PVP}, using the internal mechanism to specify exclusion criteria.
     *
     * @return A {@code WorldQuery} instance with the {@code PVP} world type excluded from the query results.
     */
    public WorldQuery nonPvp() {
        return withOutTypes(WorldType.PVP);
    }

    /**
     * Configures and returns a {@code WorldQuery} object that excludes specific world types
     * which are not part of the "standard" main game world category. This will filter out worlds
     * like fresh start, deadman, seasonal, leagues, gridmaster etc...
     *
     * <p>The following world types will be excluded:
     * <ul>
     *     <li>{@literal @}WorldType.PVP_ARENA</li>
     *     <li>{@literal @}WorldType.QUEST_SPEEDRUNNING</li>
     *     <li>{@literal @}WorldType.BETA_WORLD</li>
     *     <li>{@literal @}WorldType.LEGACY_ONLY</li>
     *     <li>{@literal @}WorldType.EOC_ONLY</li>
     *     <li>{@literal @}WorldType.NOSAVE_MODE</li>
     *     <li>{@literal @}WorldType.FRESH_START_WORLD</li>
     *     <li>{@literal @}WorldType.DEADMAN</li>
     *     <li>{@literal @}WorldType.SEASONAL</li>
     * </ul>
     * </p>
     *
     * @return A {@code WorldQuery} object that specifically excludes the listed world types.
     */
    public WorldQuery standard() {
        return withOutTypes(
                WorldType.PVP_ARENA,
                WorldType.QUEST_SPEEDRUNNING,
                WorldType.BETA_WORLD,
                WorldType.LEGACY_ONLY,
                WorldType.EOC_ONLY,
                WorldType.NOSAVE_MODE,
                WorldType.FRESH_START_WORLD,
                WorldType.DEADMAN,
                WorldType.SEASONAL
        );
    }

    /**
     * Retrieves the next {@literal WorldEntity} in the sorted list whose world ID is greater than the current world ID.
     * If no such entity exists, the method returns the first {@literal WorldEntity} in the list.
     * If the list is empty, it returns {@code null}.
     *
     * <p>This method sorts the available {@literal WorldEntity} objects in ascending order by their world ID and selects
     * the first one with an ID greater than the current world ID as determined by the client's state.</p>
     *
     * @return The next {@literal WorldEntity} with a world ID greater than the current world ID,
     *         the first {@literal WorldEntity} in the list if no ID is greater,
     *         or {@code null} if the list is empty.
     */
    public WorldEntity next() {
        int currentWorld = ctx.getClient().getWorld();
        List<WorldEntity> results = sortByWorldNumberAsc().list();
        for (WorldEntity world : results) {
            if (world.getId() > currentWorld) {
                return world;
            }
        }

        return !results.isEmpty() ? results.get(0) : null;
    }

    /**
     * Retrieves the previous {@literal World} in the sorted list of worlds based on the
     * current world number. The worlds are sorted in ascending order by their world number.
     * <p>
     * If a world with a smaller number than the current world is found, it is returned.
     * Otherwise, the last world in the sorted list is returned. If the list of worlds
     * is empty, {@code null} is returned.
     * </p>
     *
     * @return the previous {@literal World} in the sorted list, the last {@literal World} if no
     *         smaller world exists, or {@code null} if the world list is empty.
     */
    public WorldEntity previous() {
        int currentWorld = ctx.getClient().getWorld();
        List<WorldEntity> results = sortByWorldNumberAsc().list();
        for (WorldEntity world : results) {
            if (world.getId() < currentWorld) {
                return world;
            }
        }

        return !results.isEmpty() ? results.get(results.size() - 1) : null;
    }
}
