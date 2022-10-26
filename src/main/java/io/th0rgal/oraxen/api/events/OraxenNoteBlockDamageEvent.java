package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired right before a player damages a NoteBlock.
 * If cancelled, the block will not be damaged.
 * @see NoteBlockMechanic
 */
public class OraxenNoteBlockDamageEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private final int newDamageStage;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @param mechanic The NoteBlockMechanic of this block
     * @param block The block that was damaged
     * @param newDamageStage The new damage stage of the block
     * @param player The player who damaged this block
     */
    public OraxenNoteBlockDamageEvent(NoteBlockMechanic mechanic, Block block, int newDamageStage, Player player) {
        this.mechanic = mechanic;
        this.player = player;
        this.newDamageStage = newDamageStage;
        this.block = block;
        this.isCancelled = false;
    }

    /**
     * @return The note block mechanic
     */
    public NoteBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who broke the note block
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was broken
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return The current damage stage of the block
     */
    public int getCurrentDamageStage() {
        return Math.max(newDamageStage - 1, 0);
    }

    /**
     * @return The new damage stage of the StringBlock
     */
    public int getNewDamageState() {
        return newDamageStage;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
