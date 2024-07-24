package me.asofold.bpl.cncp.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.api.FloodgateApi;

public class BungeeCompatNoCheatPlus extends Plugin implements Listener {
    private boolean floodgate;
    private boolean geyser;

    @Override
    public void onEnable() {
        this.geyser = this.checkGeyser();
        this.floodgate = this.checkFloodgate();
        this.getLogger().info("Registering listeners");
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().registerChannel("cncp:geyser");
        this.getLogger().info("cncp Bungee mode with Geyser : " + this.geyser + ", Floodgate : " + this.floodgate);
    }

    private boolean checkFloodgate() {
        return ProxyServer.getInstance().getPluginManager().getPlugin("floodgate") != null;
    }

    private boolean checkGeyser() {
        return ProxyServer.getInstance().getPluginManager().getPlugin("Geyser-BungeeCord") != null;
    }

    @EventHandler
    public void onMessageReceive(final PluginMessageEvent event) {
        if (event.getTag().equalsIgnoreCase("cncp:geyser")) {
            // Message sent from client, cancel it
            if (event.getSender() instanceof ProxiedPlayer) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChangeServer(final ServerSwitchEvent event) {
        final ProxiedPlayer player = event.getPlayer();
        final Server server = player.getServer();

        if (!this.isBedrockPlayer(player)) return;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        try {
            dataOutputStream.writeUTF(player.getName());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.getProxy().getScheduler().schedule(this, () -> server.sendData("cncp:geyser",
                        outputStream.toByteArray()), 1L
                , TimeUnit.SECONDS);
    }

    private boolean isBedrockPlayer(final ProxiedPlayer player) {
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
