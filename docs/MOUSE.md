# Mouse API Documentation

The Mouse API in Kraken provides a comprehensive system for simulating mouse movements and interactions within the game client.
It supports various movement strategies, including instant, linear, Bezier curves, and replay-based movements recorded from real user input.

## Table of Contents
- [VirtualMouse](#virtualmouse)
- [Mouse Movement Strategies](#mouse-movement-strategies)
  - [Bezier Strategy](#bezier-strategy)
  - [Linear Strategy](#linear-strategy)
  - [Instant Strategy](#instant-strategy)
  - [Replay Strategy](#replay-strategy)
- [Recording Mouse Gestures](#recording-mouse-gestures)
- [Loading Gesture Libraries](#loading-gesture-libraries)
- [Examples](#examples)

## VirtualMouse

The `VirtualMouse` class is the main entry point for interacting with the mouse. 
It provides methods to move the mouse to various targets such as `Point`, `Actor`, `Tile`, `Widget`, and more.

### Basic Usage

You can inject `VirtualMouse` into your script or service and use it to move the mouse.

```java
@Inject
private VirtualMouse virtualMouse;

// Move to a specific point
virtualMouse.move(new Point(100, 200));

// Move to an NPC
NPC goblin = ...;
virtualMouse.move(goblin);

// Move to a widget
Widget inventoryItem = ...;
virtualMouse.move(inventoryItem);
```

### Setting the Default Strategy

You can set the default mouse movement strategy globally. The default strategy is `BEZIER`.

```java
// Set default strategy to Linear
VirtualMouse.setMouseMovementStrategy(MouseMovementStrategy.LINEAR);

// When using linear its important to set the amount of steps (points generated along the line).
// If your line is short generate a short number of points, same for longer lines.
LinearStrategy linear = (LinearStrategy) MouseMovementStrategy.LINEAR.getStrategy();
linear.setSteps(100);
```

### Chaining Movements

The `move` methods return the `VirtualMouse` instance, allowing for method chaining (though typically you move to one target at a time).

```java
virtualMouse.move(target1).move(target2);
```

## Mouse Movement Strategies

The API supports several strategies defined in `MouseMovementStrategy`. You can pass a specific strategy to the `move` method to override the default for a single action.

### Bezier Strategy
**Enum:** `MouseMovementStrategy.BEZIER`

Simulates human-like mouse movement using cubic Bezier curves. It generates random control points to create arcs and varies speed based on Fitts's Law. This is the default strategy.

```java
virtualMouse.move(target, MouseMovementStrategy.BEZIER);
```

### Linear Strategy
**Enum:** `MouseMovementStrategy.LINEAR`

Moves the mouse in a straight line with a specified number of steps. Useful for debugging or specific mechanical movements.

**Note:** You must set the number of steps before using this strategy if you are accessing the underlying implementation directly,
but via `VirtualMouse`, it uses default settings or requires configuration if exposed. 


```java
// Set the number of steps based on the movement that needs to be made
LinearStrategy linear = (LinearStrategy) MouseMovementStrategy.LINEAR.getStrategy();
linear.setSteps(100);

// Execute the movement
virtualMouse.move(target, MouseMovementStrategy.LINEAR);
```

### Instant Strategy
**Enum:** `MouseMovementStrategy.INSTANT`

Instantly teleports the mouse cursor to the target position. Useful for high-performance scenarios where human simulation is not required.

It is not recommended to use this strategy in any form of automation script.

```java
virtualMouse.move(target, MouseMovementStrategy.INSTANT);
```

### Replay Strategy
**Enum:** `MouseMovementStrategy.REPLAY`

Replays recorded mouse gestures from a library. This strategy finds a real human recorded path that is similar in distance, velocity and angle
to the required movement distance and transforms it to fit the start and end points.

**Requirement:** You must load a gesture library before using this strategy. Make sure you are using a gesture library trained
on the activity you are planning on doing. Mouse movements at the Hallowed Sepulchre differ greatly from mouse movement at
Shooting Stars!

```java
// Load a library first
VirtualMouse.loadLibrary("mining_varrock_east");

// Use the strategy
virtualMouse.move(target, MouseMovementStrategy.REPLAY);
```

## Recording Mouse Gestures

The `MouseRecorder` class allows you to record your own mouse movements to create gesture libraries for the Replay strategy.

### How to Record

1.  **Start Recording:** Call `start(String label)` with a unique label for your session. It is recommended to label your session according to activities like: woodcutting_yews, powermining_iron, shooting_stars, etc...
2.  **Perform Actions:** Move your mouse and click as you normally would.
3.  **Stop Recording:** Call `stop()` to save the data to disk.

The data is saved as JSON files in `user.home/.runelite/kraken/mouse_data/`. It is recommended to play between 30 minutes
and 2 hours manually for best results. This ensures you have a wide variety of mouse movements and data points that can be selected
when choosing gestures for the Replay strategy.

```java
@Inject
private MouseRecorder mouseRecorder;

public void startRecording() {
    mouseRecorder.start("mining_iron_varrock_east");
}

public void stopRecording() {
    mouseRecorder.stop();
}
```

### Data Storage
Recorded gestures are stored in:
`~/.runelite/kraken/mouse_data/<label>.json`

Each gesture includes the path points, duration, and the trigger event (e.g., click).

## Loading Gesture Libraries

To use the `REPLAY` strategy, you need to load a library of recorded gestures.

### Loading a Library
Use `VirtualMouse.loadLibrary(String libraryName)` where `libraryName` corresponds to the filename (without `.json`) in the mouse data directory.

```java
VirtualMouse.loadLibrary("mining_iron_varrock_east");
```

### Finding Available Libraries
You can list all available libraries using:

```java
List<String> libraries = VirtualMouse.findLibraries();
for (String lib : libraries) {
    System.out.println("Found library: " + lib);
}
```

## Examples

### Example 1: Using Different Strategies

```java
@Inject
private VirtualMouse virtualMouse;

public void execute() {
    Point target = new Point(500, 500);

    // Default (Bezier)
    virtualMouse.move(target);

    // Instant
    virtualMouse.move(target, MouseMovementStrategy.INSTANT);

    // Replay (requires loaded library)
    VirtualMouse.loadLibrary("hallowed_sepulchre");
    virtualMouse.move(target, MouseMovementStrategy.REPLAY);
}
```

### Example 2: Moving to Game Objects

```java
@Inject
private VirtualMouse virtualMouse;

@Inject
private Client client;

@Inject
private Context ctx;

public void interactWithObject() {
    // Assume we have a method to find an object
    GameObject bankBooth = ctx.gameObjects().withName("Bank booth").nearest();
    
    if (bankBooth != null) {
        // Move to the bank booth using the default strategy (note: this will not click the bank booth) you must
        // separately call the interact() method on the game entity to click it.
        virtualMouse.move(bankBooth);
        bankBooth.interact("Open");
    }
}
```

### Example 3: Recording a Session

```java
@Inject
private MouseRecorder recorder;

public void recordSession() {
    recorder.start("pking_switches");
    
    // ... User performs actions manually ...
    
    // Later, when finished
    recorder.stop();
}
```
