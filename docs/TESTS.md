# Kraken API Tests

This project is unique in that it functions exclusively within a RuneLite game client environment. This means that automated tests through a framework
like JUnit don't provide as much value. Sure, you can test functionality within the API but does is **really** find NPC's within 10 tiles of your player? 
The only way to know for sure is to run tests within the game client.

You can run tests by running the main class in `ExamplePluginTest.java`. This will launch a new game client which loads a custom "Testing" plugin
called "API Tests" which you will see within RuneLite. Through this plugin you can run specific tests which cover various query and service related classes
and dump output into the console and the overlay for PASS/FAIL.

Most of the tests are fully self-sufficient, that is, they set themselves up with the necessary in game items before running the tests. However,
there are a few conditions listed below.

## General Requirements

- **Location**: Most of the tests are designed to be run from **Varrock West Bank**.
- **NPCs**: Nearby "Guard" NPCs must be present (Varrock West Bank has them).
- **Players**: Some tests require other players to be nearby (e.g., `PlayerTest`).
- **Bank**: The player must be near a bank booth (Varrock West Bank) and have either no PIN set or have pre-entered their bank pin.

## Inventory & Bank Requirements

The following items must be present in your **Bank**:

- **Armor/Weapons**:
    - Rune Full Helm
    - Rune Platebody
    - Rune Platelegs
    - Rune Scimitar
- **Food**:
    - Lobster (at least 5)
    - Swordfish (at least 5)
    - Raw Salmon (at least 2)
    - Raw Trout (at least 2)
- **Runes**:
    - Law runes
    - Fire runes
    - Air runes

## Skill & Spellbook Requirements

- **Prayer**: Protect from Melee prayer must be unlocked.
- **Spellbook**: Must be on the **Standard Spellbook**.
- **Magic Level**: High enough to cast Varrock Teleport (Level 25).

## Specific Test Requirements

### `ProcessingServiceTest`
- **Location**: Must be near the **Barbarian Village fire** (permanent fire).
- **Inventory**: Requires at least 2 Raw Salmon and 2 Raw Trout in inventory.

### `TaskChainTest`
- **Location**: Expects to run near **Lumbridge**.
- **Requirements**:
    - Must be able to chop a tree (Axe in inventory or equipped).
    - Must be near a "Canoe Station" (Lumbridge has one).

### `DialogueServiceTest`
- **Location**: Near a Banker (Varrock West Bank works).
- **State**: Ensure no dialogue is currently open before starting.

### `MouseTest`
- **Configuration**: If using `REPLAY` strategy, a recording named "test" must exist. If using `LINEAR`, no specific setup is needed other than valid targets nearby.

### `PathfinderServiceTest`
- **Location**: Starts near Varrock East Bank (specifically `WorldPoint(3253, 3421, 0)`).
- **Note**: This test moves the player.

### `MovementServiceTest`
- **Interaction**: Requires user interaction (Shift + Right Click 'Walk here' -> 'Set' on a tile) to begin movement.

### `GroundObjectTest`
- **Inventory**: Requires at least one item that can be dropped (e.g., a fish or rune).

### `EquipmentTest`
- **Bank**: Requires Rune armor set (Helm, Body, Legs, Scimitar) to be in the bank.
- **State**: Player should ideally NOT be wearing the Rune armor at the start (the test will withdraw and equip it).

### `SpellServiceTest`
- **Bank**: Requires Fire, Air, and Law runes in the bank.
- **Level**: Level 25 magic to cast Varrock Teleport
- **State**: Bank must be openable (No PIN or pre-entered PIN).

### `CameraServiceTest`
- **Interaction**: Requires a target tile to be selected via the plugin overlay/interaction before the test proceeds.
