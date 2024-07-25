package me.asofold.bpl.cncp.bedrock;

import java.util.UUID;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.floodgate.api.FloodgateApi;

public class GeyserUtils {
    public static boolean isFloodGatePlayer(final UUID player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player);
    }

    public static boolean isGeyserPlayer(final UUID player) {
        try {
            final GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(player);
            return session != null;
        } catch (final NullPointerException e) {
            return false;
        }
    }
}
