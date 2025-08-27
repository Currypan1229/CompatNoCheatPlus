package me.asofold.bpl.cncp.proxy;

import com.velocitypowered.api.proxy.ProxyServer;

public final class WVeclotityCompatNoCheatPlus extends WProxyCompatNoCheatPlus {
    private final ProxyServer server;

    public WVeclotityCompatNoCheatPlus(final ProxyServer server) {
        super("geyser", "floodgate");
        this.server = server;
    }

    @Override
    public boolean isPluginLoaded(final String plugin) {
        return this.server.getPluginManager().getPlugin(plugin).isPresent();
    }
}
