package me.asofold.bpl.cncp;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.compat.Folia;
import fr.neatmonster.nocheatplus.components.registry.feature.IDisableListener;
import fr.neatmonster.nocheatplus.hooks.NCPHook;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.asofold.bpl.cncp.bedrock.BedrockPlayerListener;
import me.asofold.bpl.cncp.config.Settings;
import me.asofold.bpl.cncp.config.compatlayer.CompatConfig;
import me.asofold.bpl.cncp.config.compatlayer.NewConfig;
import me.asofold.bpl.cncp.hooks.Hook;
import me.asofold.bpl.cncp.hooks.generic.ConfigurableHook;
import me.asofold.bpl.cncp.hooks.generic.HookBlockBreak;
import me.asofold.bpl.cncp.hooks.generic.HookBlockPlace;
import me.asofold.bpl.cncp.hooks.generic.HookEntityDamageByEntity;
import me.asofold.bpl.cncp.hooks.generic.HookInstaBreak;
import me.asofold.bpl.cncp.hooks.generic.HookPlayerClass;
import me.asofold.bpl.cncp.hooks.generic.HookPlayerInteract;
import me.asofold.bpl.cncp.utils.TickTask2;
import me.asofold.bpl.cncp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Quick attempt to provide compatibility to NoCheatPlus (by NeatMonster) for some other plugins that change the
 * vanilla game mechanichs, for instance by fast block breaking.
 *
 * @author mc_dev
 */
public class CompatNoCheatPlus extends JavaPlugin implements Listener {

    /** Hooks registered with cncp */
    private static final Set<Hook> registeredHooks = new HashSet<>();
    private static CompatNoCheatPlus instance = null;
    /**
     * Flag if plugin is enabled.
     */
    private static boolean enabled = false;
    private final Settings settings = new Settings();
    private final List<Hook> builtinHooks = new LinkedList<>();
    private boolean proxy;

    /**
     * Experimental: static method to enable this plugin, only enables if it is not already enabled.
     *
     * @return
     */
    public static boolean enableCncp() {
        if (enabled) return true;
        return enablePlugin("CompatNoCheatPlus");
    }

    /**
     * Static method to enable a plugin (might also be useful for hooks).
     *
     * @param plgName
     * @return
     */
    public static boolean enablePlugin(final String plgName) {
        final PluginManager pm = Bukkit.getPluginManager();
        final Plugin plugin = pm.getPlugin(plgName);
        if (plugin == null) return false;
        if (pm.isPluginEnabled(plugin)) return true;
        pm.enablePlugin(plugin);
        return true;
    }

    /**
     * Get the plugin instance.
     *
     * @return
     */
    public static CompatNoCheatPlus getInstance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onPluginEnable(final PluginEnableEvent event) {
        final Plugin plugin = event.getPlugin();
        if (!plugin.getName().equals("NoCheatPlus")) {
            return;
        }
        // Register to remove hooks when NCP is disabling.
        // Remove all registered cncp hooks:
        NCPAPIProvider.getNoCheatPlusAPI().addComponent((IDisableListener) this::unregisterNCPHooks);
        if (registeredHooks.isEmpty()) {
            return;
        }
        this.registerHooks();
    }

    private int registerHooks() {
        int n = 0;
        for (final Hook hook : registeredHooks) {
            // TODO: try catch
            final NCPHook ncpHook = hook.getNCPHook();
            if (ncpHook == null) continue;
            NCPHookManager.addHook(hook.getCheckTypes(), ncpHook);
            n++;
        }
        this.getLogger().info("[CompatNoCheatPlus] Added " + n + " registered hooks to NoCheatPlus.");
        return n;
    }

    protected int unregisterNCPHooks() {
        // TODO: Clear list here !? Currently done externally...
        int n = 0;
        for (final Hook hook : registeredHooks) {
            String hookDescr = null;
            try {
                final NCPHook ncpHook = hook.getNCPHook();
                if (ncpHook != null) {
                    hookDescr = ncpHook.getHookName() + ": " + ncpHook.getHookVersion();
                    NCPHookManager.removeHook(ncpHook);
                    n++;
                }
            } catch (final Throwable e) {
                if (hookDescr != null) {
                    // Some error with removing a hook.
                    this.getLogger().log(Level.WARNING, "Failed to unregister hook: " + hookDescr, e);
                }
            }
        }
        this.getLogger().info("[CompatNoCheatPlus] Removed " + n + " registered hooks from NoCheatPlus.");
        registeredHooks.clear();
        return n;
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command,
     *  java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label,
                             final String[] args) {
        // Permission has already been checked.
        this.sendInfo(sender);
        return true;
    }

    @Override
    public void onDisable() {
        this.unregisterNCPHooks(); // Just in case.
        enabled = false;
        instance = null; // Set last.
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "cncp:geyser");
        super.onDisable();
    }

    @Override
    public void onEnable() {
        enabled = false; // make sure
        instance = this;
        // (no cleanup)

        // Settings:
        this.settings.clear();
        this.setupBuiltinHooks();
        this.loadSettings();
        // Register own listener:
        final PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new BedrockPlayerListener(), this);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "cncp:geyser", new BedrockPlayerListener());
        try {
            this.proxy = this.getServer().spigot().getConfig().getBoolean("settings.bungeecord");

            // sometimes not work, try the hard way
            if (!this.proxy) {
                this.proxy = YamlConfiguration.loadConfiguration(new File("spigot.yml"))
                        .getBoolean("settings.bungeecord");

                final File paperCfg = new File("config/paper-global.yml");
                if (!this.proxy && paperCfg.exists()) {
                    this.proxy = YamlConfiguration.loadConfiguration(paperCfg)
                            .getBoolean("proxies.velocity.enabled");
                }
            }
        } catch (final Throwable t) {
            this.proxy = false;
        }
        super.onEnable();

        // Add  Hooks:
        this.addAvailableHooks(); // add before enable is set to not yet register listeners.
        enabled = true;

        // register all listeners:
        for (final Hook hook : registeredHooks) {
            registerListeners(hook);
        }

        // Start ticktask 2
        Folia.runSyncRepatingTask(this, (arg) -> new TickTask2().run(), 1, 1);

        // Check for the NoCheatPlus plugin.
        final Plugin plugin = pm.getPlugin("NoCheatPlus");
        if (plugin == null) {
            this.getLogger().severe("[CompatNoCheatPlus] The NoCheatPlus plugin is not present.");
        } else if (plugin.isEnabled()) {
            this.getLogger().severe("[CompatNoCheatPlus] The NoCheatPlus plugin already is enabled, this might break " +
                    "several hooks.");
        }

        // Finished.
        this.getLogger().info(this.getDescription().getFullName() + " is enabled. Some hooks might get registered " +
                "with NoCheatPlus later on.");
    }

    /**
     * Called before loading settings, adds available hooks into a list, so they will be able to read config.
     */
    private void setupBuiltinHooks() {
        this.builtinHooks.clear();
        // Might-fail hooks:
        // Set speed
        try {
            this.builtinHooks.add(new me.asofold.bpl.cncp.hooks.generic.HookSetSpeed());
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        // Citizens 2
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            this.builtinHooks.add(new me.asofold.bpl.cncp.hooks.citizens2.HookCitizens2());
        }
        // mcMMO
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            this.builtinHooks.add(new me.asofold.bpl.cncp.hooks.mcmmo.HookmcMMO());
        }
        // GravityTubes
        if (Bukkit.getPluginManager().getPlugin("GravityTubes") != null) {
            this.builtinHooks.add(new me.asofold.bpl.cncp.hooks.GravityTubes.HookGravityTubes());
        }
        // CMI
        if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
            this.builtinHooks.add(new me.asofold.bpl.cncp.hooks.CMI.HookCMI());
        }
//        // MagicSpells
//        if (Bukkit.getPluginManager().getPlugin("MagicSpells") != null) {
//            builtinHooks.add(new me.asofold.bpl.cncp.hooks.magicspells.HookMagicSpells());
//        }
        // Simple generic hooks
        Collections.addAll(this.builtinHooks, new HookPlayerClass(),
                new HookBlockBreak(),
                new HookBlockPlace(),
                new HookInstaBreak(),
                new HookEntityDamageByEntity(),
                new HookPlayerInteract());
    }

    /**
     * Add standard hooks if enabled.
     */
    private void addAvailableHooks() {

        // Add built in hooks:
        for (final Hook hook : this.builtinHooks) {
            boolean add = true;
            if (hook instanceof ConfigurableHook) {
                if (!((ConfigurableHook) hook).isEnabled()) add = false;
            }
            if (add) {
                try {
                    addHook(hook);
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * API to add a hook. Adds the hook AND registers listeners if enabled. Also respects the configuration for
     * preventing hooks.<br>
     * If you want to not register the listeners use NCPHookManager.
     *
     * @param hook
     * @return
     */
    public static boolean addHook(final Hook hook) {
        if (Settings.preventAddHooks.contains(hook.getHookName())) {
            Bukkit.getLogger().info("[CompatNoCheatPlus] Prevented adding hook: " + hook.getHookName() + " / " + hook.getHookVersion());
            return false;
        }
        registeredHooks.add(hook);
        if (enabled) registerListeners(hook);
        final boolean added = checkAddNCPHook(hook); // Add if plugin is present, otherwise queue for adding.
        Bukkit.getLogger().info("[CompatNoCheatPlus] Registered hook" + (added ? "" : "(NCPHook might get added " +
                "later)") + ": " + hook.getHookName() + " / " + hook.getHookVersion());
        return true;
    }

    /**
     * If already added to NCP
     *
     * @param hook
     * @return
     */
    private static boolean checkAddNCPHook(final Hook hook) {
        final PluginManager pm = Bukkit.getPluginManager();
        final Plugin plugin = pm.getPlugin("NoCheatPlus");
        if (plugin == null || !pm.isPluginEnabled(plugin))
            return false;
        final NCPHook ncpHook = hook.getNCPHook();
        if (ncpHook != null)
            NCPHookManager.addHook(hook.getCheckTypes(), ncpHook);
        return true;
    }

    /**
     * Conveniently register the listeners, do not use if you add/added the hook with addHook.
     *
     * @param hook
     * @return
     */
    public static boolean registerListeners(final Hook hook) {
        if (!enabled) return false;
        final Listener[] listeners = hook.getListeners();
        if (listeners != null) {
            // attempt to register events:
            final PluginManager pm = Bukkit.getPluginManager();
            final Plugin plg = pm.getPlugin("CompatNoCheatPlus");
            if (plg == null) return false;
            for (final Listener listener : listeners) {
                pm.registerEvents(listener, plg);
            }
        }
        return true;
    }

    public boolean loadSettings() {
        final Set<String> oldForceEnableLater = new LinkedHashSet<>(this.settings.forceEnableLater);
        // Read and apply config to settings:
        final File file = new File(this.getDataFolder(), "cncp.yml");
        final CompatConfig cfg = new NewConfig(file);
        cfg.load();
        boolean changed = Settings.addDefaults(cfg);
        // General settings:
        this.settings.fromConfig(cfg);
        // Settings for builtin hooks:
        for (final Hook hook : this.builtinHooks) {
            if (hook instanceof ConfigurableHook) {
                try {
                    final ConfigurableHook cfgHook = (ConfigurableHook) hook;
                    if (cfgHook.updateConfig(cfg, "hooks.")) changed = true;
                    cfgHook.applyConfig(cfg, "hooks.");
                } catch (final Throwable t) {
                    this.getLogger().severe("[CompatNoCheatPlus] Hook failed to process config (" + hook.getHookName() + " / " + hook.getHookVersion() + "): " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        }
        // save back config if changed:
        if (changed) cfg.save();


        // Re-enable plugins that were not yet on the list:
        final Server server = this.getServer();
        final Logger logger = server.getLogger();
        for (final String plgName : this.settings.loadPlugins) {
            try {
                if (CompatNoCheatPlus.enablePlugin(plgName)) {
                    System.out.println("[CompatNoCheatPlus] Ensured that the following plugin is enabled: " + plgName);
                }
            } catch (final Throwable t) {
                logger.severe("[CompatNoCheatPlus] Failed to enable the plugin: " + plgName);
                logger.severe(Utils.toString(t));
            }
        }
        final BukkitScheduler sched = server.getScheduler();
        for (final String plgName : this.settings.forceEnableLater) {
            if (!oldForceEnableLater.remove(plgName)) oldForceEnableLater.add(plgName);
        }
        if (!oldForceEnableLater.isEmpty()) {
            System.out.println("[CompatNoCheatPlus] Schedule task to re-enable plugins later...");
            sched.scheduleSyncDelayedTask(this, () -> {
                // (Later maybe re-enabling this plugin could be added.)
                // TODO: log levels !
                for (final String plgName : oldForceEnableLater) {
                    try {
                        if (disablePlugin(plgName)) {
                            if (enablePlugin(plgName))
                                System.out.println("[CompatNoCheatPlus] Re-enabled plugin: " + plgName);
                            else System.out.println("[CompatNoCheatPlus] Could not re-enable plugin: " + plgName);
                        } else {
                            System.out.println("[CompatNoCheatPlus] Could not disable plugin (already disabled?): " + plgName);
                        }
                    } catch (final Throwable t) {
                        // TODO: maybe log ?
                    }
                }
            });
        }

        return true;
    }

    /**
     * Static method to disable a plugin (might also be useful for hooks).
     *
     * @param plgName
     * @return
     */
    public static boolean disablePlugin(final String plgName) {
        final PluginManager pm = Bukkit.getPluginManager();
        final Plugin plugin = pm.getPlugin(plgName);
        if (plugin == null) return false;
        if (!pm.isPluginEnabled(plugin)) return true;
        pm.disablePlugin(plugin);
        return true;
    }

    /**
     * Send general version and hooks info.
     *
     * @param sender
     */
    private void sendInfo(final CommandSender sender) {
        final List<String> infos = new LinkedList<>();
        infos.add("---- Version infomation ----");
        // Server
        infos.add("#### Server ####");
        infos.add(this.getServer().getVersion());
        // Core plugins (NCP + cncp)
        infos.add("#### Core plugins ####");
        infos.add(this.getDescription().getFullName());
        String temp = this.getOtherVersion("NoCheatPlus");
        infos.add(temp.isEmpty() ? "NoCheatPlus is missing or not yet enabled." : temp);
        infos.add("#### Typical plugin dependencies ####");
        for (final String pluginName : new String[]{
                "mcMMO", "Citizens", "MachinaCraft", "MagicSpells", "ViaVersion", "ProtocolSupport", "GravityTubes",
                "Geyser-Spigot", "floodgate", "CMI", "Geyser-BungeeCord"
        }) {
            temp = this.getOtherVersion(pluginName);
            if (!temp.isEmpty()) infos.add(temp);
        }
        // Hooks
        infos.add("#### Registered hooks (cncp) ###");
        for (final Hook hook : registeredHooks) {
            temp = hook.getHookName() + ": " + hook.getHookVersion();
            if (hook instanceof ConfigurableHook) {
                temp += ((ConfigurableHook) hook).isEnabled() ? " (enabled)" : " (disabled)";
            }
            infos.add(temp);
        }
        // TODO: Registered hooks (ncp) ?
        infos.add("#### Registered hooks (ncp) ####");
        for (final NCPHook hook : NCPHookManager.getAllHooks()) {
            infos.add(hook.getHookName() + ": " + hook.getHookVersion());
        }
        final String[] a = new String[infos.size()];
        infos.toArray(a);
        sender.sendMessage(a);
    }

    /**
     * @param pluginName Empty string or "name: version".
     */
    private String getOtherVersion(final String pluginName) {
        final Plugin plg = this.getServer().getPluginManager().getPlugin(pluginName);
        if (plg == null) return "";
        final PluginDescriptionFile pdf = plg.getDescription();
        return pdf.getFullName();
    }

    public Settings getSettings() {
        return this.settings;
    }

    public boolean isProxyEnabled() {
        return this.proxy;
    }
}
