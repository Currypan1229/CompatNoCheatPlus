package me.asofold.bpl.cncp.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public final class BungeeCompatNoCheatPlus extends Plugin implements Listener {
    private final WProxyCompatNoCheatPlus wrapper;

    public BungeeCompatNoCheatPlus() {
        this.wrapper = new WBungeeCompatNoCheatPlus();
    }

    @Override
    public void onEnable() {
        this.getLogger().info("Registering listeners");
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().registerChannel(WProxyCompatNoCheatPlus.IDENTIFIER);
        this.getLogger().info("cncp Bungee mode with Geyser : " + this.wrapper.geyser + ", Floodgate : " + this.wrapper.floodGate);
    }

    @EventHandler
    public void onMessageReceive(final PluginMessageEvent event) {
        if (event.getTag().equalsIgnoreCase(WProxyCompatNoCheatPlus.IDENTIFIER)) {
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

        if (!this.wrapper.isBedrockPlayer(player.getUniqueId())) return;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        try {
            dataOutputStream.writeUTF(player.getName());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.getProxy().getScheduler().schedule(this, () -> server.sendData(WProxyCompatNoCheatPlus.IDENTIFIER,
                        outputStream.toByteArray()), 1L
                , TimeUnit.SECONDS);
    }
}
