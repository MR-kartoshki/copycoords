package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
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

public class CopyCoordsDataStore {
    private static final int HISTORY_LIMIT = 25;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path dataPath;

    private static Path getScopedDataPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("copycoords").resolve("copycoords-data.json");
    }

    private static Path getLegacyDataPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("copycoords-data.json");
    }

    private List<HistoryEntry> history = new ArrayList<>();
    private Map<String, BookmarkEntry> bookmarks = new LinkedHashMap<>();

    public static CopyCoordsDataStore load() {
        dataPath = getScopedDataPath();
        Path legacyPath = getLegacyDataPath();

        Path readPath = Files.exists(dataPath) ? dataPath : legacyPath;
        if (Files.exists(readPath)) {
            try {
                String json = Files.readString(readPath);
                CopyCoordsDataStore data = GSON.fromJson(json, CopyCoordsDataStore.class);
                if (data != null) {
                    if (data.history == null) {
                        data.history = new ArrayList<>();
                    }
                    if (data.bookmarks == null) {
                        data.bookmarks = new LinkedHashMap<>();
                    }
                    if (!dataPath.equals(readPath)) {
                        data.save();
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
            dataPath = getScopedDataPath();
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

    public boolean removeHistoryEntry(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= history.size()) {
            return false;
        }
        history.remove(zeroBasedIndex);
        save();
        return true;
    }

    public boolean addBookmark(String name, double x, double y, double z, String dimensionId) {
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

    public boolean exportBookmarks(Path out) {
        try {
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = GSON.toJson(bookmarks.values());
            Files.writeString(out, json);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to export bookmarks: " + e.getMessage());
            return false;
        }
    }

    public boolean importBookmarks(Path in) {
        if (!Files.exists(in)) {
            System.err.println("Import file not found: " + in);
            return false;
        }
        try {
            String json = Files.readString(in);
            BookmarkEntry[] arr = GSON.fromJson(json, BookmarkEntry[].class);
            Map<String, BookmarkEntry> importedBookmarks = new LinkedHashMap<>();
            if (arr != null) {
                for (BookmarkEntry entry : arr) {
                    BookmarkEntry sanitized = validateImportedBookmark(entry);
                    String key = normalizeName(sanitized.name);
                    importedBookmarks.put(key, sanitized);
                }
            }
            bookmarks.putAll(importedBookmarks);
            save();
            return true;
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            System.err.println("Failed to import bookmarks: " + e.getMessage());
            return false;
        }
    }

    private BookmarkEntry validateImportedBookmark(BookmarkEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Bookmark import contains a null entry.");
        }
        if (entry.name == null || entry.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Bookmark import contains an entry with no name.");
        }
        return new BookmarkEntry(entry.name, entry.x, entry.y, entry.z, entry.dimensionId);
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
        public double x;
        public double y;
        public double z;
        public String dimensionId;

        public BookmarkEntry(String name, double x, double y, double z, String dimensionId) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }
    }
}

