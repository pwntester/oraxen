package io.th0rgal.oraxen.utils.storage;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;

public class StorageMechanic {

    public static Set<Player> playerStorages = new HashSet<>();
    public static Map<Block, StorageGui> blockStorages = new HashMap<>();
    public static Map<ItemFrame, StorageGui> frameStorages = new HashMap<>();
    public static final NamespacedKey STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "storage");
    public static final NamespacedKey PERSONAL_STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "personal_storage");
    private final int rows;
    private final String title;
    private final StorageType type;
    private final String openSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        type = StorageType.valueOf(section.getString("type", "STORAGE"));
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 0.8f);
    }

    public enum StorageType {
        STORAGE, PERSONAL, ENDERCHEST, DISPOSAL, SHULKER
    }

    public void openPersonalStorage(Player player) {
        if (type != StorageType.PERSONAL) return;
        StorageGui storageGui = createPersonalGui(player);
        storageGui.open(player);
    }

    public void openDisposal(Player player, Location location) {
        if (type != StorageType.DISPOSAL) return;
        StorageGui storageGui = createDisposalGui(location);
        storageGui.open(player);
    }

    public void openStorage(Block block, Player player) {
        if (block.getType() != Material.NOTE_BLOCK) return;
        StorageGui storageGui = (blockStorages.containsKey(block) ? blockStorages.get(block) : createGui(block));
        storageGui.open(player);
        blockStorages.put(block, storageGui);
    }

    public void openStorage(ItemFrame frame, Player player) {
        StorageGui storageGui = (frameStorages.containsKey(frame) ? frameStorages.get(frame) : createGui(frame));
        storageGui.open(player);
        frameStorages.put(frame, storageGui);
    }

    public void dropStorageContent(Block block) {
        StorageGui gui = blockStorages.get(block);
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = (blockStorages.containsKey(block) && gui != null)
                ? gui.getInventory().getContents() : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});

        if (isShulker()) {
            NoteBlockMechanic mechanic = NoteBlockMechanicListener.getNoteBlockMechanic(block);
            if (mechanic == null) return;

            ItemStack shulker = OraxenItems.getItemById(mechanic.getItemID()).build();
            ItemMeta shulkerMeta = shulker.getItemMeta();

            if (shulkerMeta != null)
                shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);

            shulker.setItemMeta(shulkerMeta);
            block.getWorld().dropItemNaturally(block.getLocation(), shulker);
        } else for (ItemStack item : items) {
            if (item == null) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
        if (gui != null) {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
            for (HumanEntity player : players) gui.close(player);
        }
        pdc.remove(STORAGE_KEY);
        blockStorages.remove(block);
    }

    public void dropStorageContent(FurnitureMechanic mechanic, ItemFrame frame) {
        StorageGui gui = frameStorages.get(frame);
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = (frameStorages.containsKey(frame) && gui != null)
                ? gui.getInventory().getContents() : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        if (isShulker()) {
            ItemStack defaultItem = OraxenItems.getItemById(mechanic.getItemID()).build();
            ItemStack shulker = frame.getItem();
            ItemMeta shulkerMeta = shulker.getItemMeta();

            if (shulkerMeta != null) {
                shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                shulkerMeta.setDisplayName(defaultItem.getItemMeta() != null ? defaultItem.getItemMeta().getDisplayName() : null);
                shulker.setItemMeta(shulkerMeta);
            }
            frame.getWorld().dropItemNaturally(frame.getLocation(), shulker);
        } else for (ItemStack item : items) {
            if (item == null) continue;
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        }

        if (gui != null) {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
            for (HumanEntity player : players) gui.close(player);
        }
        pdc.remove(STORAGE_KEY);
        frameStorages.remove(frame);
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public StorageType getStorageType() {
        return type;
    }

    public boolean isStorage() {
        return type == StorageType.STORAGE;
    }

    public boolean isPersonal() {
        return type == StorageType.PERSONAL;
    }

    public boolean isEnderchest() {
        return type == StorageType.ENDERCHEST;
    }

    public boolean isDisposal() {
        return type == StorageType.DISPOSAL;
    }

    public boolean isShulker() {
        return type == StorageType.SHULKER;
    }

    public boolean hasOpenSound() {
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasCloseSound() {
        return closeSound != null;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }

    private StorageGui createDisposalGui(Location location) {
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        gui.setOpenGuiAction(event -> {
            gui.getInventory().clear();
            if (hasOpenSound() && location.isWorldLoaded())
                Objects.requireNonNull(location.getWorld()).playSound(location, openSound, volume, pitch);
        });

        gui.setCloseGuiAction(event -> {
            gui.getInventory().clear();
            if (hasCloseSound() && location.isWorldLoaded())
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
        });
        return gui;
    }

    private StorageGui createPersonalGui(Player player) {
        PersistentDataContainer storagePDC = player.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()), 3L);
            }
        });

        gui.setOpenGuiAction(event -> {
            playerStorages.add(player);
            if (storagePDC.has(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(Objects.requireNonNull(storagePDC.get(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY)));
        });

        gui.setCloseGuiAction(event -> {
            playerStorages.remove(player);
            storagePDC.set(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && player.getLocation().isWorldLoaded())
                Objects.requireNonNull(player.getLocation().getWorld()).playSound(player.getLocation(), closeSound, volume, pitch);
        });

        return gui;
    }

    private StorageGui createGui(Block block) {
        Location location = block.getLocation();
        PersistentDataContainer storagePDC = BlockHelpers.getPDC(block);
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()), 3L);
            }
        });
        gui.setOpenGuiAction(event -> {
            if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{}));
        });

        gui.setCloseGuiAction(event -> {
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && location.isWorldLoaded() && block.getWorld().isChunkLoaded(block.getChunk()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
        });

        return gui;
    }

    private StorageGui createGui(ItemFrame frame) {
        Location location = frame.getLocation();
        PersistentDataContainer storagePDC = frame.getPersistentDataContainer();
        PersistentDataContainer shulkerPDC = Objects.requireNonNull(frame.getItem().getItemMeta()).getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()), 3L);
            }
        });

        // If it's a shulker, get the itemstack array of the items pdc, otherwise use the frame pdc
        gui.setOpenGuiAction(event -> gui.getInventory().setContents(
                (!isShulker() && storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)
                        ? storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                        : (isShulker() && shulkerPDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                        ? shulkerPDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                        : new ItemStack[]{})));

        gui.setCloseGuiAction(event -> {
            if (gui.getInventory().getViewers().size() <= 1) {
                if (isShulker()) {
                    shulkerPDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                } else {
                    storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }
            }
            if (hasCloseSound() && location.isWorldLoaded() && frame.getWorld().isChunkLoaded(frame.getLocation().getChunk()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
        });

        return gui;
    }
}
