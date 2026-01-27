package com.kraken.api.sim.engine;

import com.kraken.api.service.map.WorldPointService;
import com.kraken.api.sim.LocalCollisionMap;
import com.kraken.api.sim.model.SimActor;
import com.kraken.api.sim.model.SimNpc;
import com.kraken.api.sim.model.SimPlayer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class SimulationUtils {

    // --- Collision Flags (Mapped to standard RuneLite CollisionDataFlag) ---
    private static final int BLOCK_NW = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST; // 0x1
    private static final int BLOCK_N  = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;      // 0x2
    private static final int BLOCK_NE = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST; // 0x4
    private static final int BLOCK_E  = CollisionDataFlag.BLOCK_MOVEMENT_EAST;       // 0x8
    private static final int BLOCK_SE = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST; // 0x10
    private static final int BLOCK_S  = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;      // 0x20
    private static final int BLOCK_SW = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST; // 0x40
    private static final int BLOCK_W  = CollisionDataFlag.BLOCK_MOVEMENT_WEST;       // 0x80

    // Combine impassable obstacles (Objects, Floors, Decorations)
    private static final int BLOCK_IMPASSABLE = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT
            | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION
            | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR
            | CollisionDataFlag.BLOCK_MOVEMENT_FULL;

    /**
     * Calculates a straightforward path from a starting {@literal @WorldPoint} to a destination {@literal @WorldPoint}.
     * <p>
     * This method uses simple pathfinding logic, considering the dimensions of the actor as well as any tiles to avoid
     * (blacklist). It will stop path calculation if progress becomes blocked due to obstacles or unreachable tiles.
     * </p>
     *
     * @param start The starting {@literal @WorldPoint} representing the current position in the world.
     * @param destination The target {@literal @WorldPoint} indicating the destination in the world.
     * @param actorWidth The width of the actor (in tiles) used to consider collision during pathfinding.
     * @param actorHeight The height of the actor (in tiles) used to consider collision during pathfinding.
     * @param blacklist A list of {@literal @WorldPoint} objects that should be avoided during the pathfinding process.
     *                  This typically represents obstacles, impassable tiles, or other actors' locations.
     *
     * @return A list of {@literal @WorldPoint} objects forming the sequential tiles in the computed path from the start
     *         to the destination. If no path can be found, an empty list is returned.
     */
    public static List<WorldPoint> findSimpleTargetPath(final WorldPoint start, final WorldPoint destination, final int actorWidth, final int actorHeight, final List<WorldPoint> blacklist) {
        LocalCollisionMap.load();

        IntArrayList blist = new IntArrayList();
        if(blacklist != null && !blacklist.isEmpty()) {
            blacklist.forEach(p -> blist.add(WorldPointService.pack(p)));
        }

        int plane = start.getPlane();
        if (plane != destination.getPlane())
            return new ArrayList<>();

        int curX = start.getX();
        int curY = start.getY();
        int destX = destination.getX();
        int destY = destination.getY();

        List<WorldPoint> path = new ArrayList<>();

        while (curX != destX || curY != destY) {
            int difX = destX - curX;
            int difY = destY - curY;
            int dx = Integer.signum(difX);
            int dy = Integer.signum(difY);
            if (Math.abs(difX) == 1 && Math.abs(difY) == 1) {
                if (canMove(curX, curY, plane, dx, 0, actorWidth, actorHeight, blist)) {
                    curX += dx;
                    path.add(new WorldPoint(curX, curY, plane));
                    path.add(new WorldPoint(destX, destY, plane));
                }
                break;
            }

            // Normal pathing logic
            if (canMove(curX, curY, plane, dx, dy, actorWidth, actorHeight, blist)) {
                curX += dx;
                curY += dy;
            } else if (dx != 0 && canMove(curX, curY, plane, dx, 0, actorWidth, actorHeight, blist)) {
                curX += dx;
            } else if (dy != 0 && canMove(curX, curY, plane, 0, dy, actorWidth, actorHeight, blist)) {
                curY += dy;
            } else {
                break;
            }
            path.add(new WorldPoint(curX, curY, plane));
        }
        return path;
    }

    /**
     * Calculates a simple path for a given actor to a specified destination tile.
     * <p>
     * This method accounts for other actors' occupied tiles by adding them to the blacklist.
     * It retrieves all tiles currently occupied by the actor and appends them to the given blacklist.
     * The actor's world area (size) is also considered during path calculation.
     * </p>
     *
     * @param actor The {@literal @SimActor} for whom the path is being calculated. This represents the starting point
     *              and size of the actor in the world.
     * @param destination The {@literal @WorldPoint} that represents the target destination for the actor's path.
     * @param blacklist A list of {@literal @WorldPoint} objects to avoid during pathfinding, such as tiles occupied by
     *                  other actors or tiles marked as impassable.
     * @return A list of {@literal @WorldPoint} objects representing the sequential tiles in the computed path from the
     *         actor's current location to the destination. If no path is found, an empty list is returned.
     */
    public static List<WorldPoint> findSimpleActorPath(final SimActor actor, final WorldPoint destination, final List<WorldPoint> blacklist) {
        blacklist.addAll(getOccupiedActorTiles(actor));
        final WorldArea area = actor.getWorldArea();
        return findSimpleActorPath(actor.getLocation(), destination, area.getWidth(), area.getHeight(), blacklist);
    }

    /**
     * Calculates the simple path of an actor to the destination tile. This tile takes into consideration other
     *  actors' locations (i.e. some NPC's can block other NPC's from reaching a target). This method will load
     *  the local collision map of the player if it is not already loaded and check collision for actors based
     *  on what is around the local player.
     *
     * @param start The starting tile for the actor (their current location)
     * @param destination The destination tile for the actor
     * @param blacklist list of hardcoded WorldPoints to avoid (this could be other actors locations or unreachable tiles).
     * @return path
     */
    public static List<WorldPoint> findSimpleActorPath(final WorldPoint start, final WorldPoint destination, final int actorWidth, final int actorHeight, final List<WorldPoint> blacklist) {
        LocalCollisionMap.load();

        IntArrayList blist = new IntArrayList();
        if(blacklist != null && !blacklist.isEmpty()) {
            blacklist.forEach(p -> blist.add(WorldPointService.pack(p)));
        }

        List<WorldPoint> path = new ArrayList<>();
        int plane = start.getPlane();
        if (plane != destination.getPlane()) {
            return path;
        }

        int curX = start.getX();
        int curY = start.getY();
        int destX = destination.getX();
        int destY = destination.getY();

        // Safety limit to prevent infinite loops if path is unreachable
        int maxSteps = 100;
        int steps = 0;

        while ((curX != destX || curY != destY) && steps++ < maxSteps) {
            int difX = destX - curX;
            int difY = destY - curY;
            int dx = Integer.signum(difX);
            int dy = Integer.signum(difY);

            if(((difX == 1 && difY == 1) || (difX == 1 && difY == -1)) && cantMove(curX, curY, plane, 1, 0)) {
                break;
            } else if(((difX == -1 && difY == -1) || (difX == -1 && difY == 1)) && cantMove(curX, curY, plane, -1, 0)) {
                break;
            }

            // Try moving diagonally towards target
            if (dx != 0 && dy != 0 && canMove(curX, curY, plane, dx, dy, actorWidth, actorHeight, blist)) {
                curX += dx;
                curY += dy;
            }
            // If diagonal blocked, try moving along X axis
            else if (dx != 0 && canMove(curX, curY, plane, dx, 0, actorWidth, actorHeight, blist)) {
                curX += dx;
            }

            // If X blocked, try moving along Y axis
            else if (dy != 0 && canMove(curX, curY, plane, 0, dy, actorWidth, actorHeight, blist)) {
                curY += dy;
            } else {
                break;
            }

            path.add(new WorldPoint(curX, curY, plane));
        }
        return path;
    }

    /**
     * Checks if an actor of a specific size can take a step in a given direction.
     * Iterates over every tile the actor occupies.
     */
    private static boolean canMove(final int currX, final int currY, final int curZ, final int dx, final int dy, final int actorWidth, final int actorHeight, final IntArrayList blacklist) {
        for (int x = 0; x < actorWidth; x++) {
            for (int y = 0; y < actorHeight; y++) {
                int checkX = currX + x;
                int checkY = currY + y;

                if(blacklist.contains(WorldPointService.pack(checkX, checkY, curZ)))
                    return false;

                // Note: cantStep returns TRUE if the path is BLOCKED
                if (cantMove(checkX, checkY, curZ, dx, dy)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines if a single tile movement is blocked based on collision flags.
     * Uses the (dx+1)*3 + (dy+1) switch optimization.
     */
    private static boolean cantMove(int x, int y, int z, int dx, int dy) {
        // Map dx/dy (-1, 0, 1) to an index 0-8
        // 0:SW, 1:W, 2:NW, 3:S, 4:Center, 5:N, 6:SE, 7:E, 8:NE
        int index = (dx + 1) * 3 + (dy + 1);

        switch (index) {
            case 0: return checkSouthWest(x, y, z);
            case 1: return checkWest(x, y, z);
            case 2: return checkNorthWest(x, y, z);
            case 3: return checkSouth(x, y, z);
            case 5: return checkNorth(x, y, z);
            case 6: return checkSouthEast(x, y, z);
            case 7: return checkEast(x, y, z);
            case 8: return checkNorthEast(x, y, z);
        }
        return true;
    }

    private static boolean checkNorth(int x, int y, int z) {
        // Blocked if: Current tile blocks North OR Target tile blocks South
        return (getFlags(x, y, z) & (BLOCK_N | BLOCK_IMPASSABLE)) != 0
                || (getFlags(x, y + 1, z) & (BLOCK_S | BLOCK_IMPASSABLE)) != 0;
    }

    private static boolean checkEast(int x, int y, int z) {
        // Blocked if: Current tile blocks East OR Target tile blocks West
        return (getFlags(x, y, z) & (BLOCK_E | BLOCK_IMPASSABLE)) != 0
                || (getFlags(x + 1, y, z) & (BLOCK_W | BLOCK_IMPASSABLE)) != 0;
    }

    private static boolean checkSouth(int x, int y, int z) {
        // Blocked if: Current tile blocks South OR Target tile blocks North
        return (getFlags(x, y, z) & (BLOCK_S | BLOCK_IMPASSABLE)) != 0
                || (getFlags(x, y - 1, z) & (BLOCK_N | BLOCK_IMPASSABLE)) != 0;
    }

    private static boolean checkWest(int x, int y, int z) {
        // Blocked if: Current tile blocks West OR Target tile blocks East
        return (getFlags(x, y, z) & (BLOCK_W | BLOCK_IMPASSABLE)) != 0
                || (getFlags(x - 1, y, z) & (BLOCK_E | BLOCK_IMPASSABLE)) != 0;
    }

    private static boolean checkNorthEast(int x, int y, int z) {
        // Diagonal check: Must be able to move North AND East from current,
        // and from the resulting ordinal tiles.
        if (checkNorth(x, y, z) || checkEast(x, y, z)) return true;
        if (checkEast(x, y + 1, z)) return true;
        if (checkNorth(x + 1, y, z)) return true;
        return (getFlags(x + 1, y + 1, z) & BLOCK_IMPASSABLE) != 0;
    }

    private static boolean checkSouthEast(int x, int y, int z) {
        if (checkSouth(x, y, z) || checkEast(x, y, z)) return true;
        if (checkEast(x, y - 1, z)) return true;
        if (checkSouth(x + 1, y, z)) return true;
        return (getFlags(x + 1, y - 1, z) & BLOCK_IMPASSABLE) != 0;
    }

    private static boolean checkSouthWest(int x, int y, int z) {
        if (checkSouth(x, y, z) || checkWest(x, y, z)) return true;
        if (checkWest(x, y - 1, z)) return true;
        if (checkSouth(x - 1, y, z)) return true;
        return (getFlags(x - 1, y - 1, z) & BLOCK_IMPASSABLE) != 0;
    }

    private static boolean checkNorthWest(int x, int y, int z) {
        if (checkNorth(x, y, z) || checkWest(x, y, z)) return true;
        if (checkWest(x, y + 1, z)) return true;
        if (checkNorth(x - 1, y, z)) return true;
        return (getFlags(x - 1, y + 1, z) & BLOCK_IMPASSABLE) != 0;
    }

    /**
     * Helper to retrieve flags efficiently from the map.
     * We define a method here to bridge the raw coordinates to the packed key.
     */
    private static int getFlags(int x, int y, int z) {
        return LocalCollisionMap.getCollisionFlags(WorldPointService.pack(x, y, z));
    }

    /**
     * Retrieves all the tiles occupied by a given actor.
     * <p>
     * Actors can be either NPCs or players, and the occupied tiles are determined based on their type:
     * <ul>
     *   <li>If the actor is an instance of {@literal @SimNpc}, all tiles within the NPC's {@literal @WorldArea} are returned.</li>
     *   <li>If the actor is an instance of {@literal @SimPlayer}, only the player’s specific location is returned as a single tile.</li>
     * </ul>
     *
     * @param actor The {@literal @SimActor} whose occupied tiles are to be retrieved. It can represent either an NPC or a player.
     * @return A list of {@literal @WorldPoint} objects representing the tiles occupied by the actor.
     */
    public static List<WorldPoint> getOccupiedActorTiles(final SimActor actor) {
        List<WorldPoint> tiles = new ArrayList<>();

        if(actor instanceof SimNpc) {
            SimNpc npc = (SimNpc) actor;
            tiles.addAll(npc.getNpc().getWorldArea().toWorldPointList());
        }

        if(actor instanceof SimPlayer) {
            SimPlayer player = (SimPlayer) actor;
            tiles.add(player.getLocation());
        }

       return tiles;
    }
}