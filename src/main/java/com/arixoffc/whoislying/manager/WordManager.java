package com.arixoffc.whoislying.manager;

import com.arixoffc.whoislying.WhoIsLying;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class WordManager {
    
    private final WhoIsLying plugin;
    private final Map<String, List<String>> categories;
    private FileConfiguration wordsConfig;

    public WordManager(WhoIsLying plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        loadWords();
    }

    public void loadWords() {
        categories.clear();
        
        File wordsFile = new File(plugin.getDataFolder(), "words.yml");
        if (!wordsFile.exists()) {
            plugin.saveResource("words.yml", false);
        }
        
        wordsConfig = YamlConfiguration.loadConfiguration(wordsFile);
        ConfigurationSection categoriesSection = wordsConfig.getConfigurationSection("categories");
        
        if (categoriesSection == null) {
            plugin.getLogger().severe("No categories found in words.yml!");
            return;
        }
        
        for (String category : categoriesSection.getKeys(false)) {
            List<String> words = wordsConfig.getStringList("categories." + category);
            if (!words.isEmpty()) {
                categories.put(category, words);
                plugin.getLogger().info("Loaded category '" + category + "' with " + words.size() + " words");
            }
        }
        
        plugin.getLogger().info("Loaded " + categories.size() + " categories");
    }

    public String getRandomCategory() {
        if (categories.isEmpty()) {
            return null;
        }
        List<String> categoryList = new ArrayList<>(categories.keySet());
        return categoryList.get(new Random().nextInt(categoryList.size()));
    }

    public String getRandomWord(String category) {
        List<String> words = categories.get(category);
        if (words == null || words.isEmpty()) {
            return null;
        }
        return words.get(new Random().nextInt(words.size()));
    }

    public Map<String, String> getRandomWordWithCategory() {
        String category = getRandomCategory();
        if (category == null) {
            return null;
        }
        String word = getRandomWord(category);
        if (word == null) {
            return null;
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("category", category);
        result.put("word", word);
        return result;
    }

    public Map<String, List<String>> getCategories() {
        return new HashMap<>(categories);
    }

    public int getTotalWords() {
        return categories.values().stream().mapToInt(List::size).sum();
    }

    public int getTotalCategories() {
        return categories.size();
    }
}
