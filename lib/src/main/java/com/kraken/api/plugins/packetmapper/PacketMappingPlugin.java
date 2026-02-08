package com.kraken.api.plugins.packetmapper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

/**
 * RuneLite plugin for packet mapping tool
 * 
 * Usage:
 * 1. Start the plugin from the plugin list
 * 2. Click the packet mapping icon in the sidebar
 * 3. Click "Start Monitoring"
 * 4. Perform actions in game (click objects, NPCs, items, etc.)
 * 5. Click "Stop Monitoring" when done
 * 6. Select packets from the dropdown to view their mappings
 * 7. Export mappings to a file or copy to clipboard
 * 
 * The tool will correlate your in-game actions with the packets being sent
 * and generate the obfuscated packet mappings automatically.
 */
@Slf4j
@PluginDescriptor(
    name = "Packet Mapper",
    description = "Maps OSRS packet structures by analyzing outgoing packets",
    tags = {"packet", "mapping", "debug", "development"}
)
public class PacketMappingPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventBus eventBus;

    @Inject
    private PacketMappingTool mappingTool;

    @Inject
    private PacketQueueMonitor queueMonitor;

    @Inject
    private PacketMappingPanel panel;

    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        log.info("Packet Mapping Plugin started");

        // Register the mapping tool with the event bus so it can listen to menu events
        eventBus.register(mappingTool);

        // Create and add the sidebar panel
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/packet_icon.png");
        
        navButton = NavigationButton.builder()
            .tooltip("Packet Mapper")
            .icon(icon)
            .priority(10)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
        
        log.info("Packet Mapper UI added to sidebar");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Packet Mapping Plugin stopped");

        // Stop monitoring if active
        queueMonitor.stopMonitoring();

        // Unregister from event bus
        eventBus.unregister(mappingTool);

        // Remove sidebar panel
        clientToolbar.removeNavigation(navButton);
    }

    @Provides
    PacketMappingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PacketMappingConfig.class);
    }
}
