# OSRS Packet Mapping Tool

A RuneLite plugin tool that automatically maps Old School RuneScape packet structures by intercepting and analyzing outgoing packets.

## Overview

This tool helps you debug and map specific packet data for OSRS by:
1. Hooking into the packet-sending mechanism
2. Capturing packet data as it's sent
3. Correlating packets with in-game actions
4. Analyzing buffer contents to determine write operations and transformations
5. Generating Java constant definitions for packet mappings

## Features

- **Automatic Action Correlation**: Matches packets with game actions (clicking NPCs, objects, items, etc.)
- **Buffer Analysis**: Detects write operations and transformations (byte add, reverse, etc.)
- **Multiple Monitoring Modes**: Queue polling, direct interception
- **Export Capabilities**: Export mappings to Java files or copy to clipboard
- **UI Panel**: Easy-to-use interface integrated into RuneLite

## Installation

1. Add the packet mapping classes to your RuneLite plugin project
2. Update `ObfuscatedNames.java` with current obfuscated class/field names
3. Build and run the plugin
4. The "Packet Mapper" icon will appear in the RuneLite sidebar

## Usage

### Basic Workflow

1. **Start Monitoring**
   - Click the Packet Mapper icon in the sidebar
   - Click "Start Monitoring" button
   - The status will change to "Monitoring active"

2. **Perform In-Game Actions**
   - Click on NPCs, game objects, ground items
   - Use interface buttons, drag items
   - Walk around the game world
   - Each action will be captured and analyzed

3. **Stop Monitoring**
   - Click "Stop Monitoring" when done
   - The packet selector dropdown will populate with found packets

4. **View and Export Mappings**
   - Select a packet from the dropdown
   - View the generated mapping in the text area
   - Export individual mappings or all mappings at once

### Understanding the Output

The tool generates mappings in this format:

```java
// OPOBJ1
public static final String OPOBJ1_OBFUSCATEDNAME = "bf";
public static final String OPOBJ1_WRITE1 = "objectId";
public static final String OPOBJ1_METHOD_NAME1 = "be";
public static final String OPOBJ1_WRITE2 = "worldPointX";
public static final String OPOBJ1_METHOD_NAME2 = "ev";
public static final String OPOBJ1_WRITE3 = "worldPointY";
public static final String OPOBJ1_METHOD_NAME3 = "be";
public static final String OPOBJ1_WRITE4 = "ctrlDown";
public static final String OPOBJ1_METHOD_NAME4 = "dy";
public static final String[][] OPOBJ1_WRITES = new String[][]{
        {"r 8", "v"},    // objectId transformations
        {"v", "r 8"},    // worldPointX transformations
        {"r 8", "v"},    // worldPointY transformations
        {"s 128"},       // ctrlDown transformation
};
```

**Key Components:**
- `OBFUSCATEDNAME`: The obfuscated packet field name in the ClientPacket class
- `WRITE1`, `WRITE2`, etc.: Parameter names in order
- `METHOD_NAME1`, `METHOD_NAME2`, etc.: Obfuscated write method names
- `WRITES`: Array of transformations applied to each parameter

### Transformation Types

The tool detects these common transformations:

| Transformation | Description | Example |
|---------------|-------------|---------|
| `v` | Add 128 to high byte | Short value with high byte offset |
| `r 8` | Reverse bytes with negation | Little-endian reversed short |
| `s 128` | Subtract 128 | Byte value offset |
| `LE` | Little-endian | Reversed byte order |
| `IME` | Inverse middle-endian | Int with swapped middle bytes |
| `none` | No transformation | Standard big-endian write |

## Architecture

### Components

1. **PacketMappingTool** - Main coordinator
   - Listens to menu events
   - Manages packet mappings
   - Exports results

2. **PacketQueueMonitor** - Queue-based monitoring
   - Polls the packet queue periodically
   - Captures new packets
   - Creates packet snapshots

3. **PacketInterceptor** - Direct interception (advanced)
   - Uses dynamic proxy to intercept method calls
   - Requires careful setup

4. **EnhancedBufferAnalyzer** - Buffer analysis
   - Detects write operations
   - Identifies transformations
   - Infers write methods

5. **PacketMappingPanel** - UI component
   - Control buttons
   - Packet selector
   - Export functionality

### Data Flow

```
Game Action → MenuOptionClicked Event → PacketMappingTool
                                              ↓
                          Records action type and timestamp
                                              ↓
Packet Sent → PacketQueueMonitor detects → Creates snapshot
                                              ↓
                              PacketMappingTool.analyzePacket()
                                              ↓
                         EnhancedBufferAnalyzer analyzes buffer
                                              ↓
                           Creates PacketMapping with parameters
                                              ↓
                          Stores in mappings collection
                                              ↓
                    User exports → Java constants generated
```

## Configuration

Access configuration via RuneLite's plugin settings:

- **Auto-start monitoring**: Start monitoring when plugin loads
- **Show debug logs**: Display detailed logging
- **Action correlation timeout**: Time window to match actions with packets (default: 100ms)
- **Max packet history**: Maximum packets to store (default: 1000)
- **Auto-export path**: Directory for automatic exports
- **Export on stop**: Auto-export when stopping monitoring

## Troubleshooting

### Issue: No packets being captured

**Solution:**
- Verify `ObfuscatedNames` are up to date
- Check that the PacketWriter field name is correct
- Ensure monitoring is actually started (green status)
- Try performing more obvious actions (walking, examining items)

### Issue: Incorrect parameter mappings

**Solution:**
- The heuristic-based parameter detection may not be perfect
- Manually verify the parameter order
- Compare with known packet structures
- Perform the same action multiple times to find patterns

### Issue: Transformation detection errors

**Solution:**
- Buffer analysis uses heuristics and may be wrong
- Cross-reference with actual buffer write methods
- Use packet comparison to verify transformations
- Some transformations may require manual identification

## Advanced Usage

### Custom Packet Analysis

You can extend the buffer analyzer for custom transformation detection:

```java
EnhancedBufferAnalyzer.analyzeBuffer(bufferData, length);
```

### Pattern Detection

Analyze multiple packets to find patterns:

```java
List<byte[]> packetBuffers = ...;
PacketPattern pattern = EnhancedBufferAnalyzer.analyzePacketPattern(packetBuffers);
```

### Programmatic Access

Access mappings programmatically:

```java
@Inject
private PacketMappingTool mappingTool;

Map<String, PacketMapping> mappings = mappingTool.getMappings();
PacketMapping opcodeMapping = mappings.get("OPOBJ1");
```

## Limitations

1. **Obfuscation Changes**: Requires updating `ObfuscatedNames` after each game update
2. **Heuristic Detection**: Buffer analysis uses heuristics and may not be 100% accurate
3. **Complex Packets**: Very complex packets may require manual analysis
4. **Performance**: Continuous monitoring has minimal but measurable overhead
5. **Queue Access**: Some packet queue implementations may not be easily accessible

## Best Practices

1. **Isolate Actions**: Perform one action at a time for clearest mapping
2. **Repeat Actions**: Do the same action multiple times to verify consistency
3. **Compare Results**: Cross-reference with known packet structures
4. **Update Frequently**: Keep obfuscated names current with game updates
5. **Manual Verification**: Always verify auto-generated mappings
6. **Export Regularly**: Save mappings before stopping the plugin

## Contributing

To improve packet detection:

1. Add new action type detection in `determineActionType()`
2. Extend parameter deduction in `PacketMapping.deduceParameters()`
3. Add transformation detection in `EnhancedBufferAnalyzer`
4. Update write method name inference

## License

This tool is for educational and debugging purposes only.

## Credits

Built for OSRS packet analysis and debugging using RuneLite framework.
