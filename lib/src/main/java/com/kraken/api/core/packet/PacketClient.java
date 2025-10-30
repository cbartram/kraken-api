package com.kraken.api.core.packet;

import com.example.Packets.BufferMethods;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.model.PacketDef;
import com.kraken.api.core.packet.model.PacketMethods;
import com.kraken.api.core.packet.model.PacketType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An instance-based RuneLite client packet sending utility. Generally you should not use this class directly
 * as it functions at a low level to send packets via the RuneLite client using reflection.
 * <p />
 * Instead, it's recommended to use the higher level API's like {@code MousePackets}, {@code WidgetPackets}, {@code NpcPackets} etc... for
 * sending game packets to the server.
 */
@Slf4j
@Singleton
public class PacketClient {

    private final PacketMethods methods;
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

        if(this.methods == null) {
            throw new RuntimeException("Packet queuing method could not be determined. Make sure you initialize packets with context.initializePackets()" +
                    "before constructing this class.");
        }
    }

    public Class<?> loadGameClientClass(String name) {
        try {
            ClassLoader clientLoader = client.getClass().getClassLoader();
            return clientLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Class<?> getClassWithGetPacketBufferNode() {
        return loadGameClientClass(ObfuscatedNames.classContainingGetPacketBufferNodeName);
    }

    public Method getGetPacketBufferNode() {
        try {
            Class<?> packetBufferNodeClass = loadGameClientClass(ObfuscatedNames.packetBufferNodeClassName);
            return Arrays.stream(getClassWithGetPacketBufferNode().getDeclaredMethods()).filter(m -> m.getReturnType().equals(packetBufferNodeClass)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Class<?> getClientPacketClass(){
        return loadGameClientClass(ObfuscatedNames.clientPacketClassName);
    }

    public  Field getPacketWriterField() {
        try {
            return client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getIsaacObject() {
        try {

            Field packetWriterField = getPacketWriterField();
            packetWriterField.setAccessible(true);
            Class<?> packetWriterClass = packetWriterField.get(null).getClass();
            Object packetWriter = packetWriterField.get(null);
            packetWriterField.setAccessible(false);

            Field isaacField = packetWriterClass.getDeclaredField(ObfuscatedNames.isaacCipherFieldName);
            isaacField.setAccessible(true);

            Object isaacObject = isaacField.get(packetWriter);
            isaacField.setAccessible(false);
            return isaacObject;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Constructs and sends a packet to the game server.
     * @param def Packet definition
     * @param objects The packet data
     */
    public void sendPacket(PacketDef def, Object... objects) {
        Object packetBufferNode = null;
        Method getPacketBufferNode = getGetPacketBufferNode();
        Class ClientPacket = getClientPacketClass();
        Object isaac = getIsaacObject();

        getPacketBufferNode.setAccessible(true);
        long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.getPacketBufferNodeGarbageValue));
        if (garbageValue < 256) {
            try {
                packetBufferNode = getPacketBufferNode.invoke(null, fetchPacketField(def.getName()).get(ClientPacket),
                        isaac, Byte.parseByte(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else if (garbageValue < 32768) {
            try {
                packetBufferNode = getPacketBufferNode.invoke(null, fetchPacketField(def.getName()).get(ClientPacket),
                        isaac, Short.parseShort(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else if (garbageValue < Integer.MAX_VALUE) {
            try {
                packetBufferNode = getPacketBufferNode.invoke(null, fetchPacketField(def.getName()).get(ClientPacket),
                        isaac, Integer.parseInt(ObfuscatedNames.getPacketBufferNodeGarbageValue));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        Object buffer = null;
        try {
            buffer = packetBufferNode.getClass().getDeclaredField(ObfuscatedNames.packetBufferFieldName).get(packetBufferNode);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        getPacketBufferNode.setAccessible(false);
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

        if (params != null) {
            for (int i = 0; i < def.getWriteData().length; i++) {
                int index = params.indexOf(def.getWriteData()[i]);
                Object writeValue = objects[index];
                for (String s : def.getWriteMethods()[i]) {
                    if (s.equalsIgnoreCase("strn")) {
                        BufferMethods.writeStringCp1252NullTerminated((String) writeValue, buffer);
                        continue;
                    }
                    if (s.equalsIgnoreCase("strc")) {
                        BufferMethods.writeStringCp1252NullCircumfixed((String) writeValue, buffer);
                        continue;
                    }
                    BufferMethods.writeValue(s, (Integer) writeValue, buffer);
                }
            }

            Field PACKETWRITER = getPacketWriterField();
            PACKETWRITER.setAccessible(true);
            try {
                addNode(PACKETWRITER.get(null), packetBufferNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            PACKETWRITER.setAccessible(false);
        }
    }

    public void addNode(Object packetWriter, Object packetBufferNode) {
        if (methods.isUsingClientAddNode()) {
            try {
                Method addNode = null;
                long garbageValue = Math.abs(Long.parseLong(ObfuscatedNames.addNodeGarbageValue));
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
                    //System.out.println("addnode: "+addNode);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, Integer.parseInt(ObfuscatedNames.addNodeGarbageValue));
                }
                if (addNode != null) {
                    addNode.setAccessible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Method addNode = methods.getAddNodeMethod();
                addNode.setAccessible(true);
                if (addNode.getParameterCount() == 2) {
                    addNode.invoke(null, packetWriter, packetBufferNode);
                } else {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     private Field fetchPacketField(String name) {
        try {
            Class<?> ClientPacket = getClientPacketClass();
            return ClientPacket.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}
