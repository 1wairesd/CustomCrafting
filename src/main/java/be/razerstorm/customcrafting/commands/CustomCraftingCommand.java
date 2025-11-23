package be.razerstorm.customcrafting.commands;

import be.razerstorm.customcrafting.CustomCrafting;
import be.razerstorm.customcrafting.enums.RecipeType;
import be.razerstorm.customcrafting.inventories.ManageRecipeMenu;
import be.razerstorm.customcrafting.inventories.ViewRecipeMenu;
import be.razerstorm.customcrafting.managers.MessageManager;
import be.razerstorm.customcrafting.managers.RecipeManager;
import be.razerstorm.customcrafting.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomCraftingCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customcrafting.command")) {
            sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(sender, args);
            case "edit":
                return handleEdit(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "list":
                return handleList(sender);
            case "recipes":
                return handleRecipes(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageManager.getInstance().getMessageList("commands.help").forEach(sender::sendMessage);
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        MessageManager msg = MessageManager.getInstance();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getMessage("only-players"));
            return true;
        }

        if (!sender.hasPermission("customcrafting.command.create")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(msg.getMessage("commands.create.usage"));
            return true;
        }

        Player player = (Player) sender;
        String typeStr = args[1].toUpperCase();
        String recipeName = args[2];

        if (!RecipeType.typeExists(typeStr)) {
            player.sendMessage(msg.getMessage("commands.create.invalid-type"));
            return true;
        }

        RecipeType type = RecipeType.valueOf(typeStr);
        if (RecipeManager.getInstance().recipeExists(recipeName)) {
            player.sendMessage(msg.getMessage("commands.create.already-exists"));
            return true;
        }

        switch (type) {
            case CRAFTING:
                new ManageRecipeMenu(player, type, recipeName, false).openMenu();
                break;
            case FURNACE:
                String[] furnaceArgs = Arrays.copyOfRange(args, 3, args.length);
                if (furnaceArgs.length == 2) {
                    if (Utils.isInteger(furnaceArgs[0]) && Utils.isInteger(furnaceArgs[1])) {
                        new ManageRecipeMenu(player, type, recipeName, false, Integer.parseInt(furnaceArgs[0]), Integer.parseInt(furnaceArgs[1]) * 20).openMenu();
                        return true;
                    }
                    player.sendMessage(msg.getMessage("commands.create.invalid-args"));
                } else if (furnaceArgs.length == 1) {
                    if (Utils.isInteger(furnaceArgs[0])) {
                        new ManageRecipeMenu(player, type, recipeName, false, Integer.parseInt(furnaceArgs[0]), 5 * 20).openMenu();
                        return true;
                    }
                    player.sendMessage(msg.getMessage("commands.create.invalid-args"));
                } else {
                    new ManageRecipeMenu(player, type, recipeName, false, 0, 5 * 20).openMenu();
                }
                break;
        }
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        MessageManager msg = MessageManager.getInstance();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getMessage("only-players"));
            return true;
        }

        if (!sender.hasPermission("customcrafting.command.edit")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getMessage("commands.edit.usage"));
            return true;
        }

        Player player = (Player) sender;
        String recipeName = args[1];
        if (!RecipeManager.getInstance().recipeExists(recipeName)) {
            player.sendMessage(msg.getMessage("commands.edit.not-found"));
            return true;
        }

        RecipeType type = RecipeManager.getInstance().getType(recipeName);

        switch (type) {
            case CRAFTING:
                new ManageRecipeMenu(player, type, recipeName, true).openMenu();
                break;
            case FURNACE:
                new ManageRecipeMenu(player, type, recipeName, true, RecipeManager.getInstance().getExperience(recipeName), RecipeManager.getInstance().getCookingTime(recipeName)).openMenu();
                break;
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        MessageManager msg = MessageManager.getInstance();
        
        if (!sender.hasPermission("customcrafting.command.delete")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getMessage("commands.delete.usage"));
            return true;
        }

        String recipeName = args[1];

        if (RecipeManager.getInstance().getRecipes().contains(recipeName)) {
            RecipeManager.getInstance().deleteRecipe(recipeName);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", recipeName);
            sender.sendMessage(msg.getMessage("commands.delete.success", placeholders));
        } else {
            sender.sendMessage(msg.getMessage("commands.delete.not-found"));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        MessageManager msg = MessageManager.getInstance();
        
        if (!sender.hasPermission("customcrafting.command.list")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        ArrayList<String> recipes = RecipeManager.getInstance().getRecipes();
        if (recipes.isEmpty()) {
            sender.sendMessage(msg.getMessage("commands.list.empty"));
            return true;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(recipes.size()));
        sender.sendMessage(msg.getMessage("commands.list.header", placeholders));
        
        recipes.forEach(recipe -> {
            Map<String, String> itemPlaceholders = new HashMap<>();
            itemPlaceholders.put("name", recipe);
            sender.sendMessage(msg.getMessage("commands.list.item", itemPlaceholders));
        });
        return true;
    }

    private boolean handleRecipes(CommandSender sender, String[] args) {
        MessageManager msg = MessageManager.getInstance();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.getMessage("only-players"));
            return true;
        }

        if (!sender.hasPermission("customcrafting.command.recipes")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(msg.getMessage("commands.recipes.usage"));
            return true;
        }

        Player player = (Player) sender;
        String recipeName = args[1];

        if (!RecipeManager.getInstance().recipeExists(recipeName)) {
            player.sendMessage(msg.getMessage("commands.recipes.not-found"));
            return true;
        }

        RecipeType type = RecipeManager.getInstance().getType(recipeName);
        new ViewRecipeMenu(player, type, recipeName).openMenu();
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        MessageManager msg = MessageManager.getInstance();

        if (!sender.hasPermission("customcrafting.command.reload")) {
            sender.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        // Перезагружаем конфигурацию
        CustomCrafting.getInstance().reloadConfig();
        
        // Перезагружаем сообщения
        MessageManager.getInstance().reloadMessages();
        
        // Перезагружаем рецепты
        RecipeManager.getInstance().loadRecipes();

        sender.sendMessage(msg.getMessage("reload-success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("customcrafting.command")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "edit", "delete", "list", "recipes", "reload").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                return Arrays.asList("CRAFTING", "FURNACE").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("recipes")) {
                return RecipeManager.getInstance().getRecipes().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}