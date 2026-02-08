package com.kraken.api.plugins.packetmapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Practical packet monitor that polls the packet queue and analyzes packets
 * This approach is more reliable for RuneLite plugins than bytecode manipulation
 */
@Slf4j
@Singleton
public class PacketQueueMonitor {

    @Inject
    private Client client;

    @Inject
    private PacketMappingTool mappingTool;

    private volatile boolean monitoring = false;
    private Thread monitorThread;
    private Queue<PacketSnapshot> packetHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY = 1000;

    /**
     * Starts monitoring the packet queue
     */
    public void startMonitoring() {
        if (monitoring) {
            log.warn("Already monitoring packets");
            return;
        }

        monitoring = true;
        monitorThread = new Thread(this::monitorLoop, "PacketQueueMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        
        log.info("Packet queue monitoring started");
    }

    /**
     * Stops monitoring the packet queue
     */
    public void stopMonitoring() {
        monitoring = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        log.info("Packet queue monitoring stopped");
    }

    /**
     * Main monitoring loop
     */
    private void monitorLoop() {
        Set<Object> seenPackets = Collections.newSetFromMap(new WeakHashMap<>());
        
        while (monitoring) {
            try {
                // Get the PacketWriter
                Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
                packetWriterField.setAccessible(true);
                Object packetWriter = packetWriterField.get(null);

                if (packetWriter != null) {
                    // Try to access the packet queue/buffer
                    inspectPacketWriter(packetWriter, seenPackets);
                }

                // Sleep briefly to avoid excessive CPU usage
                Thread.sleep(5);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in packet monitoring loop: ", e);
            }
        }
    }

    /**
     * Inspects the PacketWriter object for new packets
     */
    private void inspectPacketWriter(Object packetWriter, Set<Object> seenPackets) throws Exception {
        // Look for fields that might contain the packet queue
        Class<?> packetWriterClass = packetWriter.getClass();
        
        for (Field field : packetWriterClass.getDeclaredFields()) {
            field.setAccessible(true);
            Object fieldValue = field.get(packetWriter);
            
            // Check if this field is a queue-like structure
            if (fieldValue != null && isQueueLike(fieldValue)) {
                inspectQueue(fieldValue, seenPackets);
            }
            
            // Check for PacketBufferNode instances
            if (fieldValue != null && isPacketBufferNode(fieldValue)) {
                if (!seenPackets.contains(fieldValue)) {
                    seenPackets.add(fieldValue);
                    capturePacket(fieldValue);
                }
            }
        }
    }

    /**
     * Checks if an object appears to be a queue
     */
    private boolean isQueueLike(Object obj) {
        String className = obj.getClass().getSimpleName().toLowerCase();
        return className.contains("queue") || className.contains("deque") || 
               className.contains("node") && className.contains("queue");
    }

    /**
     * Checks if an object is a PacketBufferNode
     */
    private boolean isPacketBufferNode(Object obj) {
        return obj.getClass().getSimpleName().equals(ObfuscatedNames.packetBufferNodeClassName);
    }

    /**
     * Inspects a queue for PacketBufferNodes
     */
    private void inspectQueue(Object queue, Set<Object> seenPackets) throws Exception {
        // Try different methods to access queue contents
        
        // Try Collection interface
        if (queue instanceof Collection) {
            Collection<?> collection = (Collection<?>) queue;
            for (Object item : collection) {
                if (isPacketBufferNode(item) && !seenPackets.contains(item)) {
                    seenPackets.add(item);
                    capturePacket(item);
                }
            }
            return;
        }

        // Try to find iterator method
        try {
            Method iteratorMethod = queue.getClass().getMethod("iterator");
            Object iterator = iteratorMethod.invoke(queue);
            
            Method hasNextMethod = iterator.getClass().getMethod("hasNext");
            Method nextMethod = iterator.getClass().getMethod("next");
            
            while ((Boolean) hasNextMethod.invoke(iterator)) {
                Object item = nextMethod.invoke(iterator);
                if (isPacketBufferNode(item) && !seenPackets.contains(item)) {
                    seenPackets.add(item);
                    capturePacket(item);
                }
            }
        } catch (NoSuchMethodException e) {
            // Not iterable, try other approaches
            inspectNodeStructure(queue, seenPackets);
        }
    }

    /**
     * Inspects a linked-list style node structure
     */
    private void inspectNodeStructure(Object node, Set<Object> seenPackets) throws Exception {
        // Look for common node field names: next, previous, etc.
        Field nextField = findField(node.getClass(), "next", "successor", "af", "al", "aq");
        
        if (nextField != null) {
            nextField.setAccessible(true);
            Object current = node;
            int maxIterations = 100; // Prevent infinite loops
            
            while (current != null && maxIterations-- > 0) {
                if (isPacketBufferNode(current) && !seenPackets.contains(current)) {
                    seenPackets.add(current);
                    capturePacket(current);
                }
                
                current = nextField.get(current);
                
                // Check for circular reference
                if (current == node) {
                    break;
                }
            }
        }
    }

    /**
     * Finds a field by trying multiple possible names
     */
    private Field findField(Class<?> clazz, String... possibleNames) {
        for (String name : possibleNames) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Try next name
            }
        }
        return null;
    }

    /**
     * Captures and analyzes a packet
     */
    private void capturePacket(Object packetBufferNode) {
        try {
            // Create a snapshot before the packet is processed
            PacketSnapshot snapshot = createSnapshot(packetBufferNode);
            
            // Add to history
            packetHistory.offer(snapshot);
            while (packetHistory.size() > MAX_HISTORY) {
                packetHistory.poll();
            }
            
            // Analyze the packet
            mappingTool.analyzePacket(packetBufferNode);
            
            log.debug("Captured packet: {}", snapshot);
            
        } catch (Exception e) {
            log.error("Error capturing packet: ", e);
        }
    }

    /**
     * Creates a snapshot of a PacketBufferNode
     */
    private PacketSnapshot createSnapshot(Object packetBufferNode) throws Exception {
        PacketSnapshot snapshot = new PacketSnapshot();
        snapshot.setTimestamp(System.currentTimeMillis());
        
        // Extract packet definition
        try {
            Field packetField = findField(packetBufferNode.getClass(), "a", "packet", "packetDef");
            if (packetField != null) {
                packetField.setAccessible(true);
                Object packetDef = packetField.get(packetBufferNode);
                snapshot.setPacketDefinition(packetDef);
            }
        } catch (Exception e) {
            log.debug("Could not extract packet definition: {}", e.getMessage());
        }
        
        // Extract buffer
        try {
            Field bufferField = packetBufferNode.getClass().getDeclaredField(ObfuscatedNames.packetBufferFieldName);
            bufferField.setAccessible(true);
            Object buffer = bufferField.get(packetBufferNode);
            snapshot.setBuffer(buffer);
            
            // Get buffer contents
            Field offsetField = buffer.getClass().getDeclaredField(ObfuscatedNames.bufferOffsetField);
            offsetField.setAccessible(true);
            int offset = offsetField.getInt(buffer);
            snapshot.setBufferLength(offset);
            
            Field arrayField = buffer.getClass().getDeclaredField(ObfuscatedNames.bufferArrayField);
            arrayField.setAccessible(true);
            byte[] bufferArray = (byte[]) arrayField.get(buffer);
            snapshot.setBufferData(Arrays.copyOf(bufferArray, offset));
            
        } catch (Exception e) {
            log.debug("Could not extract buffer: {}", e.getMessage());
        }
        
        return snapshot;
    }

    /**
     * Gets the packet history
     */
    public List<PacketSnapshot> getPacketHistory() {
        return new ArrayList<>(packetHistory);
    }

    /**
     * Clears the packet history
     */
    public void clearHistory() {
        packetHistory.clear();
    }

    /**
     * Snapshot of a packet at a point in time
     */
    public static class PacketSnapshot {
        private long timestamp;
        private Object packetDefinition;
        private Object buffer;
        private int bufferLength;
        private byte[] bufferData;

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Object getPacketDefinition() { return packetDefinition; }
        public void setPacketDefinition(Object packetDefinition) { this.packetDefinition = packetDefinition; }
        
        public Object getBuffer() { return buffer; }
        public void setBuffer(Object buffer) { this.buffer = buffer; }
        
        public int getBufferLength() { return bufferLength; }
        public void setBufferLength(int bufferLength) { this.bufferLength = bufferLength; }
        
        public byte[] getBufferData() { return bufferData; }
        public void setBufferData(byte[] bufferData) { this.bufferData = bufferData; }

        @Override
        public String toString() {
            return String.format("PacketSnapshot{time=%d, length=%d}", timestamp, bufferLength);
        }
    }
}
