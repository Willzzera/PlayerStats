package com.gmail.artemis.the.gr8.playerstats.utils;

import com.gmail.artemis.the.gr8.playerstats.filehandlers.ConfigHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class OfflinePlayerHandler {

    private final ConfigHandler config;
    private static HashMap<String, UUID> offlinePlayerUUIDs;

    public OfflinePlayerHandler(ConfigHandler c) {
        config = c;
    }

    public static boolean isOfflinePlayerName(String playerName) {
        return offlinePlayerUUIDs.containsKey(playerName);
    }

    public static int getOfflinePlayerCount() throws NullPointerException {
        if (offlinePlayerUUIDs != null && offlinePlayerUUIDs.size() > 0) return offlinePlayerUUIDs.size();
        else throw new NullPointerException("No players found!");
    }

    public static ArrayList<String> getOfflinePlayerNames() {
        return new ArrayList<>(offlinePlayerUUIDs.keySet());
    }

    public void updateOfflinePlayerList() {
        updateOfflinePlayerList(config.whitelistOnly(), config.excludeBanned(), config.lastPlayedLimit());
    }

    //stores a private HashMap of all relevant offline players with keys:playerName and values:UUID
    private void updateOfflinePlayerList(boolean whitelistOnly, boolean excludeBanned, int lastPlayedLimit) {
        if (offlinePlayerUUIDs == null) offlinePlayerUUIDs = new HashMap<>();
        else if (!offlinePlayerUUIDs.isEmpty()) {
            offlinePlayerUUIDs.clear();
        }
        Arrays.stream(Bukkit.getOfflinePlayers()).filter(offlinePlayer ->
                offlinePlayer.getName() != null &&
                    (!excludeBanned || !offlinePlayer.isBanned()) &&
                    (!whitelistOnly || offlinePlayer.isWhitelisted()) &&
                    (lastPlayedLimit == 0 || UnixTimeHandler.hasPlayedSince(lastPlayedLimit, offlinePlayer.getLastPlayed())))
                .forEach(offlinePlayer -> offlinePlayerUUIDs.put((offlinePlayer.getName()), offlinePlayer.getUniqueId()));
    }

    /**
     * Uses the playerName to get the player's UUID from a private HashMap, and uses the UUID to get the corresponding OfflinePlayer Object.
     * @param playerName name of the target player
     * @return OfflinePlayer (if this player is on the list)
     */

    public static OfflinePlayer getOfflinePlayer(String playerName) {
        return Bukkit.getOfflinePlayer(offlinePlayerUUIDs.get(playerName));
    }
}
