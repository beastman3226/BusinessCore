package me.beastman3226.bc;

import java.util.HashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.beastman3226.bc.business.BusinessManager;
import me.beastman3226.bc.commands.BusinessCommand;
import me.beastman3226.bc.commands.JobCommand;
import me.beastman3226.bc.data.file.FileManager;
import me.beastman3226.bc.job.JobManager;
import me.beastman3226.bc.listener.BusinessListener;
import me.beastman3226.bc.listener.JobListener;
import me.beastman3226.bc.listener.PlayerListener;
import me.beastman3226.bc.player.EmployeeManager;
import me.beastman3226.bc.util.Settings;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

/**
 *
 * @author beastman3226
 */
public class BusinessCore extends JavaPlugin {

    private static BusinessCore instance;
    private Economy eco;
    private Chat chat;
    private FileManager businessFileManager, jobFileManager, employeeFileManager;
    private Settings settings;

    HashMap<String, String> hm = new HashMap<String, String>();


    @Override
    public void onEnable() {
        instance = this;
        this.getLogger().info("Loaded Economy: " + setupEconomy());
        this.getLogger().info("Loaded Chat: " + setupChat());
        this.saveDefaultConfig();
        businessFileManager = new FileManager("business.yml");
        employeeFileManager = new FileManager("employee.yml");
        jobFileManager = new FileManager("job.yml");
        registerListeners();
        registerCommands();
        //settings and configs
        settings = new Settings(this.getConfig());
        if(settings.verboseLogging()) {
            getLogger().info("(================Business Core Settings================)");
            getLogger().info("Businesses:");
            getLogger().info("\tRequire Payment: " + settings.isPayRequiredBusiness());
            getLogger().info("\tRequired Payment: " + settings.getPayRequiredBusiness());
            getLogger().info("\tStarting Balance: " + settings.getStartingBusinessBalance());
            getLogger().info("Jobs:");
            getLogger().info("\tRequire Payment: " + settings.isPayRequiredJob());
            getLogger().info("\tRequired Payment: " + settings.getPayRequiredJob());
            getLogger().info("\tMinimum Payment: " + settings.getMinimumJobPayment());
        }
        this.getLogger().info("Loading businesses...");
        BusinessManager.createBusinesses();
        this.getLogger().info("Loading employees...");
        EmployeeManager.loadEmployees();
        this.getLogger().info("Loading jobs...");
        JobManager.loadJobs();
        getLogger().info("Do /businesscore for information about this plugin");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Saving businesses...");
        BusinessManager.saveBusinesses();
        this.getLogger().info("Saving employees...");
        EmployeeManager.saveEmployees();
        this.getLogger().info("Saving jobs...");
        JobManager.saveJobs();
        this.reloadConfig();
        this.saveConfig();
    }

    /**
     * A method to condense the clutter inside the onEnable method.
     */    
    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new BusinessListener(), this);
        getServer().getPluginManager().registerEvents(new JobListener(), this);
    }

    /**
     * Method to avoid clutter inside onEnable for commands.
     */
    public void registerCommands() {
        new BusinessCommand();
        new JobCommand();
    }

    public boolean setupEconomy() {
        Plugin p = this.getServer().getPluginManager().getPlugin("Vault");
        if (p == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = null;
        rsp = (RegisteredServiceProvider<Economy>) this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            this.getLogger().severe("Economy plugin not detected");
            rsp = (RegisteredServiceProvider<Economy>) this.getServer().getServicesManager().getRegistration(Economy.class);
        }
        if (rsp == null) {
            return false;
        }
        eco = rsp.getProvider();
        return eco != null;

    }

    private boolean setupChat() {
        Plugin p = this.getServer().getPluginManager().getPlugin("Vault");
        if (p == null) {
            return false;
        }
        RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> rsp = null;
        rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            this.getLogger().warning("Chat plugin not detected");
            return false;
        }
        chat = rsp.getProvider();
        return chat != null;
    }

    public Economy getEconomy() {
		return this.eco;
    }

    public FileManager getBusinessFileManager() {
        return this.businessFileManager;
    }

    public FileManager getEmployeeFileManager() {
        return this.employeeFileManager;
    }

    public FileManager getJobFileManager() {
        return this.jobFileManager;
    }

    public static BusinessCore getInstance() {
        if(instance == null)
            return null;
        return instance;
    }
}
