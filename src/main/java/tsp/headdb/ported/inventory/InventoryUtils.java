package tsp.headdb.ported.inventory;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientHandledScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.InputOverlay;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.StringInput;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.HideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tsp.headdb.ported.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryUtils {

    private static final Map<String, Integer> uiLocation = new HashMap<>();
    private static final Map<String, ItemStack> uiItem = new HashMap<>();

    public static int getUILocation(String category, int slot) {
        // Try to use the cached value first.
        if (uiLocation.containsKey(category)) return uiLocation.get(category);
        
        // No valid value in the config file, return the given default.
        uiLocation.put(category, slot);
        return slot;
    }

    public static ItemStack getUIItem(String category, ItemStack item) {
        // Try to use the cached item first.
        if (uiItem.containsKey(category)) return uiItem.get(category);
        
        // No valid head or item in the config file, return the given default.
        uiItem.put(category, item);
        return item;
    }

    public static void openLocalMenu() {
        PagedPane pane = new PagedPane(4, 6, TextInst.translatable("nbteditor.hdb.local.title").getString());

        List<LocalHead> heads = HeadAPI.getLocalHeads();
        for (LocalHead localHead : heads) {
            pane.addButton(new Button(localHead.getItemStack(), e -> {
                if (e.getContainerInput() == ClickTypeMod.LEFT_SHIFT) {
                    purchaseHead(localHead, 64, "local", localHead.getName());
                    return;
                }
                if (e.getContainerInput() == ClickTypeMod.LEFT) {
                    purchaseHead(localHead, 1, "local", localHead.getName());
                    return;
                }
                if (e.getContainerInput() == ClickTypeMod.RIGHT) {
                    MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.feedback.local_no_favorites"));
                }
            }));
        }

        pane.open();
    }

    public static void openCustomMenu() {
        PagedPane pane = new PagedPane(4, 6, TextInst.translatable("nbteditor.hdb.custom.title").getString());

        List<Head> heads = HeadAPI.getCustomHeads();
        for (Head head : heads) {
            pane.addButton(new Button(head.getItemStack(), e -> {
                if (e.getContainerInput() == ClickTypeMod.LEFT_SHIFT) {
                    purchaseHead(head, 64, "custom", head.getName());
                    return;
                }
                if (e.getContainerInput() == ClickTypeMod.LEFT) {
                    purchaseHead(head, 1, "custom", head.getName());
                    return;
                }
                if (e.getContainerInput() == ClickTypeMod.RIGHT) {
                    HeadAPI.removeCustomHead(head.getValue());
                    openCustomMenu();
                    MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.feedback.removed_custom", head.getName()));
                }
            }));
        }

        pane.open();
    }

    public static void openFavoritesMenu() {
        PagedPane pane = new PagedPane(4, 6, TextInst.translatable("nbteditor.hdb.favorites.title").getString());

        List<Head> heads = HeadAPI.getFavoriteHeads();
        for (Head head : heads) {
            pane.addButton(new Button(head.getItemStack(), e -> {
                if (e.getContainerInput() == ClickTypeMod.LEFT_SHIFT) {
                    purchaseHead(head, 64, HeadAPI.getCategoryName(head), head.getName());
                    return;
                }
                if (e.getContainerInput() == ClickTypeMod.LEFT) {
                    purchaseHead(head, 1, HeadAPI.getCategoryName(head), head.getName());
                }
                if (e.getContainerInput() == ClickTypeMod.RIGHT) {
                    HeadAPI.removeFavoriteHead(head.getValue());
                    openFavoritesMenu();
                    MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.hdb.feedback.removed_favorite", head.getName()));
                }
            }));
        }

        pane.open();
    }

    public static PagedPane openSearchDatabase(String search) {
        PagedPane pane = new PagedPane(4, 6, TextInst.translatable("nbteditor.hdb.search.title", search).getString());

        List<Head> heads = HeadAPI.getHeadsByName(search);
        for (Head head : heads)
            pane.addButton(genButton(head));

        pane.open();
        return pane;
    }

    public static void openTagSearchDatabase(String tag) {
        PagedPane pane = new PagedPane(4, 6, TextInst.translatable("nbteditor.hdb.tags.title", tag).getString());

        List<Head> heads = HeadAPI.getHeadsByTag(tag);
        for (Head head : heads) {
            pane.addButton(genButton(head));
        }

        pane.open();
    }

    public static void openCategoryDatabase(Category category) {
        PagedPane pane = new PagedPane(4, 6, Utils.colorize("&c&lHeadDB &8- &e" + category.getTranslatedName()));

        List<Head> heads = HeadAPI.getHeads(category);
        for (Head head : heads) {
            pane.addButton(genButton(head));
        }

        pane.open();
    }
    
    private static Button genButton(Head head) {
    	return new Button(head.getItemStack(), e -> {
            if (e.getContainerInput() == ClickTypeMod.LEFT_SHIFT)
                purchaseHead(head, 64, head.getCategory().getName(), head.getName());
            else if (e.getContainerInput() == ClickTypeMod.LEFT)
                purchaseHead(head, 1, head.getCategory().getName(), head.getName());
            else if (e.getContainerInput() == ClickTypeMod.RIGHT)
                HeadAPI.toggleFavoriteHead(head);
        });
    }

    public static void openDatabase() {
	    	ClientHandledScreen screen = new ClientHandledScreen(6,
	    			TextInst.translatable("nbteditor.hdb.main.title", HeadAPI.getHeads().size())) {
	    		@Override
	    		protected void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType) {
	    			if (slot == null)
	    				return;
	    			slotId = slot.index;
	    			
	    			Container inventory = this.menu.getContainer();
	    			
	                if (inventory != null) {
	                    ItemStack item = slot.getItem();

	                    if (item != null && !item.isEmpty()) {
	                        if (slotId == getUILocation("favorites", 39)) {
	                            InventoryUtils.openFavoritesMenu();
	                            return;
	                        }
	                        if (slotId == getUILocation("local", 41)) {
	                            InventoryUtils.openLocalMenu();
	                            return;
	                        }
	                        if (slotId == getUILocation("custom", 42)) {
	                            InventoryUtils.openCustomMenu();
	                            return;
	                        }
	                        if (slotId == getUILocation("search", 40)) {
	                        	InputOverlay.show(
	                        			TextInst.translatable("nbteditor.hdb.search"),
	                        			StringInput.builder().withPlaceholder(TextInst.of("Query")).build(),
	                        			InventoryUtils::openSearchDatabase);
	                            return;
	                        }

	                        String name = MainUtil.stripColor(item.getHoverName().getString().toLowerCase());
	                        Category category = Category.getByName(name);

	                        if (category != null) {
	                            HeadAPI.openCategoryDatabase(category);
	                        }
	                    }
	                }
	    		}
    		@Override
    		public void onClose() {
    			if (MainUtil.client.player != null)
    				MainUtil.client.player.closeContainer();
    		}
    	};
        Container inventory = screen.getMenu().getContainer();

        for (Category category : Category.getValues()) {
            ItemStack item = getUIItem(category.getName(), category.getItem());
            item.set(DataComponents.CUSTOM_NAME,TextInst.of(Utils.colorize(category.getColor() + "&l" + category.getTranslatedName().toUpperCase())));
            ItemTagReferences.LORE.set(item, List.of(TextInst.of(
            		Utils.colorize("&e" + TextInst.translatable("nbteditor.hdb.head_count", HeadAPI.getHeads(category).size()).getString()))));
            inventory.setItem(getUILocation(category.getName(), category.getLocation()), item);
        }

        inventory.setItem(getUILocation("favorites", 39), buildButton(
            getUIItem("favorites", new ItemStack(Items.BOOK)),
            TextInst.translatable("nbteditor.hdb.button.favorites").getString(),
            "",
            TextInst.translatable("nbteditor.hdb.lore.favorites").getString())
        );

        inventory.setItem(getUILocation("search", 40), buildButton(
            getUIItem("search", new ItemStack(Items.DARK_OAK_SIGN)),
            TextInst.translatable("nbteditor.hdb.button.search").getString(),
            "",
            TextInst.translatable("nbteditor.hdb.lore.search").getString())
        );

        inventory.setItem(getUILocation("local", 41), buildButton(
            getUIItem("local", new ItemStack(Items.COMPASS)),
            TextInst.translatable("nbteditor.hdb.button.local").getString(),
            "",
            TextInst.translatable("nbteditor.hdb.lore.local").getString())
        );

        inventory.setItem(getUILocation("custom", 42), buildButton(
            getUIItem("custom", new ItemStack(Items.FILLED_MAP)),
            TextInst.translatable("nbteditor.hdb.button.custom").getString(),
            "",
            TextInst.translatable("nbteditor.hdb.lore.custom").getString())
        );

        fill(inventory);
        MainUtil.client.gui.setScreen(screen);
    }

    public static void fill(Container inv) {
        ItemStack item = getUIItem("fill", new ItemStack(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BLACK)));
        // Do not bother filling the inventory if item to fill it with is AIR.
        if (item == null || item.isEmpty()) return;
        
        if (HideFlag.TOOLTIP != null)
        	ItemTagReferences.HIDE_FLAGS.set(item, Map.of(HideFlag.TOOLTIP, true));

        // Fill any non-empty inventory slots with the given item.
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem == null || slotItem.isEmpty()) {
                inv.setItem(i, item);
            }
        }
    }

    private static ItemStack buildButton(ItemStack item, String name, String... lore) {
        item.set(DataComponents.CUSTOM_NAME,TextInst.of(Utils.colorize(name)));
        ItemTagReferences.LORE.set(item, Arrays.stream(lore).map(Utils::colorize).map(TextInst::of).toList());
        return item;
    }
    
    
    public static void purchaseHead(Head head, int amount, String category, String description) {
        ItemStack item = head.getItemStack();
        item.setCount(amount);
        MainUtil.getWithMessage(item);
    }

}
