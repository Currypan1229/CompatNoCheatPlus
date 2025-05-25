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
import me.asofold.bpl.cncp.utils.GeyserUtils;

public class BungeeCompatNoCheatPlus extends Plugin implements Listener {
    @Override
    public void onEnable() {
        final boolean geyser = checkGeyser();
        final boolean floodgate = checkFloodgate();
        GeyserUtils.init(floodgate, geyser);

        getLogger().info("Registering listeners");
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("cncp:geyser");
        getLogger().info("cncp Bungee mode with Geyser : " + geyser + ", Floodgate : " + floodgate);
    }

    @EventHandler
    public void onMessageReceive(PluginMessageEvent event) {
        if (event.getTag().equalsIgnoreCase("cncp:geyser")) {
            // Message sent from client, cancel it
            if (event.getSender() instanceof ProxiedPlayer) {
                event.setCancelled(true);
            }
        }
    }

    private boolean checkFloodgate() {
        return ProxyServer.getInstance().getPluginManager().getPlugin("floodgate") != null;
    }

    private boolean checkGeyser() {
        return ProxyServer.getInstance().getPluginManager().getPlugin("Geyser-BungeeCord") != null;
    }

    @EventHandler
    public void onChangeServer(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        Server server = player.getServer();

        if (!GeyserUtils.isBedrockPlayer(player.getUniqueId())) return;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        try {
            dataOutputStream.writeUTF(player.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        getProxy().getScheduler().schedule(this, () -> server.sendData("cncp:geyser", outputStream.toByteArray()), 1L, TimeUnit.SECONDS);
    }
}
