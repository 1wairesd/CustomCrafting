package be.razerstorm.customcrafting.inventories;

import be.razerstorm.customcrafting.enums.RecipeType;
import be.razerstorm.customcrafting.managers.MessageManager;
import be.razerstorm.customcrafting.managers.RecipeManager;
import be.razerstorm.customcrafting.objects.RecipeInfo;
import be.razerstorm.customcrafting.utils.GUIHolder;
import be.razerstorm.customcrafting.utils.ItemBuilder;
import be.razerstorm.customcrafting.utils.Utils;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewRecipeMenu extends GUIHolder {

    private final RecipeType type;
    private final Player player;
    private final String recipeName;
    private final List<Integer> craftingSlots = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);
    private final List<Integer> furnaceSlots = Arrays.asList(10, 28);
    private final HashMap<Integer, Integer[]> rows = new HashMap<Integer, Integer[]>() {{
        put(1, new Integer[]{10, 11, 12});
        put(2, new Integer[]{19, 20, 21});
        put(3, new Integer[]{28, 29, 30});
    }};

    public ViewRecipeMenu(Player player, RecipeType type, String recipeName) {
        this.type = type;
        this.player = player;
        this.recipeName = recipeName;
    }

    public void openMenu() {
        MessageManager msg = MessageManager.getInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", recipeName);
        
        this.inventory = Bukkit.createInventory(this, 5 * 9, msg.getMessage("gui.viewing", placeholders));

        ItemStack glassPane = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial())
                .setColoredName("&8")
                .toItemStack();

        // Заполняем все слоты стеклом
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, glassPane);
        }

        // Очищаем слоты для крафта (квадрат 3x3)
        if (type == RecipeType.CRAFTING) {
            for (int slot : craftingSlots) {
                inventory.setItem(slot, null);
            }
            // Очищаем слот результата и стрелку
            inventory.setItem(22, null);
            inventory.setItem(23, null);
        } else if (type == RecipeType.FURNACE) {
            // Очищаем слоты для печи
            for (int slot : furnaceSlots) {
                inventory.setItem(slot, null);
            }
            // Очищаем слоты для информации, стрелки и результата
            inventory.setItem(19, null);
            inventory.setItem(22, null);
            inventory.setItem(23, null);
        }

        switch (type) {
            case CRAFTING:
                loadCraftingRecipe();
                break;
            case FURNACE:
                loadFurnaceRecipe();
                break;
        }

        // Добавляем информационный предмет
        placeholders.put("type", type.name());
        ItemStack info = new ItemBuilder(XMaterial.BOOK.parseMaterial())
                .setColoredName(msg.getMessage("items.recipe-info.name"))
                .setLore(msg.getMessageList("items.recipe-info.lore").stream()
                        .map(line -> {
                            String result = line;
                            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
                            }
                            return result;
                        })
                        .toArray(String[]::new))
                .toItemStack();
        inventory.setItem(4, info);

        open(player);
    }

    private void loadCraftingRecipe() {
        RecipeInfo recipeInfo = RecipeManager.getInstance().getRecipeInfo(recipeName);
        ItemStack output = recipeInfo.getOutput();
        String[] shape = recipeInfo.getShape();
        HashMap<Character, ItemStack> ingredients = recipeInfo.getIngredients();

        // Устанавливаем результат
        inventory.setItem(23, output);

        // Устанавливаем ингредиенты
        for (int i = 0; i < shape.length; i++) {
            String row = shape[i];
            for (int j = 0; j < row.length(); j++) {
                char c = row.charAt(j);
                ItemStack ingredient = ingredients.get(c);
                if (ingredient != null) {
                    inventory.setItem(rows.get(i + 1)[j], ingredient);
                }
            }
        }

        // Добавляем стрелку
        ItemStack arrow = new ItemBuilder(XMaterial.ARROW.parseMaterial())
                .setColoredName(MessageManager.getInstance().getMessage("items.crafting-result.name"))
                .toItemStack();
        inventory.setItem(22, arrow);
    }

    private void loadFurnaceRecipe() {
        ItemStack output = RecipeManager.getInstance().getOutput(recipeName);
        ItemStack ingredient = RecipeManager.getInstance().getIngredient(recipeName);
        int experience = RecipeManager.getInstance().getExperience(recipeName);
        int cookingTime = RecipeManager.getInstance().getCookingTime(recipeName);

        // Устанавливаем ингредиент
        inventory.setItem(10, ingredient);

        // Устанавливаем результат
        inventory.setItem(23, output);

        // Добавляем информацию о печи
        MessageManager msg = MessageManager.getInstance();
        Map<String, String> furnacePlaceholders = new HashMap<>();
        furnacePlaceholders.put("experience", String.valueOf(experience));
        furnacePlaceholders.put("time", String.valueOf(cookingTime / 20));
        
        ItemStack furnaceInfo = new ItemBuilder(XMaterial.FURNACE.parseMaterial())
                .setColoredName(msg.getMessage("items.furnace-info.name"))
                .setLore(msg.getMessageList("items.furnace-info.lore").stream()
                        .map(line -> {
                            String result = line;
                            for (Map.Entry<String, String> entry : furnacePlaceholders.entrySet()) {
                                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
                            }
                            return result;
                        })
                        .toArray(String[]::new))
                .toItemStack();
        inventory.setItem(19, furnaceInfo);

        // Добавляем стрелку
        ItemStack arrow = new ItemBuilder(XMaterial.ARROW.parseMaterial())
                .setColoredName(msg.getMessage("items.smelting-result.name"))
                .toItemStack();
        inventory.setItem(22, arrow);

        // Добавляем топливо (декоративное)
        ItemStack fuel = new ItemBuilder(XMaterial.COAL.parseMaterial())
                .setColoredName(msg.getMessage("items.fuel.name"))
                .toItemStack();
        inventory.setItem(28, fuel);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // Блокируем все клики в верхнем инвентаре
        if (event.getView().getTopInventory() == event.getClickedInventory()) {
            event.setCancelled(true);
        }

        // Блокируем shift-click из нижнего инвентаря
        if (event.isShiftClick() && event.getView().getBottomInventory() == event.getClickedInventory()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        // Ничего не делаем при закрытии
    }
}
