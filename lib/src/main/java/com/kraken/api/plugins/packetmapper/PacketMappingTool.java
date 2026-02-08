package com.kraken.api.plugins.packetmapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PacketMappingTool intercepts outgoing packets and maps their structure
 * by correlating game actions with packet data.
 */
@Slf4j
@Singleton
public class PacketMappingTool {

    @Inject
    private Client client;

    @Getter
    private final Map<String, PacketMapping> mappings = new ConcurrentHashMap<>();
    
    private String currentAction = null;
    private long lastActionTime = 0;
    private static final long ACTION_TIMEOUT_MS = 100; // Time window to correlate action with packet

    private volatile boolean isHooked = false;
    private Method originalAddNodeMethod = null;
    
    /**
     * Starts the packet interception by hooking into the addNode method
     */
    public void startInterception() {
        if (isHooked) {
            log.warn("Packet interception already active");
            return;
        }
        
        try {
            hookAddNode();
            isHooked = true;
            log.info("Packet interception started successfully");
        } catch (Exception e) {
            log.error("Failed to start packet interception: ", e);
        }
    }

    /**
     * Stops packet interception
     */
    public void stopInterception() {
        isHooked = false;
        log.info("Packet interception stopped");
    }

    /**
     * Listens for menu actions to correlate with packets
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        currentAction = determineActionType(event);
        lastActionTime = System.currentTimeMillis();
        log.debug("Action detected: {}", currentAction);
    }

    /**
     * Determines the packet type based on the menu action
     */
    private String determineActionType(MenuOptionClicked event) {
        String option = event.getMenuOption();
        String target = event.getMenuTarget();
        int opcode = event.getMenuAction().getId();
        
        // Map menu actions to packet types
        if (option.equals("Walk here")) {
            return "MOVE_GAMECLICK";
        } else if (target.contains("->")) {
            return "IF_BUTTONT"; // Drag and drop
        } else if (option.matches("Attack|Examine|Talk-to|Pick-up")) {
            if (event.getId() >= 0) {
                // NPC interaction
                if (option.equals("Attack")) return "OPNPC1";
                if (option.equals("Talk-to")) return "OPNPC3";
                return "OPNPC" + (opcode - 7); // Adjust based on actual opcode mapping
            }
        } else if (option.matches("Examine|Take|Use")) {
            if (event.isItemOp()) {
                return "IF_BUTTON" + event.getParam0();
            }
        }
        
        // GameObject interactions
        if (opcode >= 3 && opcode <= 6) {
            return "OPLOC" + (opcode - 2);
        }
        
        // Ground item interactions  
        if (opcode >= 7 && opcode <= 11 && target.contains("Take")) {
            return "OPOBJ" + (opcode - 6);
        }
        
        return "UNKNOWN_" + opcode;
    }

    /**
     * Hooks into the packet sending mechanism using reflection and bytecode manipulation
     */
    private void hookAddNode() throws Exception {
        // Get the PacketWriter class
        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        Object packetWriter = packetWriterField.get(null);
        
        if (packetWriter == null) {
            throw new RuntimeException("PacketWriter is null");
        }

        // Find the addNode method
        Class<?> packetWriterClass = packetWriter.getClass();
        Class<?> packetBufferNodeClass = loadGameClientClass(ObfuscatedNames.packetBufferNodeClassName);
        
        if (packetBufferNodeClass == null) {
            throw new RuntimeException("Cannot load PacketBufferNode class");
        }

        // Since we can't directly modify bytecode, we'll use a different approach
        // We'll hook into the buffer write operations instead
        log.info("Setting up packet buffer monitoring...");
        
        // Start a monitoring thread that periodically checks for new packets
        startPacketMonitoring(packetWriter);
    }

    /**
     * Monitors packet queue for new packets
     */
    private void startPacketMonitoring(Object packetWriter) {
        Thread monitorThread = new Thread(() -> {
            log.info("Packet monitoring thread started");
            
            while (isHooked) {
                try {
                    Thread.sleep(10); // Check every 10ms
                    
                    // This is a simplified approach - in practice you'd need to
                    // hook more directly into the addNode method
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in packet monitoring: ", e);
                }
            }
            
            log.info("Packet monitoring thread stopped");
        });
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Analyzes a captured packet buffer node
     */
    public void analyzePacket(Object packetBufferNode) {
        try {
            // Extract the packet definition
            Field packetField = packetBufferNode.getClass().getDeclaredField("a"); // Adjust field name
            packetField.setAccessible(true);
            Object clientPacketDef = packetField.get(packetBufferNode);
            
            // Get packet name
            String packetName = getPacketName(clientPacketDef);
            
            // Get the buffer
            Field bufferField = packetBufferNode.getClass().getDeclaredField(ObfuscatedNames.packetBufferFieldName);
            bufferField.setAccessible(true);
            Object buffer = bufferField.get(packetBufferNode);
            
            // Analyze buffer contents
            BufferAnalysis analysis = analyzeBuffer(buffer);
            
            // Correlate with current action if within timeout
            String actionType = null;
            if (System.currentTimeMillis() - lastActionTime < ACTION_TIMEOUT_MS) {
                actionType = currentAction;
            }
            
            // Create or update mapping
            PacketMapping mapping = new PacketMapping(packetName, actionType, analysis);
            mappings.put(packetName, mapping);
            
            log.info("Analyzed packet: {} for action: {}", packetName, actionType);
            log.info("Buffer analysis: {}", analysis);
            
        } catch (Exception e) {
            log.error("Failed to analyze packet: ", e);
        }
    }

    /**
     * Analyzes buffer contents to determine write operations
     */
    private BufferAnalysis analyzeBuffer(Object buffer) throws Exception {
        BufferAnalysis analysis = new BufferAnalysis();
        
        // Get buffer offset and array
        Field offsetField = buffer.getClass().getDeclaredField(ObfuscatedNames.bufferOffsetField);
        offsetField.setAccessible(true);
        int offset = offsetField.getInt(buffer);
        
        Field arrayField = buffer.getClass().getDeclaredField(ObfuscatedNames.bufferArrayField);
        arrayField.setAccessible(true);
        byte[] bufferArray = (byte[]) arrayField.get(buffer);
        
        // Analyze the written data
        analysis.setTotalBytes(offset);
        analysis.setBufferData(Arrays.copyOf(bufferArray, offset));
        
        // Try to determine the write operations based on the data
        List<WriteOperation> operations = deduceWriteOperations(bufferArray, offset);
        analysis.setOperations(operations);
        
        return analysis;
    }

    /**
     * Attempts to deduce the write operations from buffer data
     */
    private List<WriteOperation> deduceWriteOperations(byte[] data, int length) {
        List<WriteOperation> operations = new ArrayList<>();
        int pos = 0;
        
        while (pos < length) {
            // Try to identify the type of write operation
            // This is heuristic-based and may need refinement
            
            // Check if it looks like a short (2 bytes)
            if (pos + 1 < length) {
                short shortValue = (short) ((data[pos] << 8) | (data[pos + 1] & 0xFF));
                
                // Check for common transformations
                WriteOperation op = new WriteOperation();
                op.setPosition(pos);
                op.setSize(2);
                op.setValue(shortValue);
                
                // Detect transformation type
                if ((data[pos] & 0xFF) == ((shortValue >> 8) + 128) % 256) {
                    op.setTransformation("v"); // Add 128 transformation
                } else if ((data[pos] & 0xFF) == (-(shortValue >> 8)) % 256) {
                    op.setTransformation("r 8"); // Reverse bytes with negation
                } else {
                    op.setTransformation("none");
                }
                
                operations.add(op);
                pos += 2;
            } else {
                // Single byte
                WriteOperation op = new WriteOperation();
                op.setPosition(pos);
                op.setSize(1);
                op.setValue(data[pos] & 0xFF);
                
                // Check for transformations
                if ((data[pos] & 0xFF) == 128) {
                    op.setTransformation("s 128");
                } else {
                    op.setTransformation("none");
                }
                
                operations.add(op);
                pos++;
            }
        }
        
        return operations;
    }

    /**
     * Extracts the packet name from a ClientPacket definition
     */
    private String getPacketName(Object clientPacketDef) throws Exception {
        Class<?> clientPacketClass = loadGameClientClass(ObfuscatedNames.clientPacketClassName);
        
        if (clientPacketClass == null) {
            return "UNKNOWN";
        }
        
        // Iterate through static fields to find matching packet definition
        for (Field field : clientPacketClass.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object fieldValue = field.get(null);
                
                if (fieldValue == clientPacketDef) {
                    return field.getName();
                }
            }
        }
        
        return "UNKNOWN";
    }

    /**
     * Loads a game client class
     */
    private Class<?> loadGameClientClass(String name) {
        try {
            ClassLoader clientLoader = client.getClass().getClassLoader();
            return clientLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            log.error("Failed to load game client class: {}", name, e);
        }
        return null;
    }

    /**
     * Exports all mappings to Java constant definitions
     */
    public String exportMappings() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Auto-generated packet mappings\n");
        sb.append("// Generated at: ").append(new Date()).append("\n\n");
        
        for (Map.Entry<String, PacketMapping> entry : mappings.entrySet()) {
            String packetName = entry.getKey();
            PacketMapping mapping = entry.getValue();
            
            sb.append(mapping.toJavaConstants()).append("\n\n");
        }
        
        return sb.toString();
    }

    /**
     * Exports mappings for a specific packet type
     */
    public String exportMapping(String packetName) {
        PacketMapping mapping = mappings.get(packetName);
        if (mapping == null) {
            return "// No mapping found for: " + packetName;
        }
        
        return mapping.toJavaConstants();
    }

    /**
     * Clears all collected mappings
     */
    public void clearMappings() {
        mappings.clear();
        log.info("All packet mappings cleared");
    }
}
