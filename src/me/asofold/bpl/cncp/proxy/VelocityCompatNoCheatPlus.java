package me.asofold.bpl.cncp.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.asofold.bpl.cncp.utils.GeyserUtils;
import org.slf4j.Logger;

@Plugin(
        id = "compatnocheatplus", name = "CompatNoCheatPlus", version = "6.7.0-SNAPSHOT",
        url = "https://github.com/Currypan1229/CompatNoCheatPlus", authors = {"asofold", "xaw3ep", "Currypan1229"},
        dependencies = {
                @Dependency(id = "floodgate"),
                @Dependency(id = "geyser")
        }
)
public final class VelocityCompatNoCheatPlus {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("cncp:geyser");

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocityCompatNoCheatPlus(final ProxyServer server, final Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onEnable(final ProxyInitializeEvent event) {
        final boolean geyser = checkGeyser();
        final boolean floodgate = checkFloodgate();
        GeyserUtils.init(floodgate, geyser);

        this.logger.info("Registering listeners");
        this.server.getChannelRegistrar().register(VelocityCompatNoCheatPlus.IDENTIFIER);
        this.logger.info("cncp Velocity mode with Geyser : " + geyser + ", Floodgate : " + floodgate);
    }

    @Subscribe
    public void onMessageReceive(PluginMessageEvent event) {
        if (event.getIdentifier().equals(VelocityCompatNoCheatPlus.IDENTIFIER)) {
            // Message sent from client, cancel it
            if (event.getSource() instanceof Player) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }
        }
    }

    private boolean checkFloodgate() {
        return this.server.getPluginManager().getPlugin("floodgate") != null;
    }

    private boolean checkGeyser() {
        return this.server.getPluginManager().getPlugin("geyser") != null;
    }

    @Subscribe
    public void onChangeServer(ServerConnectedEvent event) {
        final Player player = event.getPlayer();
        final RegisteredServer server = event.getServer();

        if (!GeyserUtils.isBedrockPlayer(player.getUniqueId())) return;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        try {
            dataOutputStream.writeUTF(player.getUsername());
        } catch (IOException e) {
            e.printStackTrace();
        }

        final TimerTask t = new TimerTask() {
            @Override
            public void run() {
                server.sendPluginMessage(VelocityCompatNoCheatPlus.IDENTIFIER, outputStream.toByteArray());
            }
        };
        final Timer timer = new Timer(false);
        timer.schedule(t, 0, 1000);
    }
}
