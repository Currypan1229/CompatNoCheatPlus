package me.asofold.bpl.cncp.proxy;

import java.util.UUID;
import me.asofold.bpl.cncp.bedrock.GeyserUtils;

public abstract class WProxyCompatNoCheatPlus {
    public static final String IDENTIFIER = "cncp:geyser";

    protected boolean floodGate;
    protected boolean geyser;

    public WProxyCompatNoCheatPlus(final String geyserId, final String floodGateId) {
        this.floodGate = this.isPluginLoaded(floodGateId);
        this.geyser = this.isPluginLoaded(geyserId);
    }

    public abstract boolean isPluginLoaded(String plugin);

    public final boolean isBedrockPlayer(final UUID player) {
        if (this.floodGate) return GeyserUtils.isFloodGatePlayer(player);
        if (this.geyser) return GeyserUtils.isGeyserPlayer(player);
        return false;
    }
}
