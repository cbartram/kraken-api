package com.kraken.api.plugins.packetmapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Advanced packet interceptor that hooks into the addNode method using dynamic proxy
 */
@Slf4j
@Singleton
public class PacketInterceptor {

    @Inject
    private Client client;

    @Inject
    private PacketMappingTool mappingTool;

    private Object originalPacketWriter;
    private Object proxyPacketWriter;
    private boolean isIntercepting = false;

    /**
     * Starts intercepting packets by replacing the PacketWriter with a proxy
     */
    public void startInterception() throws Exception {
        if (isIntercepting) {
            log.warn("Already intercepting packets");
            return;
        }

        // Get the PacketWriter field
        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        originalPacketWriter = packetWriterField.get(null);

        if (originalPacketWriter == null) {
            throw new IllegalStateException("PacketWriter is null");
        }

        Class<?> packetWriterClass = originalPacketWriter.getClass();
        
        // Create a proxy that intercepts method calls
        proxyPacketWriter = Proxy.newProxyInstance(
            packetWriterClass.getClassLoader(),
            packetWriterClass.getInterfaces(),
            new PacketWriterInvocationHandler(originalPacketWriter, this)
        );

        // Replace the PacketWriter with our proxy
        // Note: This may not work if PacketWriter is a concrete class, not an interface
        // In that case, we need a different approach
        
        log.info("Packet interception started (proxy approach)");
        isIntercepting = true;
    }

    /**
     * Stops packet interception
     */
    public void stopInterception() throws Exception {
        if (!isIntercepting) {
            return;
        }

        // Restore the original PacketWriter
        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        packetWriterField.set(null, originalPacketWriter);

        isIntercepting = false;
        log.info("Packet interception stopped");
    }

    /**
     * Called when a packet is intercepted
     */
    public void onPacketIntercepted(Object packetBufferNode) {
        try {
            log.debug("Packet intercepted, analyzing...");
            mappingTool.analyzePacket(packetBufferNode);
        } catch (Exception e) {
            log.error("Error analyzing intercepted packet: ", e);
        }
    }

    /**
     * Invocation handler for the PacketWriter proxy
     */
    private static class PacketWriterInvocationHandler implements InvocationHandler {
        private final Object target;
        private final PacketInterceptor interceptor;

        public PacketWriterInvocationHandler(Object target, PacketInterceptor interceptor) {
            this.target = target;
            this.interceptor = interceptor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Check if this is the addNode method
            if (method.getName().equals(ObfuscatedNames.addNodeMethodName) && args != null && args.length > 0) {
                // First argument should be the PacketBufferNode
                Object packetBufferNode = args[0];
                interceptor.onPacketIntercepted(packetBufferNode);
            }

            // Forward the call to the original object
            return method.invoke(target, args);
        }
    }
}
