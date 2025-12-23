package com.kraken.api.core.packet;

import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketMethods;
import com.kraken.api.core.packet.model.PacketType;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * {@code PacketClient} is an instance-based RuneLite client packet sending utility which uses reflection to
 * construct and send low level packets directly to the game servers. Generally you should not use this class directly
 * as it functions at a low level when sending packets.
 * <p>
 * Instead, it's recommended to use the higher level API's like {@code MousePackets}, {@code WidgetPackets}, or {@code NpcPackets} for
 * sending game packets to the server based on your specific entity interaction needs (clicking interfaces, NPC's, GameObjects, etc...
 */
@Slf4j
@Singleton
public class PacketClient {

    private final PacketMethods methods;

    @Getter
    private final Client client;

    /**
     * Creates a new PacketSender. This constructor initializes packet queueing functionality by either loading the client packet
     * sending method from the cached json file or running an analysis on the RuneLite injected client
     * to determine the packet sending method.
     *
     * @param client The RuneLite Client instance.
     */
    @Inject
    @SneakyThrows
    public PacketClient(Client client) {
        this.methods = PacketMethodLocator.packetMethods;
        this.client = client;

        if (this.methods == null) {
            // This is a hard failure because without the packet methods, no packets can be sent.
            // PacketMethodLocator.initializePackets() must be called before this class is injected.
            throw new RuntimeException("Packet queuing method could not be determined. Make sure you initialize packets with context.initializePackets()" +
                    "before constructing this class.");
        }
    }

    /**
     * Constructs and sends a packet to the game server.
     * This is the primary public method of this class.
     *
     * @param def     The {@link PacketDefinition} enumeration defining the packet structure.
     * @param objects The data (payload) for the packet, in the order defined by the PacketDefinition.
     */
    public void sendPacket(PacketDefinition def, Object... objects) {
        // 1. Get all necessary reflection components to build and send the packet.
        Object packetBufferNode = null;
        Method getPacketBufferNode = getGetPacketBufferNode();
        Class<?> clientPacket = getClientPacketClass();
        Object isaac = getIsaacObject();

        if (getPacketBufferNode == null || clientPacket == null || isaac == null) {
            log.error("Failed to get critical reflection components for sending packet: {}", def.getName());
            return;
        }

        // 2. Invoke the getPacketBufferNode method to create a new packet node instance.
        // This method is obfuscated and may require a "garbage value" of a specific type.
        getPacketBufferNode.setAccessible(true);
        long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.getPacketBufferNodeGarbageValue));

        try {
            Field packetField = fetchPacketField(def.getName());
            if (packetField == null) {
                log.error("Could not find packet field for: {}", def.getName());
                getPacketBufferNode.setAccessible(false);
                return;
            }
            Object packetDefInstance = packetField.get(clientPacket);

            // The method signature for getPacketBufferNode changes based on the obfuscated garbage value.
            if (garbageValue < 256) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, Byte.parseByte(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            } else if (garbageValue < 32768) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, Short.parseShort(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            } else if (garbageValue < Integer.MAX_VALUE) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, Integer.parseInt(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to invoke getPacketBufferNode: ", e);
            e.printStackTrace();
        } finally {
            getPacketBufferNode.setAccessible(false);
        }

        if (packetBufferNode == null) {
            log.error("PacketBufferNode was null after creation attempt for packet: {}", def.getName());
            return;
        }

        // 3. Get the raw 'buffer' object from the 'packetBufferNode' to write data into.
        Object buffer = null;
        try {
            Field bufferField = packetBufferNode.getClass().getDeclaredField(ObfuscatedNames.packetBufferFieldName);
            bufferField.setAccessible(true);
            buffer = bufferField.get(packetBufferNode);
            bufferField.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Failed to get packet buffer from node: ", e);
            e.printStackTrace();
            return; // Can't proceed without the buffer
        }

        // 4. Map the PacketType to the expected parameter order.
        // This is necessary because the varargs 'objects' must be written in a specific
        // sequence defined by the packet structure, not just the order they are passed in.
        List<String> params = null;
        if (def.getType() == PacketType.RESUME_NAMEDIALOG || def.getType() == PacketType.RESUME_STRINGDIALOG) {
            params = List.of("length", "string");
        }
        if (def.getType() == PacketType.OPHELDD) {
            params = List.of("selectedId", "selectedChildIndex", "selectedItemId", "destId", "destChildIndex", "destItemId");
        }
        if (def.getType() == PacketType.RESUME_COUNTDIALOG || def.getType() == PacketType.RESUME_OBJDIALOG) {
            params = List.of("var0");
        }
        if (def.getType() == PacketType.RESUME_PAUSEBUTTON) {
            params = List.of("var0", "var1");
        }
        if (def.getType() == PacketType.IF_BUTTON) {
            params = List.of("widgetId", "slot", "itemId");
        }
        if (def.getType() == PacketType.IF_SUBOP) {
            params = List.of("widgetId", "slot", "itemId", "menuIndex", "subActionIndex");
        }
        if (def.getType() == PacketType.IF_BUTTONX) {
            params = List.of("widgetId", "slot", "itemId", "opCode");
        }
        if (def.getType() == PacketType.OPLOC) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (def.getType() == PacketType.OPNPC) {
            params = List.of("npcIndex", "ctrlDown");
        }
        if (def.getType() == PacketType.OPPLAYER) {
            params = List.of("playerIndex", "ctrlDown");
        }
        if (def.getType() == PacketType.OPOBJ) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (def.getType() == PacketType.OPOBJT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (def.getType() == PacketType.EVENT_MOUSE_CLICK) {
            params = List.of("mouseInfo", "mouseX", "mouseY", "0");
        }
        if (def.getType() == PacketType.MOVE_GAMECLICK) {
            params = List.of("worldPointX", "worldPointY", "ctrlDown", "5");
        }
        if (def.getType() == PacketType.IF_BUTTONT) {
            params = List.of("sourceWidgetId", "sourceSlot", "sourceItemId", "destinationWidgetId",
                    "destinationSlot", "destinationItemId");
        }
        if (def.getType() == PacketType.OPLOCT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (def.getType() == PacketType.OPPLAYERT) {
            params = List.of("playerIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (def.getType() == PacketType.OPNPCT) {
            params = List.of("npcIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }

        // 5. If the packet type is recognized, write the data into the buffer.
        if (params != null) {
            for (int i = 0; i < def.getWriteData().length; i++) {
                // Find the index of the data element (e.g., "widgetId") in the params list
                int index = params.indexOf(def.getWriteData()[i]);
                // Get the corresponding value from the varargs
                Object writeValue = objects[index];

                // Write the value to the buffer using the specified method(s) (e.g., "strn", "writeInt")
                for (String s : def.getWriteMethods()[i]) {
                    if (s.equalsIgnoreCase("strn")) {
                        BufferUtils.writeStringCp1252NullTerminated((String) writeValue, buffer);
                        continue;
                    }
                    if (s.equalsIgnoreCase("strc")) {
                        BufferUtils.writeStringCp1252NullCircumfixed((String) writeValue, buffer);
                        continue;
                    }
                    // Assumes all other write methods take an Integer
                    BufferUtils.writeValue(s, (Integer) writeValue, buffer);
                }
            }

            // 6. Get the PacketWriter field and queue the fully constructed packet node.
            Field packetWriterField = getPacketWriterField();
            if (packetWriterField == null) {
                log.error("Could not get PacketWriter field to queue packet.");
                return;
            }

            packetWriterField.setAccessible(true);
            try {
                Object packetWriter = packetWriterField.get(null);
                if (packetWriter != null) {
                    addNode(packetWriter, packetBufferNode);
                } else {
                    log.error("PacketWriter object was null.");
                }
            } catch (Exception e) {
                log.error("Failed to add packet node to queue: ", e);
                e.printStackTrace();
            } finally {
                packetWriterField.setAccessible(false);
            }
        } else {
            log.warn("Unrecognized packet type, packet not sent: {}", def.getType());
        }
    }

    /**
     * Queues the completed {@code PacketBufferNode} to the client's {@code PacketWriter}.
     * This method handles two different ways the client might queue packets, determined
     * by the analysis from {@link PacketMethodLocator}.
     *
     * @param packetWriter     The client's PacketWriter instance.
     * @param packetBufferNode The fully constructed packet to be sent.
     */
    private void addNode(Object packetWriter, Object packetBufferNode) {
        try {
            if (methods.isUsingClientAddNode()) {
                // Path 1: The 'addNode' method is a member of the PacketWriter class itself.
                Method addNode = null;
                long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.addNodeGarbageValue));

                // Find the correct obfuscated method signature based on the garbage value type.
                if (garbageValue < 256) {
                    addNode = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNode.getClass(), byte.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, Byte.parseByte(ObfuscatedNames.addNodeGarbageValue));
                } else if (garbageValue < 32768) {
                    addNode = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNode.getClass(), short.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, Short.parseShort(ObfuscatedNames.addNodeGarbageValue));
                } else if (garbageValue < Integer.MAX_VALUE) {
                    addNode = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNode.getClass(), int.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, Integer.parseInt(ObfuscatedNames.addNodeGarbageValue));
                }

                if (addNode != null) {
                    addNode.setAccessible(false);
                }
            } else {
                // Path 2: The 'addNode' method is a static utility method found elsewhere.
                Method addNode = methods.getAddNodeMethod();
                addNode.setAccessible(true);

                if (addNode.getParameterCount() == 2) {
                    // Method signature is: addNode(packetWriter, packetBufferNode)
                    addNode.invoke(null, packetWriter, packetBufferNode);
                } else {
                    // Method signature is: addNode(packetWriter, packetBufferNode, garbageValue)
                    long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.addNodeGarbageValue));
                    if (garbageValue < 256) {
                        addNode.invoke(null, packetWriter, packetBufferNode, Byte.parseByte(ObfuscatedNames.addNodeGarbageValue));
                    } else if (garbageValue < 32768) {
                        addNode.invoke(null, packetWriter, packetBufferNode, Short.parseShort(ObfuscatedNames.addNodeGarbageValue));
                    } else if (garbageValue < Integer.MAX_VALUE) {
                        addNode.invoke(null, packetWriter, packetBufferNode, Integer.parseInt(ObfuscatedNames.addNodeGarbageValue));
                    }
                }
                addNode.setAccessible(false);
            }
        } catch (Exception e) {
            log.error("Failed during addNode packet queueing: ", e);
            e.printStackTrace();
        }
    }

    /**
     * Loads a class from the game client via RuneLite's class loader.
     *
     * @param name The obfuscated or non-obfuscated name of the class to load.
     * @return The loaded {@code Class} object, or null if not found.
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
     * Finds the static method responsible for creating a {@code PacketBufferNode}.
     *
     * @return The reflected {@code Method} object, or null if not found.
     */
    private Method getGetPacketBufferNode() {
        try {
            Class<?> packetBufferNodeAccessorClass = loadGameClientClass(ObfuscatedNames.classContainingGetPacketBufferNodeName);
            if (packetBufferNodeAccessorClass == null) {
                return null;
            }

            Class<?> packetBufferNodeClass = loadGameClientClass(ObfuscatedNames.packetBufferNodeClassName);
            if (packetBufferNodeClass == null) {
                return null;
            }

            // Find the method within the accessor class that returns a PacketBufferNode.
            // This is fragile and assumes only one such method exists.
            return Arrays.stream(packetBufferNodeAccessorClass.getDeclaredMethods())
                    .filter(m -> m.getReturnType().equals(packetBufferNodeClass))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get packet buffer node method: ", e);
        }
        return null;
    }

    /**
     * Loads the {@code ClientPacket} class, which contains static definitions for packets.
     *
     * @return The {@code ClientPacket} class, or null if not found.
     */
    private Class<?> getClientPacketClass() {
        return loadGameClientClass(ObfuscatedNames.clientPacketClassName);
    }

    /**
     * Finds the client's static {@code PacketWriter} field.
     *
     * @return The reflected {@code Field} object, or null if not found.
     */
    private Field getPacketWriterField() {
        try {
            return client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        } catch (NoSuchFieldException e) {
            log.error("Failed to get field: {}", ObfuscatedNames.packetWriterFieldName, e);
        }
        return null;
    }

    /**
     * Retrieves the {@code IsaacCipher} object from the {@code PacketWriter}.
     * The cipher is needed to correctly construct the packet header.
     *
     * @return The {@code IsaacCipher} instance, or null if failed.
     */
    private Object getIsaacObject() {
        try {
            Field packetWriterField = getPacketWriterField();
            if (packetWriterField == null) return null;

            packetWriterField.setAccessible(true);
            Object packetWriter = packetWriterField.get(null); // Get static field
            packetWriterField.setAccessible(false);

            if (packetWriter == null) {
                 log.error("PacketWriter object is null, cannot get ISAAC cipher.");
                 return null;
            }

            Class<?> packetWriterClass = packetWriter.getClass();
            Field isaacField = packetWriterClass.getDeclaredField(ObfuscatedNames.isaacCipherFieldName);
            isaacField.setAccessible(true);
            Object isaacObject = isaacField.get(packetWriter); // Get instance field
            isaacField.setAccessible(false);

            return isaacObject;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to get ISAAC object: ", e);
        }
        return null;
    }

    /**
     * Finds a specific static packet definition field within the {@code ClientPacket} class.
     *
     * @param name The name of the packet field (e.g., "IF_BUTTON1").
     * @return The reflected {@code Field} object, or null if not found.
     */
    private Field fetchPacketField(String name) {
        try {
            Class<?> clientPacket = getClientPacketClass();
            if (clientPacket == null) return null;
            return clientPacket.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            log.error("Failed to get packet field: {}", name, e);
        }
        return null;
    }
}