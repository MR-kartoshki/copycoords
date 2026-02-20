package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Stores coordinate history and bookmarks in a persistent JSON file
public class CopyCoordsDataStore {
    private static final int HISTORY_LIMIT = 25;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path dataPath;

    private List<HistoryEntry> history = new ArrayList<>();
    private Map<String, BookmarkEntry> bookmarks = new LinkedHashMap<>();

    public static CopyCoordsDataStore load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        dataPath = configDir.resolve("copycoords-data.json");

        if (Files.exists(dataPath)) {
            try {
                String json = Files.readString(dataPath);
                CopyCoordsDataStore data = GSON.fromJson(json, CopyCoordsDataStore.class);
                if (data != null) {
                    if (data.history == null) {
                        data.history = new ArrayList<>();
                    }
                    if (data.bookmarks == null) {
                        data.bookmarks = new LinkedHashMap<>();
                    }
                    return data;
                }
            } catch (IOException e) {
                System.err.println("Failed to read copycoords data: " + e.getMessage());
            }
        }

        CopyCoordsDataStore data = new CopyCoordsDataStore();
        data.save();
        return data;
    }

    public void save() {
        if (dataPath == null) {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            dataPath = configDir.resolve("copycoords-data.json");
        }
        try {
            Files.createDirectories(dataPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(dataPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save copycoords data: " + e.getMessage());
        }
    }

    public void addHistoryEntry(int x, int y, int z, String dimensionId) {
        history.add(0, new HistoryEntry(x, y, z, dimensionId));
        while (history.size() > HISTORY_LIMIT) {
            history.remove(history.size() - 1);
        }
        save();
    }

    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void clearHistory() {
        history.clear();
        save();
    }

    public boolean addBookmark(String name, int x, int y, int z, String dimensionId) {
        String key = normalizeName(name);
        if (bookmarks.containsKey(key)) {
            return false;
        }
        bookmarks.put(key, new BookmarkEntry(name, x, y, z, dimensionId));
        save();
        return true;
    }

    public boolean removeBookmark(String name) {
        String key = normalizeName(name);
        if (bookmarks.remove(key) != null) {
            save();
            return true;
        }
        return false;
    }

    public BookmarkEntry getBookmark(String name) {
        String key = normalizeName(name);
        return bookmarks.get(key);
    }

    public List<BookmarkEntry> getBookmarks() {
        return new ArrayList<>(bookmarks.values());
    }

    public List<String> getBookmarkNames() {
        List<String> names = new ArrayList<>();
        for (BookmarkEntry entry : bookmarks.values()) {
            names.add(entry.name);
        }
        return names;
    }

    /**
     * Export current bookmarks to JSON file at given path. Returns true on success.
     */
    public boolean exportBookmarks(Path out) {
        try {
            Files.createDirectories(out.getParent());
            String json = GSON.toJson(bookmarks.values());
            Files.writeString(out, json);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to export bookmarks: " + e.getMessage());
            return false;
        }
    }

    /**
     * Import bookmarks from JSON file; existing names are overwritten.
     */
    public boolean importBookmarks(Path in) {
        if (!Files.exists(in)) {
            System.err.println("Import file not found: " + in);
            return false;
        }
        try {
            String json = Files.readString(in);
            BookmarkEntry[] arr = GSON.fromJson(json, BookmarkEntry[].class);
            if (arr != null) {
                for (BookmarkEntry entry : arr) {
                    String key = normalizeName(entry.name);
                    bookmarks.put(key, entry);
                }
                save();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to import bookmarks: " + e.getMessage());
            return false;
        }
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static class HistoryEntry {
        public int x;
        public int y;
        public int z;
        public String dimensionId;

        public HistoryEntry(int x, int y, int z, String dimensionId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }
    }

    public static class BookmarkEntry {
        public String name;
        public int x;
        public int y;
        public int z;
        public String dimensionId;

        public BookmarkEntry(String name, int x, int y, int z, String dimensionId) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }
    }
}
