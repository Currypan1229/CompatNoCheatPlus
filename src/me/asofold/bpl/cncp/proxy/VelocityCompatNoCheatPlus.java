package me.asofold.bpl.cncp.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import net.md_5.bungee.api.plugin.Listener;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;

@Plugin(
        id = "CompatNoCheatPlus", name = "My First Plugin",
        url = "https://github.com/Currypan1229/CompatNoCheatPlus", authors = {"asofold", "xaw3ep", "Currypan1229"}
)
public class VelocityCompatNoCheatPlus implements Listener {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("cncp:geyser");
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private boolean floodgate;
    private boolean geyser;

    @Inject
    public VelocityCompatNoCheatPlus(final ProxyServer server, final Logger logger,
                                     @DataDirectory final Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

    }

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent event) {
        this.geyser = this.checkGeyser();
        this.floodgate = this.checkFloodgate();

        logger.info("Registering listeners");
        this.server.getEventManager().register(this, this);
        server.getChannelRegistrar().register(IDENTIFIER);
        logger.info("cncp Bungee mode with Geyser : " + this.geyser + ", Floodgate : " + this.floodgate);
    }

    private boolean checkFloodgate() {
        return this.server.getPluginManager().getPlugin("floodgate").isPresent();
    }

    private boolean checkGeyser() {
        return this.server.getPluginManager().getPlugin("Geyser-Velocity").isPresent();
    }

    @Subscribe
    public void onMessageReceive(final PluginMessageEvent event) {
        if (event.getIdentifier().equals(IDENTIFIER)) {
            // Message sent from clients, cancel it
            if (event.getSource() instanceof Player) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }
        }
    }

    @Subscribe
    public void onChangeServer(final ServerConnectedEvent event) {
        final Player player = event.getPlayer();
        final ServerConnection server = player.getCurrentServer().get();

        if (!this.isBedrockPlayer(player)) return;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        try {
            dataOutputStream.writeUTF(player.getGameProfile().getName());
        } catch (final IOException e) {
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

    private boolean isBedrockPlayer(final Player player) {
        if (this.floodgate) {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        }
        if (this.geyser) {
            try {
                final GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(player.getUniqueId());
                return session != null;
            } catch (final NullPointerException e) {
                return false;
            }
        }
        return false;
    }
}
