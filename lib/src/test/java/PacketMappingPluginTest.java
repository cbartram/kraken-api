import com.kraken.api.plugins.packetmapper.PacketMappingPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PacketMappingPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(PacketMappingPlugin.class);
        RuneLite.main(args);
    }
}
