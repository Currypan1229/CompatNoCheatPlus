package me.asofold.bpl.cncp.bedrock;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import me.asofold.bpl.cncp.BukkitCompatNoCheatPlus;
import me.asofold.bpl.cncp.config.Settings;
import me.asofold.bpl.cncp.proxy.WProxyCompatNoCheatPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.api.FloodgateApi;

public class BedrockPlayerListener implements Listener, PluginMessageListener {

    private final Settings settings = BukkitCompatNoCheatPlus.getInstance().getSettings();
    private Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
    private Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (this.floodgate != null && this.floodgate.isEnabled()) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                this.processExemption(player);
            }
        } else if (this.geyser != null && this.geyser.isEnabled()) {
            try {
                final GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(player.getUniqueId());
                if (session != null) this.processExemption(player);
            } catch (final NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private void processExemption(final Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData != null) {
            for (final CheckType check : this.settings.extemptChecks) pData.exempt(check);
            pData.setBedrockPlayer(true);
        }
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] data) {
        if (BukkitCompatNoCheatPlus.getInstance().isProxyEnabled() && channel.equals(WProxyCompatNoCheatPlus.IDENTIFIER)) {
            this.geyser = null;
            this.floodgate = null;
            final ByteArrayDataInput input = ByteStreams.newDataInput(data);
            final String playerName = input.readUTF();
            this.processExemption(playerName);
        }
    }

    private void processExemption(final String playername) {
        final IPlayerData pData = DataManager.getPlayerData(playername);
        if (pData != null) {
            for (final CheckType check : this.settings.extemptChecks) pData.exempt(check);
            pData.setBedrockPlayer(true);
        }
    }
}
