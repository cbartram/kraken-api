package com.kraken.api.core.packet;

import com.example.PacketUtils.PacketDef;
import com.example.PacketUtils.PacketType;
import com.example.Packets.BufferMethods;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketMethods;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * An instance-based RuneLite client packet sending utility.
 */
@Slf4j
@Singleton
public class PacketSender {
    private final PacketMethods methods;

    private final Object packetWriter;
    private final Object isaacCipher;
    private final Class<?> clientPacketClass;
    private final Class<?> packetBufferNodeClass;
    private final Method getPacketBufferNodeMethod;
    private final Field packetBufferField;

    /**
     * Creates a new PacketSender. This constructor initializes packet queueing functionality by either loading the client packet
     * sending method from the cached json file or running an analysis on the RuneLite injected client
     * to determine the packet sending method.
     *
     * @param client The RuneLite Client instance.
     */
    @Inject
    @SneakyThrows
    public PacketSender(Client client) {
        this.methods = PacketMethodLocator.packetMethods;

        if(this.methods == null) {
            throw new RuntimeException("Packet queuing method could not be determined. Make sure you initialize packets with context.initializePackets()" +
                    "before constructing this class.");
        }

        // 1. Get PacketWriter Field and Object
        // Field for the PacketWriter itself
        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        this.packetWriter = packetWriterField.get(null);
        packetWriterField.setAccessible(false);
        log.debug("Cached PacketWriter instance.");

        // 2. Get IsaacCipher
        Class<?> packetWriterClass = packetWriter.getClass();
        Field isaacField = packetWriterClass.getDeclaredField(ObfuscatedNames.isaacCipherFieldName);
        isaacField.setAccessible(true);
        this.isaacCipher = isaacField.get(packetWriter);
        isaacField.setAccessible(false);
        log.debug("Cached IsaacCipher instance.");

        // 3. Get Packet Class definitions
        ClassLoader clientLoader = client.getClass().getClassLoader();
        this.clientPacketClass = clientLoader.loadClass(ObfuscatedNames.clientPacketClassName);
        this.packetBufferNodeClass = clientLoader.loadClass(ObfuscatedNames.packetBufferNodeClassName);
        log.debug("Cached ClientPacket and PacketBufferNode classes.");

        // 4. Get getPacketBufferNode() factory method
        Class<?> factoryClass = clientLoader.loadClass(ObfuscatedNames.classContainingGetPacketBufferNodeName);
        Method foundMethod = null;
        for (Method method : factoryClass.getDeclaredMethods()) {
            if (method.getReturnType().equals(packetBufferNodeClass)) {
                foundMethod = method;
                break;
            }
        }
        this.getPacketBufferNodeMethod = foundMethod;
        if (this.getPacketBufferNodeMethod == null) {
            throw new NoSuchMethodException("Could not find getPacketBufferNode method in " + factoryClass.getName());
        }
        log.debug("Cached getPacketBufferNode method: {}", getPacketBufferNodeMethod.getName());

        // 5. Get PacketBuffer field from PacketBufferNode
        this.packetBufferField = this.packetBufferNodeClass.getDeclaredField(ObfuscatedNames.packetBufferFieldName);
        log.debug("Cached packetBuffer field: {}", packetBufferField.getName());
    }

    /**
     * Creates and allocates a new PacketBufferNode for a given packet definition. A packet biffer
     * node functions as a container or wrapper containing the packet data, ISAAC Cipher, and garbage value for the packet to be sent.
     *
     * @param def The packet definition.
     * @return The allocated PacketBufferNode object.
     */
    @SneakyThrows
    private Object createPacketBufferNode(PacketDef def) {
        Field packetField = clientPacketClass.getDeclaredField(def.name);
        Object clientPacket = packetField.get(null); // It's a static field

        getPacketBufferNodeMethod.setAccessible(true);
        Object packetBufferNode = null;
        try {
            // This replicates the original "garbage value" logic
            long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            if (garbageValue < 256) {
                packetBufferNode = getPacketBufferNodeMethod.invoke(null, clientPacket, isaacCipher, (byte) garbageValue);
            } else if (garbageValue < 32768) {
                packetBufferNode = getPacketBufferNodeMethod.invoke(null, clientPacket, isaacCipher, (short) garbageValue);
            } else if (garbageValue < Integer.MAX_VALUE) {
                packetBufferNode = getPacketBufferNodeMethod.invoke(null, clientPacket, isaacCipher, (int) garbageValue);
            } else {
                throw new IllegalArgumentException("Unsupported garbage value: " + garbageValue);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("Failed to invoke getPacketBufferNode", e);
        } finally {
            getPacketBufferNodeMethod.setAccessible(false);
        }
        return packetBufferNode;
    }

    /**
     * Gets the raw PacketBuffer object from inside a PacketBufferNode.
     */
    @SneakyThrows
    private Object getBufferFromNode(Object packetBufferNode) {
        return packetBufferField.get(packetBufferNode);
    }

    /**
     * Queues a finalized PacketBufferNode to be sent by the PacketWriter.
     * This is the refactored "addNode" method.
     *
     * @param packetBufferNode The populated packet to send.
     */
    @SneakyThrows
    private void queuePacket(Object packetBufferNode) {
        Method addNodeMethod;
        Object[] args;
        Object targetInstance; // null for static, packetWriter for member

        if (methods.usingClientAddNode) {
            // Method is on the PacketWriter instance
            targetInstance = this.packetWriter;

            // Replicate garbage value logic for the addNode method
            long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.addNodeGarbageValue));
            if (garbageValue < 256) {
                addNodeMethod = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNodeClass, byte.class);
                args = new Object[]{packetBufferNode, (byte) garbageValue};
            } else if (garbageValue < 32768) {
                addNodeMethod = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNodeClass, short.class);
                args = new Object[]{packetBufferNode, (short) garbageValue};
            } else if (garbageValue < Integer.MAX_VALUE) {
                addNodeMethod = packetWriter.getClass().getDeclaredMethod(ObfuscatedNames.addNodeMethodName, packetBufferNodeClass, int.class);
                args = new Object[]{packetBufferNode, (int) garbageValue};
            } else {
                throw new IllegalArgumentException("Unsupported addNode garbage value: " + garbageValue);
            }

        } else {
            // Method is static, retrieved from PacketMethodLocator
            targetInstance = null;
            addNodeMethod = methods.addNodeMethod; // This is the cached Method object

            // Replicate garbage value logic for the static method
            long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.addNodeGarbageValue));
            if (addNodeMethod.getParameterCount() == 2) {
                args = new Object[]{packetWriter, packetBufferNode};
            } else if (garbageValue < 256) {
                args = new Object[]{packetWriter, packetBufferNode, (byte) garbageValue};
            } else if (garbageValue < 32768) {
                args = new Object[]{packetWriter, packetBufferNode, (short) garbageValue};
            } else if (garbageValue < Integer.MAX_VALUE) {
                args = new Object[]{packetWriter, packetBufferNode, (int) garbageValue};
            } else {
                throw new IllegalArgumentException("Unsupported static addNode garbage value: " + garbageValue);
            }
        }

        // --- Invoke ---
        try {
            addNodeMethod.setAccessible(true);
            addNodeMethod.invoke(targetInstance, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("Failed to invoke addNode method: {}", addNodeMethod.getName(), e);
        } finally {
            addNodeMethod.setAccessible(false);
        }
    }

    /**
     * This method preserves the original, brittle mapping logic from the
     * PacketReflection class. Ideally, this info would be on the PacketDef enum.
     *
     * @param def The PacketDef.
     * @return A list of parameter names in the order they are expected.
     */
    private List<String> getParameterOrderForDef(PacketDef def) {
        if (def.type == PacketType.RESUME_NAMEDIALOG || def.type == PacketType.RESUME_STRINGDIALOG) {
            return List.of("length", "string");
        }
        if (def.type == PacketType.OPHELDD) {
            return List.of("selectedId", "selectedChildIndex", "selectedItemId", "destId", "destChildIndex", "destItemId");
        }
        if (def.type == PacketType.RESUME_COUNTDIALOG || def.type == PacketType.RESUME_OBJDIALOG) {
            return List.of("var0");
        }
        if (def.type == PacketType.RESUME_PAUSEBUTTON) {
            return List.of("var0", "var1");
        }
        if (def.type == PacketType.IF_BUTTON) {
            return List.of("widgetId", "slot", "itemId");
        }
        if (def.type == PacketType.IF_SUBOP) {
            return List.of("widgetId", "slot", "itemId", "menuIndex", "subActionIndex");
        }
        if (def.type == PacketType.IF_BUTTONX) {
            return List.of("widgetId", "slot", "itemId", "opCode");
        }
        if (def.type == PacketType.OPLOC) {
            return List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (def.type == PacketType.OPNPC) {
            return List.of("npcIndex", "ctrlDown");
        }
        if (def.type == PacketType.OPPLAYER) {
            return List.of("playerIndex", "ctrlDown");
        }
        if (def.type == PacketType.OPOBJ) {
            return List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (def.type == PacketType.OPOBJT) {
            return List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId", "ctrlDown");
        }
        if (def.type == PacketType.EVENT_MOUSE_CLICK) {
            return List.of("mouseInfo", "mouseX", "mouseY", "0");
        }
        if (def.type == PacketType.MOVE_GAMECLICK) {
            return List.of("worldPointX", "worldPointY", "ctrlDown", "5");
        }
        if (def.type == PacketType.IF_BUTTONT) {
            return List.of("sourceWidgetId", "sourceSlot", "sourceItemId", "destinationWidgetId", "destinationSlot", "destinationItemId");
        }
        if (def.type == PacketType.OPLOCT) {
            return List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId", "ctrlDown");
        }
        if (def.type == PacketType.OPPLAYERT) {
            return List.of("playerIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (def.type == PacketType.OPNPCT) {
            return List.of("npcIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        return null; // No mapping found
    }

    /**
     * Constructs, populates, and sends a packet.
     *
     * @param def  The packet definition (e.g., OPLOC).
     * @param data The data for the packet, which must be in the exact
     * order specified by the internal parameter mapping.
     */
    public void sendPacket(PacketDef def, Object... data) {
        Object packetBufferNode = createPacketBufferNode(def);
        Object buffer = getBufferFromNode(packetBufferNode);

        List<String> paramOrder = getParameterOrderForDef(def);
        if (paramOrder == null) {
            log.error("No parameter mapping found for packet type: {}. Packet not sent.", def.type);
            return;
        }

        if (paramOrder.size() != data.length) {
            log.error("Packet data/param mismatch for {}. Expected {} args, got {}. Packet not sent.",
                    def.name, paramOrder.size(), data.length);
            return;
        }

        // This logic is preserved from the original. It maps the packet's
        // required write operations (def.writeData) to the ordered
        // input data (data) via the parameter name list (paramOrder).
        try {
            for (int i = 0; i < def.writeData.length; i++) {
                int index = paramOrder.indexOf(def.writeData[i]);
                if (index == -1) {
                    log.warn("Packet {}: could not find param '{}' in mapping.", def.name, def.writeData[i]);
                    continue;
                }

                Object writeValue = data[index];
                for (String writeMethod : def.writeMethods[i]) {
                    // Use BufferMethods to write the data
                    if (writeMethod.equalsIgnoreCase("strn")) {
                        BufferMethods.writeStringCp1252NullTerminated((String) writeValue, buffer);
                    } else if (writeMethod.equalsIgnoreCase("strc")) {
                        BufferMethods.writeStringCp1252NullCircumfixed((String) writeValue, buffer);
                    } else {
                        BufferMethods.writeValue(writeMethod, (Integer) writeValue, buffer);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to write packet data for {}. Packet not sent.", def.name, e);
            return;
        }

        queuePacket(packetBufferNode);
        log.debug("Sent packet: {}", def.name);
    }
}
