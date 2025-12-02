package com.kraken.api.core;

public interface Interactable<T> {
    /**
     * Interacts with the entity using the given action verb.
     * @param action The menu action to trigger (e.g., "Attack", "Talk-to", "Take")
     * @return true if the interaction packet was successfully queued/sent
     */
    boolean interact(String action);

    /**
     * Returns the wrapped (raw) RuneLite API object for this interactable game entity. This
     * is useful to provide easy access to familiar and underlying RuneLite game data. For example:
     * an {@code EquipmentEntity} will expose the RuneLite {@code Widget} object for the interactable piece of equipment.
     * @return T wrapped RuneLite API object.
     */
    T raw();

    /**
     * The item ID for the wrapped game entity
     * @return int Item id
     */
    int getId();

    /**
     * The game entities name.
     * @return The name of the game entity i.e. NPC name for NPC's, item name for ContainerItem's, and GameObject name
     * for various game objects.
     */
    String getName();

    /**
     * True when the game entity is null and false otherwise. This may happen when a game entity you expected to be present
     * is no longer available i.e. The nearest Oak tree was just chopped down by another player and is now null.
     * @return True when the game entity is null and false otherwise.
     */
    boolean isNull();
}