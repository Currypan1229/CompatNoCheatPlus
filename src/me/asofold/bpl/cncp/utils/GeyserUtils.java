package me.asofold.bpl.cncp.utils;

import java.util.UUID;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.api.FloodgateApi;

public class GeyserUtils {
    private static boolean floodgate;
    private static boolean geyser;

    public static void init(boolean floodgate, boolean geyser) {
        GeyserUtils.floodgate = floodgate;
        GeyserUtils.geyser = geyser;
    }

    public static boolean isFloodGatePlayer(final UUID player) {
        return floodgate && FloodgateApi.getInstance().isFloodgatePlayer(player);
    }

    public static boolean isGeyserPlayer(final UUID player) {
        if (!geyser) {
            return false;
        }

        try {
            return GeyserConnector.getInstance().getPlayerByUuid(player) != null;
        } catch (final NullPointerException e) {
            return false;
        }
    }

    public static boolean isBedrockPlayer(UUID player) {
        return isFloodGatePlayer(player) || isGeyserPlayer(player);
    }
}