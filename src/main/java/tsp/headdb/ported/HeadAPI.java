package tsp.headdb.ported;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.multiplayer.PlayerInfo;
import tsp.headdb.ported.inventory.InventoryUtils;

/**
 * This class provides simple methods
 * for interacting with the HeadDB plugin
 *
 * @author TheSilentPro
 */
public final class HeadAPI {

    private HeadAPI() {}

    private static final HeadDatabase database = new HeadDatabase();

    /**
     * Retrieves the main {@link HeadDatabase}
     *
     * @return Head Database
     */
    public static HeadDatabase getDatabase() {
        return database;
    }

    /**
     * Opens the database for a player
     * 
     */
    public static void openDatabase() {
        InventoryUtils.openDatabase();
    }

    /**
     * Opens a specific category of the database for a player
     * 
     * @param category Category to open
     */
    public static void openCategoryDatabase(Category category) {
        InventoryUtils.openCategoryDatabase(category);
    }

    /**
     * Opens the database with results of a specific search term
     *
     * @param search Search term
     */
    public static void openSearchDatabase(String search) {
        InventoryUtils.openSearchDatabase(search);
    }

    public static void openTagSearchDatabase(String tag) {
        InventoryUtils.openTagSearchDatabase(tag);
    }
    
    public static boolean checkUpdated() {
    	if (HeadAPI.getDatabase().isLastUpdateOld()) {
			MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.unloaded_database"));
			return false;
    	}
    	
    	return true;
    }

    /**
     * Retrieve a {@link Head} by it's ID
     *
     * @param id The ID of the head
     * @return The head
     */
    public static Head getHeadByID(int id) {
        return database.getHeadByID(id);
    }

    /**
     * Retrieve a {@link Head} by it's UUID
     *
     * @param uuid The UUID of the head
     * @return The head
     */
    public static Head getHeadByUniqueId(UUID uuid) {
        return database.getHeadByUniqueId(uuid);
    }

    public static List<Head> getHeadsByTag(String tag) {
        return database.getHeadsByTag(tag);
    }

    /**
     * Retrieves a {@link List} of {@link Head}'s matching a name
     *
     * @param name The name to match for
     * @return List of heads
     */
    public static List<Head> getHeadsByName(String name) {
        return database.getHeadsByName(name);
    }

    /**
     * Retrieves a {@link List} of {@link Head}'s in a {@link Category} matching a name
     *
     * @param category The category to search in
     * @param name The name to match for
     * @return List of heads
     */
    public static List<Head> getHeadsByName(Category category, String name) {
        return database.getHeadsByName(category, name);
    }

    /**
     * Retrieve a {@link Head} by it's value
     *
     * @param value The texture value
     * @return The head
     */
    public static Head getHeadByValue(String value) {
        return database.getHeadByValue(value);
    }

    /**
     * Retrieve a {@link List} of {@link Head}'s in a specific {@link Category}
     *
     * @param category The category to search in
     * @return List of heads
     */
    public static List<Head> getHeads(Category category) {
        return database.getHeads(category);
    }

    /**
     * Retrieve a {@link List} of all {@link Head}'s
     *
     * @return List of all heads
     */
    public static List<Head> getHeads() {
        return database.getHeads();
    }
    
    public static List<Category> getCategories() {
    	return database.getCategories();
    }
    
    private static final File FAVORITES_FILE = new File(NBTEditorClient.SETTINGS_FOLDER, "headdb_favorites.txt");
    private static final List<String> FAVORITES = new ArrayList<>();
    
    private static final File CUSTOM_FILE = new File(NBTEditorClient.SETTINGS_FOLDER, "headdb_custom.txt");
    private static final List<String> CUSTOM_HEADS = new ArrayList<>();
    
    /**
     * Add a {@link Head} to a players favorites
     *
     * @param texture The texture of the head
     */
    public static void addFavoriteHead(String texture) {
    	if (FAVORITES.contains(texture))
    		return;
    	
    	FAVORITES.add(texture);
    	saveFavorites();
    }

    /**
     * Remove a {@link Head} from a players favorites
     *
     * @param texture The texture of the head
     */
    public static void removeFavoriteHead(String texture) {
    	if (FAVORITES.remove(texture))
    		saveFavorites();
    }
    
    public static void toggleFavoriteHead(Head head) {
    	if (FAVORITES.contains(head.getValue())) {
    		removeFavoriteHead(head.getValue());
    		MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.feedback.removed_favorite", head.getName()));
    	} else {
    		addFavoriteHead(head.getValue());
    		MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.feedback.added_favorite", head.getName()));
    	}
    }
    
    public static void loadFavorites() throws IOException {
    	FAVORITES.clear();
    	
    	if (!FAVORITES_FILE.exists())
    		return;
    	
    	String heads = new String(Files.readAllBytes(FAVORITES_FILE.toPath())).replace("\r", "");
    	if (heads.startsWith("v2\n")) {
    		
    		JsonArray headsArray = new Gson().fromJson(heads.substring("v2\n".length()), JsonArray.class);
    		for (JsonElement head : headsArray)
    			FAVORITES.add(head.getAsString());
    		
    	} else {
	    	
    		for (String line : heads.split("\n")) {
	    		if (line.isEmpty())
	    			continue;
	    		
	    		try {
	    			FAVORITES.add("LEGACY: " + Integer.parseInt(line));
	    		} catch (NumberFormatException e) {
	    			NBTEditor.LOGGER.error("Invalid legacy favorite", e);
	    		}
	    	}
    		
    	}
    }
    public static void resolveFavorites() {
    	List<String> legacyFavorites = FAVORITES.stream().filter(entry -> entry.startsWith("LEGACY: ")).toList();
    	for (String legacyFavorite : legacyFavorites) {
    		FAVORITES.remove(legacyFavorite);
    		
    		Head head = getHeadByID(Integer.parseInt(legacyFavorite.substring("LEGACY: ".length())));
    		FAVORITES.add(head.getValue());
    	}
    	saveFavorites();
    }
    private static void saveFavorites() {
    	JsonArray output = new JsonArray();
    	for (String favorite : FAVORITES)
    		output.add(favorite);
    	try {
			Files.write(FAVORITES_FILE.toPath(), ("v2\n" + output.toString()).getBytes());
		} catch (IOException e) {
			NBTEditor.LOGGER.error("Error while saving HeadDB favorites", e);
		}
    }
    
    /**
     * Add a {@link Head} to the local custom head library
     *
     * @param value The texture value of the head
     */
    public static void addCustomHead(String value) {
    	if (CUSTOM_HEADS.contains(value))
    		return;
    	
    	CUSTOM_HEADS.add(value);
    	saveCustomHeads();
    }

    /**
     * Remove a {@link Head} from the local custom head library
     *
     * @param value The texture value of the head
     */
    public static void removeCustomHead(String value) {
    	if (CUSTOM_HEADS.remove(value))
    		saveCustomHeads();
    }
    
    /**
     * Load the local custom head library from disk
     */
    public static void loadCustomHeads() throws IOException {
    	CUSTOM_HEADS.clear();
    	
    	if (!CUSTOM_FILE.exists())
    		return;
    	
    	String heads = new String(Files.readAllBytes(CUSTOM_FILE.toPath())).replace("\r", "");
    	if (!heads.startsWith("v1\n"))
    		return;
    	
    	JsonArray headsArray = new Gson().fromJson(heads.substring("v1\n".length()), JsonArray.class);
    	for (JsonElement head : headsArray)
    		CUSTOM_HEADS.add(head.getAsString());
    }
    
    private static void saveCustomHeads() {
    	JsonArray output = new JsonArray();
    	for (String customHead : CUSTOM_HEADS)
    		output.add(customHead);
    	try {
    		Files.write(CUSTOM_FILE.toPath(), ("v1\n" + output.toString()).getBytes());
    	} catch (IOException e) {
    		NBTEditor.LOGGER.error("Error while saving HeadDB custom heads", e);
    	}
    }

    /**
     * Retrieve a {@link List} of heads in the local custom head library
     *
     * @return List of {@link Head}'s
     */
    public static List<Head> getCustomHeads() {
        List<Head> heads = new ArrayList<>();
        for (String customHead : CUSTOM_HEADS)
            heads.add(buildCustomHead(customHead));
        
        return heads;
    }

    /**
     * Retrieve a {@link List} of favorite {@link Head} for a player
     *
     * @param uuid The UUID of the player
     * @return List of favorite heads
     */
    public static List<Head> getFavoriteHeads() {
        List<Head> heads = new ArrayList<>();
        for (String favorite : FAVORITES) {
        	if (favorite.startsWith("LEGACY: ")) // Legacy favorites should already be resolved
        		continue;
            
            Head head = getHeadByValue(favorite);
            heads.add(head != null ? head : buildCustomHead(favorite));
        }
        
        return heads;
    }

    /**
     * Retrieve a {@link List} of local heads.
     * These heads are from players that have joined the server at least once.
     *
     * @return List of {@link LocalHead}'s
     */
    public static List<LocalHead> getLocalHeads() {
        List<LocalHead> heads = new ArrayList<>();
        for (PlayerInfo player : MainUtil.client.getConnection().getOnlinePlayers()) {
            heads.add(new LocalHead(player.getProfile().id())
                    .withName(player.getProfile().name()));
        }

        return heads;
    }

    /**
     * Parse the display name from a head texture value (base64 encoded profile JSON)
     *
     * @param value The texture value
     * @return The player name, or null if it could not be parsed
     */
    public static String parseHeadName(String value) {
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("name") && !obj.get("name").isJsonNull())
                return obj.get("name").getAsString();
            if (obj.has("profileName") && !obj.get("profileName").isJsonNull())
                return obj.get("profileName").getAsString();
        } catch (Exception e) {
            NBTEditor.LOGGER.warn("Failed to parse head value name", e);
        }
        return null;
    }

    /**
     * Builds a {@link Head} from a texture value, falling back to a self-contained custom head
     * when the value is not part of the official database
     *
     * @param value The texture value
     * @return A head, never null
     */
    public static Head buildCustomHead(String value) {
        Head official = getHeadByValue(value);
        if (official != null)
            return official;
        String name = parseHeadName(value);
        UUID uuid = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        return new Head(-1)
                .withName(name != null ? name : "Custom Head")
                .withUniqueId(uuid)
                .withValue(value);
    }

    /**
     * Returns the category name of a head, falling back to "custom" when it has no category.
     */
    public static String getCategoryName(Head head) {
        Category category = head.getCategory();
        return category != null ? category.getName() : "custom";
    }

    /**
     * Asynchronously queries a player head by their Minecraft username via the Mojang API.
     * Network requests run on a daemon thread, callbacks are dispatched on the client thread.
     *
     * @param name The player name to query
     * @param success Called with the resulting {@link Head} on success (never null)
     * @param error Called with an error key ("not_found", "no_textures" or "error") on failure
     */
    public static void getPlayerHeadAsync(String name, Consumer<Head> success, Consumer<String> error) {
        Thread thread = new Thread(() -> {
            try {
                String profileJson = getDatabase().fetch("https://api.mojang.com/users/profiles/minecraft/" + name);
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                if (profile == null || !profile.has("id")) {
                    MainUtil.client.execute(() -> error.accept("not_found"));
                    return;
                }
                String uuid = profile.get("id").getAsString();
                String dashed = uuid.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");

                String sessionJson = getDatabase().fetch(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + dashed);
                Head head = null;
                JsonArray properties = JsonParser.parseString(sessionJson).getAsJsonObject().getAsJsonArray("properties");
                for (JsonElement element : properties) {
                    JsonObject property = element.getAsJsonObject();
                    if ("textures".equals(property.get("name").getAsString())) {
                        head = new Head(-1)
                                .withName(profile.get("name").getAsString())
                                .withUniqueId(UUID.fromString(dashed))
                                .withValue(property.get("value").getAsString());
                        break;
                    }
                }
                Head result = head;
                MainUtil.client.execute(() -> {
                    if (result != null)
                        success.accept(result);
                    else
                        error.accept("no_textures");
                });
            } catch (Exception e) {
                NBTEditor.LOGGER.error("Failed to fetch player head for " + name, e);
                MainUtil.client.execute(() -> error.accept("error"));
            }
        }, "NBTEditor/HeadPlayer");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Update the Head Database
     */
    public static void updateDatabase() {
        database.update();
    }

}
