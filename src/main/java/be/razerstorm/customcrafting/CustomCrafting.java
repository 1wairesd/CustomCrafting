package be.razerstorm.customcrafting;

import be.razerstorm.customcrafting.commands.CustomCraftingCommand;
import be.razerstorm.customcrafting.listeners.AdminJoinListener;
import be.razerstorm.customcrafting.managers.MessageManager;
import be.razerstorm.customcrafting.managers.RecipeManager;
import be.razerstorm.customcrafting.utils.GUIHolder;
import be.razerstorm.customcrafting.utils.Metrics;
import be.razerstorm.customcrafting.utils.UpdateChecker;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomCrafting extends JavaPlugin {

    private static @Getter CustomCrafting instance;
    private @Getter boolean lastVersion;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        new Metrics(this, 19927);

        MessageManager.getInstance().loadMessages();
        GUIHolder.init(this);

        if (getConfig().getBoolean("update-checker")) UpdateChecker.getInstance().checkForUpdate();

        CustomCraftingCommand commandExecutor = new CustomCraftingCommand();
        PluginCommand command = getCommand("customcrafting");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new AdminJoinListener(), this);

        RecipeManager.getInstance().loadRecipes();
    }
}