package me.asofold.bpl.cncp.proxy;

import net.md_5.bungee.api.ProxyServer;

public final class WBungeeCompatNoCheatPlus extends WProxyCompatNoCheatPlus {
    public WBungeeCompatNoCheatPlus() {
        super("Geyser-BungeeCord", "floodgate");
    }

    @Override
    public boolean isPluginLoaded(final String plugin) {
        return ProxyServer.getInstance().getPluginManager().getPlugin(plugin) != null;
    }
}
