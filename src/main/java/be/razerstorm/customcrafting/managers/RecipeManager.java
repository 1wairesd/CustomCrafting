package be.razerstorm.customcrafting.managers;

import be.razerstorm.customcrafting.CustomCrafting;
import be.razerstorm.customcrafting.enums.RecipeType;
import be.razerstorm.customcrafting.objects.RecipeInfo;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipeManager {

    private static RecipeManager instance;
    private final CustomCrafting plugin = CustomCrafting.getInstance();

    public void loadRecipes() {
        FileConfiguration config = plugin.getConfig();

        // Удаляем старые рецепты плагина перед загрузкой новых
        clearPluginRecipes();

        long initializeTime = System.currentTimeMillis();
        AtomicInteger recipesLoaded = new AtomicInteger();

        Objects.requireNonNull(config.getConfigurationSection("recipes")).getKeys(false).forEach(recipeName -> {
            plugin.getLogger().info("Loading recipe " + recipeName);
            String basePath = "recipes." + recipeName;
            
            if (config.get(basePath) == null ||
                    config.get(basePath + ".result") == null ||
                    config.get(basePath + ".type") == null) {
                plugin.getLogger().warning("Recipe " + recipeName + " is invalid!");
                return;
            }

            RecipeType type = RecipeType.valueOf(config.getString(basePath + ".type"));
            ItemStack output = (ItemStack) config.get(basePath + ".result");

            switch (type) {
                case CRAFTING: {
                    if (config.get(basePath + ".ingredients") == null ||
                            config.get(basePath + ".shape") == null) {
                        plugin.getLogger().warning("Recipe " + recipeName + " is invalid!");
                        return;
                    }

                    String[] shape = config.getStringList(basePath + ".shape").toArray(new String[0]);
                    HashMap<Character, ItemStack> ingredients = new HashMap<>();

                    config.getConfigurationSection(basePath + ".ingredients").getKeys(false).forEach(ingredientKey -> {
                        ingredients.put(ingredientKey.charAt(0), (ItemStack) config.get(basePath + ".ingredients." + ingredientKey));
                    });

                    pushToServerRecipes(output, ingredients, new NamespacedKey(plugin, recipeName), shape);
                    break;
                }
                case FURNACE: {
                    if (config.get(basePath + ".ingredient") == null ||
                            config.get(basePath + ".experience") == null ||
                            config.get(basePath + ".cookingTime") == null) {
                        plugin.getLogger().warning("Recipe " + recipeName + " is invalid!");
                        return;
                    }

                    ItemStack ingredient = (ItemStack) config.get(basePath + ".ingredient");
                    int experience = config.getInt(basePath + ".experience");
                    int cookingTime = config.getInt(basePath + ".cookingTime");

                    pushToServerRecipes(output, ingredient, new NamespacedKey(plugin, recipeName), experience, cookingTime);
                    break;
                }
            }
            recipesLoaded.getAndIncrement();
        });

        plugin.getLogger().info("Loaded " + recipesLoaded.get() + " recipes in " + (System.currentTimeMillis() - initializeTime) + "ms!");
    }

    public void addRecipe(String recipeName, ItemStack output, HashMap<Character, ItemStack> ingredients, String... shape) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "recipes." + recipeName;

        config.set(basePath + ".type", RecipeType.CRAFTING.name());
        config.set(basePath + ".result", output);
        config.set(basePath + ".shape", shape);

        ingredients.forEach((identifier, ingredient) -> 
            config.set(basePath + ".ingredients." + identifier, ingredient)
        );

        saveAndReload();
        pushToServerRecipes(output, ingredients, new NamespacedKey(plugin, recipeName), shape);
    }

    public void addRecipe(String recipeName, ItemStack output, ItemStack ingredient, int experience, int cookingTime) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "recipes." + recipeName;

        config.set(basePath + ".type", RecipeType.FURNACE.name());
        config.set(basePath + ".result", output);
        config.set(basePath + ".ingredient", ingredient);
        config.set(basePath + ".experience", experience);
        config.set(basePath + ".cookingTime", cookingTime);

        saveAndReload();
        pushToServerRecipes(output, ingredient, new NamespacedKey(plugin, recipeName), experience, cookingTime);
    }

    public void deleteRecipe(String recipeName) {
        FileConfiguration config = plugin.getConfig();

        config.set("recipes." + recipeName, null);
        saveAndReload();

        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeName));
    }

    public void editRecipe(String recipeName, ItemStack output, HashMap<Character, ItemStack> ingredients, String... shape) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "recipes." + recipeName;

        config.set(basePath, null);
        config.set(basePath + ".type", RecipeType.CRAFTING.name());
        config.set(basePath + ".result", output);
        config.set(basePath + ".shape", shape);

        ingredients.forEach((identifier, ingredient) -> 
            config.set(basePath + ".ingredients." + identifier, ingredient)
        );

        saveAndReload();

        NamespacedKey recipeKey = new NamespacedKey(plugin, recipeName);
        plugin.getServer().removeRecipe(recipeKey);
        pushToServerRecipes(output, ingredients, recipeKey, shape);
    }

    public void editRecipe(String recipeName, ItemStack output, ItemStack ingredient, int experience, int cookingTime) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "recipes." + recipeName;

        config.set(basePath, null);
        config.set(basePath + ".type", RecipeType.FURNACE.name());
        config.set(basePath + ".result", output);
        config.set(basePath + ".ingredient", ingredient);
        config.set(basePath + ".experience", experience);
        config.set(basePath + ".cookingTime", cookingTime);

        saveAndReload();

        NamespacedKey recipeKey = new NamespacedKey(plugin, recipeName);
        plugin.getServer().removeRecipe(recipeKey);
        pushToServerRecipes(output, ingredient, recipeKey, experience, cookingTime);
    }

    public int getExperience(String recipeName) {
        return plugin.getConfig().getInt("recipes." + recipeName + ".experience");
    }

    public int getCookingTime(String recipeName) {
        return plugin.getConfig().getInt("recipes." + recipeName + ".cookingTime");
    }

    public ItemStack getOutput(String recipeName) {
        return (ItemStack) plugin.getConfig().get("recipes." + recipeName + ".result");
    }

    public ItemStack getIngredient(String recipeName) {
        return (ItemStack) plugin.getConfig().get("recipes." + recipeName + ".ingredient");
    }

    private void pushToServerRecipes(ItemStack output, HashMap<Character, ItemStack> ingredients, NamespacedKey recipeKey, String... shape) {
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, output);
        recipe.shape(shape);

        ingredients.forEach((identifier, ingredient) -> 
            recipe.setIngredient(identifier, new RecipeChoice.ExactChoice(ingredient))
        );

        plugin.getServer().addRecipe(recipe);
    }

    private void pushToServerRecipes(ItemStack output, ItemStack ingredient, NamespacedKey recipeKey, int experience, int cookingTime) {
        plugin.getServer().addRecipe(
            new FurnaceRecipe(recipeKey, output, new RecipeChoice.ExactChoice(ingredient), experience, cookingTime)
        );
    }

    public ArrayList<String> getRecipes() {
        FileConfiguration config = plugin.getConfig();
        return new ArrayList<>(config.getConfigurationSection("recipes").getKeys(false));
    }

    public RecipeInfo getRecipeInfo(String recipeName) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "recipes." + recipeName;

        ItemStack output = (ItemStack) config.get(basePath + ".result");
        String[] shape = config.getStringList(basePath + ".shape").toArray(new String[0]);
        HashMap<Character, ItemStack> ingredients = new HashMap<>();

        config.getConfigurationSection(basePath + ".ingredients").getKeys(false).forEach(ingredientKey -> 
            ingredients.put(ingredientKey.charAt(0), (ItemStack) config.get(basePath + ".ingredients." + ingredientKey))
        );

        return new RecipeInfo(recipeName, output, ingredients, shape);
    }

    public boolean recipeExists(String recipe) {
        return plugin.getConfig().get("recipes." + recipe) != null;
    }

    public RecipeType getType(String recipe) {
        return RecipeType.valueOf(plugin.getConfig().getString("recipes." + recipe + ".type"));
    }

    private void saveAndReload() {
        plugin.saveConfig();
        plugin.reloadConfig();
    }

    private void clearPluginRecipes() {
        FileConfiguration config = plugin.getConfig();
        if (config.getConfigurationSection("recipes") == null) {
            return;
        }

        config.getConfigurationSection("recipes").getKeys(false).forEach(recipeName -> {
            NamespacedKey key = new NamespacedKey(plugin, recipeName);
            plugin.getServer().removeRecipe(key);
        });
    }

    public static RecipeManager getInstance() {
        if (instance == null) {
            instance = new RecipeManager();
        }
        return instance;
    }
}
