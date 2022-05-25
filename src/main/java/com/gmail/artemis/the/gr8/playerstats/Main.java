package com.gmail.artemis.the.gr8.playerstats;

import com.gmail.artemis.the.gr8.playerstats.commands.ReloadCommand;
import com.gmail.artemis.the.gr8.playerstats.commands.StatCommand;
import com.gmail.artemis.the.gr8.playerstats.commands.TabCompleter;
import com.gmail.artemis.the.gr8.playerstats.filehandlers.ConfigHandler;
import com.gmail.artemis.the.gr8.playerstats.listeners.JoinListener;
import com.gmail.artemis.the.gr8.playerstats.utils.OfflinePlayerHandler;
import com.gmail.artemis.the.gr8.playerstats.utils.OutputFormatter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public class Main extends JavaPlugin {

    private static boolean enableHexColors;
    private BukkitAudiences adventure;

    public @NotNull BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    @Override
    public void onEnable() {
        long time = System.currentTimeMillis();

        //initialize the Adventure library
        adventure = BukkitAudiences.create(this);

        //check if Spigot ChatColors can be used, and prepare accordingly
        try {
            Class.forName("net.md_5.bungee.api.ChatColor");
            enableHexColors = true;
            this.getLogger().info("Hex Color support enabled!");
        }
        catch (ClassNotFoundException e) {
            enableHexColors = false;
            this.getLogger().info("Hex Colors are not supported for this server type, proceeding with default Chat Colors...");
        }

        //get instances of the classes that should be initialized
        ConfigHandler config = new ConfigHandler(this);
        OutputFormatter outputFormatter = new OutputFormatter(config, enableHexColors);
        OfflinePlayerHandler offlinePlayerHandler = new OfflinePlayerHandler(config);
        getLogger().info("Amount of offline players: " + offlinePlayerHandler.getOfflinePlayerCount());

        //register the commands
        PluginCommand statcmd = this.getCommand("statistic");
        if (statcmd != null) {
            statcmd.setExecutor(new StatCommand(config, offlinePlayerHandler, outputFormatter, this));
            statcmd.setTabCompleter(new TabCompleter(offlinePlayerHandler));
        }
        PluginCommand reloadcmd = this.getCommand("statisticreload");
        if (reloadcmd != null) reloadcmd.setExecutor(new ReloadCommand(adventure(), config, offlinePlayerHandler, outputFormatter, this));

        //register the listener
        Bukkit.getPluginManager().registerEvents(new JoinListener(offlinePlayerHandler), this);
        logTimeTaken("Time", "taken", time);
        this.getLogger().info("Enabled PlayerStats!");
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }

        this.getLogger().info("Disabled PlayerStats!");
    }

    public static boolean hexEnabled() {
        return enableHexColors;
    }

    public long logTimeTaken(String className, String methodName, long previousTime) {
        getLogger().info(className + " " + methodName + ": " + (System.currentTimeMillis() - previousTime));
        return System.currentTimeMillis();
    }
}
