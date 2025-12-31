# Mouse API Documentation

The Mouse API in Kraken provides a comprehensive system for simulating mouse movements and interactions within the game client.
It supports various movement strategies, including instant, linear, Bezier curves, and replay-based movements recorded from real user input.

## Table of Contents
- [VirtualMouse](#virtualmouse)
- [Mouse Movement Strategies](#mouse-movement-strategies)
  - [Bezier Strategy](#bezier-strategy)
  - [Wind Strategy](#wind-strategy)
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


### Wind Strategy 
**Enum:** `MouseMovementStrategy.WIND`

If you want a strategy that creates truly unique, non-repeating paths, which has overshoot logic and asymmetric easing function, then the ["WindMouse"](https://ben.land/post/2021/04/25/windmouse-human-mouse-movement/) algorithm will
be your best choice. [This algorithm](https://ben.land/post/2021/04/25/windmouse-human-mouse-movement/) was popularized by `BenLand100` in the early 2010s and simulates the mouse cursor as a physical object with mass.

- Gravity: A force pulling the mouse toward the target.
- Wind: A chaotic random force pushing the mouse in varying directions.
- Fluidity: Because it relies on velocity and momentum, the path loops and arcs naturally without ever looking like a pre-calculated curve.

Here is the documentation formatted in clean, readable Markdown, suitable for a README or API wiki.

####  WindMouseConfig Configuration

The `WindMouseConfig` class controls the physics simulation of the mouse cursor. 
By tweaking these values, you can alter the "personality" of the mouse movement, 
ranging from a steady, fast hand to a tired, wandering cursor.

#### `gravity`

**Type:** `double` | **Default:** `9.0`

This determines the strength of the force pulling the mouse cursor toward the target destination. It acts as the primary driver of velocity.

* **High Value (>15.0):** The mouse moves very directly and quickly to the target. It feels "snappy" and accurate but may look robotic if set too high.
* **Low Value (<5.0):** The mouse feels "floaty" or "slippery." It is much more prone to overshooting the target and looping back, simulating a lack of focus or low friction.

#### `wind`

**Type:** `double` | **Default:** `3.0`

This controls the magnitude of chaotic, random forces applied to the cursor during movement. It introduces noise to the path so lines are never perfectly straight.

* **High Value (>5.0):** The cursor will shake and jitter significantly. This simulates a nervous hand, a dirty mousepad, or high sensitivity.
* **Low Value (<2.0):** The path becomes smoother and cleaner, simulating a steady hand or lower mouse sensitivity.

#### `minWait` & `maxWait`

**Type:** `double` | **Default:** `2.0` / `5.0`

These control the temporal speed of the mouse by defining the sleep duration (in milliseconds) between each movement step (loop).

* **Effect:** The algorithm picks a random sleep time between `minWait` and `maxWait` for every step.
* **Lower Values:** Results in a higher refresh rate and faster overall movement.
* **Higher Values:** Slows the cursor down physically. If values are too high (>15ms), the movement may look "choppy" or laggy.

#### `maxStep`

**Type:** `double` | **Default:** `10.0`

This acts as a terminal velocity cap. It restricts the maximum number of pixels the cursor can travel in a single processing loop.

* **Purpose:** Prevents the physics engine from accumulating too much velocity and "teleporting" the mouse across the screen instantly, which is a flag for bot detection.
* **Adjustment:** Increase this for cross-screen movements on high-resolution displays; decrease it for precise, short-range interactions.

#### `targetArea`

**Type:** `double` | **Default:** `8.0`

Defines the radius (in pixels) around the exact target point where the "approach phase" begins.

* **Behavior:** When the distance to the target is less than `targetArea`:
1. The `wind` (chaos) is dampened significantly to allow for fine motor control.
2. If the cursor gets very close (e.g., <3px) within this area, the loop terminates and snaps to the final pixel.


* **Tuning:** A larger area ensures the mouse slows down sooner. A smaller area keeps the mouse moving fast until the very last moment.

---

#### Quick Tuning Reference

Use these presets as a starting point for creating different mouse "personalities."

| Personality | Gravity | Wind | MaxStep | Description |
| --- | --- | --- | --- | --- |
| **The Gamer** (Default) | `9.0` | `3.0` | `10.0` | Balanced speed and accuracy. Occasional overshoot. |
| **The Robot** | `20.0` | `1.0` | `25.0` | Extremely fast, straight lines, almost no shake. |
| **The Tired Human** | `4.0` | `5.0` | `8.0` | Loopy, shaky, misses target often and corrects slowly. |
| **The Sniper** | `7.0` | `1.5` | `6.0` | Slow, deliberate, and very steady movement. |

```java

import net.runelite.api.Point;

Point target = new Point(100, 100);
VirtualMouse mouse = RuneLite.getInjector().getInstance(VirtualMouse.class);

// Move using the default configuration
mouse.move(target, MouseMovementStrategy.WIND);

// Use defaults for maxWait, maxStep and targetArea but customize
// other algorithm properties
WindMouseConfig config = WindMouseConfig.builder()
        .gravity(8.4)
        .wind(2.1)
        .minWait(2.0);

// Use custom configuration to move the mouse
mouse.move(target, config);

// Fast, accurate mouse for banking (High Gravity, Low Wind)
WindMouseConfig bankingConfig = WindMouseConfig.builder()
        .gravity(15.0)
        .wind(2.0)
        .targetArea(10.0)
        .build();

// Lazy, shaky mouse for woodcutting (Low Gravity, High Wind)
WindMouseConfig tiredConfig = WindMouseConfig.builder()
        .gravity(6.0)
        .wind(5.0)
        .targetArea(5.0)
        .build();


mouse.move(target, tiredConfig);
```

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
