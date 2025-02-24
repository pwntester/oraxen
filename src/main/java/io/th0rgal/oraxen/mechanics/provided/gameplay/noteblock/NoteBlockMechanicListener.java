package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.events.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.events.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.events.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.storage.StorageMechanic;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.*;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.utils.BlockHelpers.getAnvilFacing;
import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;
import static io.th0rgal.oraxen.utils.storage.StorageMechanic.STORAGE_KEY;

public class NoteBlockMechanicListener implements Listener {
    private final MechanicFactory factory;

    public NoteBlockMechanicListener(final NoteBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        OraxenNoteBlockInteractEvent oraxenEvent = new OraxenNoteBlockInteractEvent(mechanic, block, event.getItem(), event.getPlayer(), event.getBlockFace());
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        if (oraxenEvent.isCancelled()) event.setCancelled(true);
    }

    // TODO try and fix these and not just cancel them
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        final Block aboveBlock = event.getBlock().getRelative(BlockFace.UP);
        if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            updateAndCheck(event.getBlock().getLocation());
            event.setCancelled(true);
        }
        if (event.getBlock().getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNoteblockPowered(final GenericGameEvent event) {
        Block block = event.getLocation().getBlock();
        NamespacedKey eventKey = NamespacedKey.minecraft("note_block_play");
        Location eLoc = block.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        // This GameEvent only exists in 1.19
        // If server is 1.18 check if its there and if not return
        // If 1.19 we can check if this event is fired
        if (!GameEvent.values().contains(GameEvent.getByKey(eventKey))) return;
        if (block.getType() != Material.NOTE_BLOCK || event.getEvent() != GameEvent.getByKey(eventKey)) return;
        NoteBlock data = (NoteBlock) block.getBlockData().clone();
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> block.setBlockData(data, false), 1L);
    }

    public void updateAndCheck(final Location loc) {
        final Block block = loc.add(0, 1, 0).getBlock();
        if (block.getType() == Material.NOTE_BLOCK)
            block.getState().update(true, true);
        final Location nextBlock = block.getLocation().add(0, 1, 0);
        if (nextBlock.getBlock().getType() == Material.NOTE_BLOCK)
            updateAndCheck(block.getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;
        NoteBlockMechanic mechanic = (NoteBlockMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(belowPlaced, blockFace)) {
            event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractNoteBlock(OraxenNoteBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = event.getNoteBlockMechanic();
        if (mechanic == null) return;

        if (!player.isSneaking()) {
            if (mechanic.hasClickActions()) {
                mechanic.runClickActions(player);
                event.setCancelled(true);
            }

            if (mechanic.isStorage()) {
                StorageMechanic storageMechanic = mechanic.getStorage();
                switch (storageMechanic.getStorageType()) {
                    case STORAGE, SHULKER -> storageMechanic.openStorage(block, player);
                    case PERSONAL -> storageMechanic.openPersonalStorage(player);
                    case DISPOSAL -> storageMechanic.openDisposal(player, block.getLocation());
                    case ENDERCHEST -> player.openInventory(player.getEnderChest());
                }
                event.setCancelled(true);
            }
        }

    }

    // TODO Make this function less of a clusterfuck and more readable
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceAgainstNoteBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK)
            return;

        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
        if (noteBlockMechanic == null) return;
        if (noteBlockMechanic.isDirectional())
            noteBlockMechanic = noteBlockMechanic.getDirectional().getParentBlockMechanic(noteBlockMechanic);

        OraxenNoteBlockInteractEvent noteBlockInteractEvent = new OraxenNoteBlockInteractEvent(noteBlockMechanic, block, event.getItem(), event.getPlayer(), event.getBlockFace());
        OraxenPlugin.get().getServer().getPluginManager().callEvent(noteBlockInteractEvent);
        if (noteBlockInteractEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (item == null) return;

        Block relative = block.getRelative(event.getBlockFace());
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (type == Material.BUCKET && relative.isLiquid()) {
            final Sound sound;
            if (relative.getType() == Material.WATER) sound = Sound.ITEM_BUCKET_FILL;
            else sound = Sound.valueOf("ITEM_BUCKET_FILL_" + relative.getType());

            if (player.getGameMode() != GameMode.CREATIVE)
                item.setType(Objects.requireNonNull(Material.getMaterial(relative.getType() + "_BUCKET")));

            player.playSound(relative.getLocation(), sound, 1.0f, 1.0f);
            relative.setType(Material.AIR, true);
            return;
        }

        final boolean bucketCheck = type.toString().endsWith("_BUCKET");
        final String bucketBlock = type.toString().replace("_BUCKET", "");
        EntityType bucketEntity;
        try {
            bucketEntity = EntityType.valueOf(bucketBlock);
        } catch (IllegalArgumentException e) {
            bucketEntity = null;
        }

        if (bucketCheck && type != Material.MILK_BUCKET) {
            if (bucketEntity == null)
                type = Material.getMaterial(bucketBlock);
            else {
                type = Material.WATER;
                player.getWorld().spawnEntity(relative.getLocation().add(0.5, 0.0, 0.5), bucketEntity);
            }
        }

        if (type == null) return;
        if (type.hasGravity() && relative.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
            BlockData data = Bukkit.createBlockData(type);
            if (type.toString().endsWith("ANVIL")) {
                ((Directional) data).setFacing(getAnvilFacing(event.getBlockFace()));
            }
            block.getWorld().spawnFallingBlock(relative.getLocation().add(0.5, 0, 0.5), data);
            return;
        }

        if (type.isBlock())
            makePlayerPlaceBlock(player, event.getHand(), item, block, event.getBlockFace(), Bukkit.createBlockData(type));
    }

    // If block is not a custom block, play the correct sound according to the below block or default
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(final NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO) event.setCancelled(true);
        else {
            if (instrumentMap.isEmpty()) instrumentMap = getInstrumentMap();
            String blockType = event.getBlock().getRelative(BlockFace.DOWN).getType().toString().toLowerCase();
            Instrument fakeInstrument = instrumentMap.entrySet().stream().filter(e -> e.getValue().contains(blockType)).map(Map.Entry::getKey).findFirst().orElse(Instrument.PIANO);
            // This is deprecated, but seems to be without reason
            event.setInstrument(fakeInstrument);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        if (block.getType() != Material.NOTE_BLOCK || !event.isDropItems()) return;

        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (block.getType() != Material.NOTE_BLOCK || !event.isDropItems()) return;
        if (mechanic.isDirectional())
            mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);

        OraxenNoteBlockBreakEvent noteBlockBreakEvent = new OraxenNoteBlockBreakEvent(mechanic, block, player);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(noteBlockBreakEvent);
        if (noteBlockBreakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (mechanic.hasLight())
            WrappedLightAPI.removeBlockLight(block.getLocation());
        if (player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().spawns(block.getLocation(), player.getInventory().getItemInMainHand());
        if (mechanic.isStorage() && mechanic.getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            mechanic.getStorage().dropStorageContent(block);
        }
        event.setDropItems(false);
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.NOTE_BLOCK)).toList();
        blockList.forEach(block -> {
            NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
            if (mechanic == null) return;
            if (mechanic.isDirectional())
                mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);
            if (mechanic.isStorage()) //TODO Should this drop items on explosions?
                mechanic.getStorage().dropStorageContent(block);

            mechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            block.setType(Material.AIR, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || placedAgainst == null) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (placedAgainst.getType().isInteractable() && placedAgainst.getType() != Material.NOTE_BLOCK) return;

        // determines the new block data of the block
        NoteBlockMechanic mechanic = (NoteBlockMechanic) factory.getMechanic(itemID);
        int customVariation = mechanic.getCustomVariation();
        BlockFace face = event.getBlockFace();

        if (mechanic.isDirectional()) {
            DirectionalBlock directional = mechanic.getDirectional();
            if (!directional.isParentBlock())
                directional = directional.getParentBlockMechanic(mechanic).getDirectional();

            customVariation = directional.getDirectionVariation(face, player);
        }

        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(), placedAgainst, face, NoteBlockMechanicFactory.createNoteBlockData(customVariation));
        if (placedBlock != null) {
            if (mechanic.hasLight())
                WrappedLightAPI.createBlockLight(placedBlock.getLocation(), mechanic.getLight());

            if (mechanic.hasDryout() && mechanic.getDryout().isFarmBlock()) {
                BlockHelpers.getPDC(placedBlock).set(FARMBLOCK_KEY, PersistentDataType.STRING, mechanic.getItemID());
            }

            if (mechanic.isStorage() && mechanic.getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
                BlockHelpers.getPDC(placedBlock).set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetFire(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() != BlockFace.UP) return;
        if (item == null) return;

        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (mechanic.isDirectional())
            mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);

        if (!mechanic.canIgnite()) return;
        if (item.getType() != Material.FLINT_AND_STEEL && item.getType() != Material.FIRE_CHARGE) return;
        BlockIgniteEvent igniteEvent = new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, event.getPlayer());
        Bukkit.getPluginManager().callEvent(igniteEvent);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(final BlockIgniteEvent event) {
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (block.getType() != Material.NOTE_BLOCK || mechanic == null) return;
        if (!mechanic.canIgnite()) event.setCancelled(true);

        block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
        block.getRelative(BlockFace.UP).setType(Material.FIRE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(final BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_WOOD_PLACE) return;
        if (getNoteBlockMechanic(placed) != null || placed.getType() == Material.MUSHROOM_STEM) return;

        if (placed.getType() == Material.NOTE_BLOCK && !OraxenItems.exists(event.getItemInHand()))
            placed.setBlockData(Bukkit.createBlockData(Material.NOTE_BLOCK), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.NOTE_BLOCK) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
            if (noteBlockMechanic == null) return;

            ItemStack item;
            if (noteBlockMechanic.isDirectional())
                item = OraxenItems.getItemById(noteBlockMechanic.getDirectional().getParentBlock()).build();
            else
                item = OraxenItems.getItemById(noteBlockMechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), OraxenItems.getIdByItem(item))) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(item);
        }
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.NOTE_BLOCK)
                    return false;

                NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
                if (noteBlockMechanic == null) return false;

                if (noteBlockMechanic.isDirectional())
                    noteBlockMechanic = noteBlockMechanic.getDirectional().getParentBlockMechanic(noteBlockMechanic);

                return noteBlockMechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
                if (noteBlockMechanic == null) return 0;
                if (noteBlockMechanic.isDirectional())
                    noteBlockMechanic = noteBlockMechanic.getDirectional().getParentBlockMechanic(noteBlockMechanic);

                final long period = noteBlockMechanic.getPeriod();
                double modifier = 1;
                if (noteBlockMechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = noteBlockMechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    private Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                       final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final String sound;
        final Material type = placedAgainst.getType();

        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;

        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
        }

        final BlockData curentBlockData = target.getBlockData();
        final boolean isFlowing = (newBlock.getMaterial() == Material.WATER || newBlock.getMaterial() == Material.LAVA);
        target.setBlockData(newBlock, isFlowing);
        final BlockState currentBlockState = target.getState();
        final NoteBlockMechanic againstMechanic = getNoteBlockMechanic(placedAgainst);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (BlockHelpers.correctAllBlockStates(target, player, face, item)) blockPlaceEvent.setCancelled(true);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (againstMechanic != null && (againstMechanic.isStorage() || againstMechanic.hasClickActions()))
            blockPlaceEvent.setCancelled(true);
        if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            blockPlaceEvent.setCancelled(true);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        final OraxenNoteBlockPlaceEvent oraxenPlaceEvent = new OraxenNoteBlockPlaceEvent(getNoteBlockMechanic(target), target, player);
        Bukkit.getPluginManager().callEvent(oraxenPlaceEvent);
        if (oraxenPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        if (isFlowing) {
            if (newBlock.getMaterial() == Material.WATER) sound = "item.bucket.empty";
            else sound = "item.bucket.empty_" + newBlock.getMaterial().toString().toLowerCase();
        } else sound = null;

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (item.getType().toString().toLowerCase().contains("bucket")) item.setType(Material.BUCKET);
            else item.setAmount(item.getAmount() - 1);
        }

        if (sound != null)
            BlockHelpers.playCustomBlockSound(target.getLocation(), sound, SoundCategory.BLOCKS, 0.8f, 0.8f);
        Utils.sendAnimation(player, hand);

        return target;
    }

    public static NoteBlockMechanic getNoteBlockMechanic(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;
        final NoteBlock noteblock = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteblock.getInstrument().getType()) * 25
                        + noteblock.getNote().getId() + (noteblock.isPowered() ? 400 : 0) - 26);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(NoteBlock noteblock) {
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteblock.getInstrument().getType()) * 25
                        + noteblock.getNote().getId() + (noteblock.isPowered() ? 400 : 0) - 26);
    }

    // Used to determine what instrument to use when playing a note depending on below block
    public static Map<Instrument, List<String>> instrumentMap = new HashMap<>();
    private static Map<Instrument, List<String>> getInstrumentMap() {
        Map<Instrument, List<String>> map = new HashMap<>();
        map.put(Instrument.BELL, List.of("gold_block"));
        map.put(Instrument.BASS_DRUM, Arrays.asList("stone", "netherrack", "bedrock", "observer", "coral", "obsidian", "anchor", "quartz"));
        map.put(Instrument.FLUTE, List.of("clay"));
        map.put(Instrument.CHIME, List.of("packed_ice"));
        map.put(Instrument.GUITAR, List.of("wool"));
        map.put(Instrument.XYLOPHONE, List.of("bone_block"));
        map.put(Instrument.IRON_XYLOPHONE, List.of("iron_block"));
        map.put(Instrument.COW_BELL, List.of("soul_sand"));
        map.put(Instrument.DIDGERIDOO, List.of("pumpkin"));
        map.put(Instrument.BIT, List.of("emerald_block"));
        map.put(Instrument.BANJO, List.of("hay_bale"));
        map.put(Instrument.PLING, List.of("glowstone"));
        map.put(Instrument.BASS_GUITAR, List.of("wood"));
        map.put(Instrument.SNARE_DRUM, Arrays.asList("sand", "gravel", "concrete_powder", "soul_soil"));
        map.put(Instrument.STICKS, Arrays.asList("glass", "sea_lantern", "beacon"));

        return map;
    }
}
