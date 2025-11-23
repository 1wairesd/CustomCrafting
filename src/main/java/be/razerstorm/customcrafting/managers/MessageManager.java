package be.razerstorm.customcrafting.managers;

import be.razerstorm.customcrafting.CustomCrafting;
import be.razerstorm.customcrafting.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private static MessageManager instance;
    private final CustomCrafting plugin = CustomCrafting.getInstance();
    private FileConfiguration messages;
    private File messagesFile;

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Загружаем дефолтные значения
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messages.setDefaults(defConfig);
        }
    }

    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void saveMessages() {
        if (messages == null || messagesFile == null) {
            return;
        }
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return Utils.color(message);
    }

    public String getMessageWithPrefix(String path) {
        return getMessageWithPrefix(path, new HashMap<>());
    }

    public String getMessageWithPrefix(String path, Map<String, String> placeholders) {
        String prefix = getMessage("prefix");
        String message = getMessage(path, placeholders);
        return prefix + " " + message;
    }

    public List<String> getMessageList(String path) {
        List<String> messages = this.messages.getStringList(path);
        messages.replaceAll(Utils::color);
        return messages;
    }

    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }
}
