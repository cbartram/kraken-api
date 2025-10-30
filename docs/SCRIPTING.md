# Scripting

This doc covers some basic principles and examples to start writing
scripts with the Kraken API.

### Script Structure

There are 2 main structures you can use for actually writing scripts with the Kraken API,
although, you can implement other ways of maintaining script state if you'd like!

1. Extending the basic `Script` class
2. Behavior Trees

#### Extending the Script Class

For simple plugins, most users will want to extend the `Script` class which provides helpful methods like `onStart()`, `onLoop()`, and `onEnd()` for 
managing script state. You can opt to implement a Finite State Machine (FSM) pattern for your scripts where, when certain conditions are met
the script transitions to a state and performs an action. For example a mining script may have:

States:
- IDLE: Initial state, ready to begin mining
- FINDING_ROCKS: Searching for available mining rocks
- MOVING_TO_ROCKS: Walking to the selected mining location
- MINING: Actively mining ore from rocks
- INVENTORY_FULL: Inventory is full, need to bank
- MOVING_TO_BANK: Walking to the bank
- BANKING: Depositing ore into bank
- ERROR: Something went wrong, needs intervention

and governing logic like: 

IDLE → FINDING_ROCKS → MOVING_TO_ROCKS → MINING → MINING → MINING →
INVENTORY_FULL → MOVING_TO_BANK → BANKING → FINDING_ROCKS → ...

This approach has several benefits:

- Clear Logic Flow: Easy to understand and debug bot behavior
- Error Handling: Structured approach to handle failures
- Maintainability: Simple to add new states or modify existing ones
- Predictable Behavior: Bot actions are deterministic based on current state
- Logging: Easy to track state transitions for debugging

Extending the `Script` class gives you a blank slate to work with,
giving your freedom to determine how your script operates with the Kraken API. 

#### Behavior Trees

As you move to making more complex scripts you may run into issues with large FSM's that make managing states difficult to debug. This is where Behavior Trees come
into play. Traditionally, behavior trees have been used to give depth to A.I. enemies in video games, however, the Kraken API includes a foundation for creating
scripts using Behavior Trees. This document won't cover the mechanics behind behavior trees in detail however, you can check out the [Kraken Example Mining Plugin](https://github.com/cbartram/kraken-example-plugin)
to see a fully implemented example of a Behavior tree based script. 

Behavior trees are one of those things where you don't need them until you do. You may eventually get to a point in your script where the state transitions
become too complex and unwieldy to maintain which is why the Kraken API provides this programming paradigm to you!
