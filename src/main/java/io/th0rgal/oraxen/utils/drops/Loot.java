package io.th0rgal.oraxen.utils.drops;

import io.lumine.mythiccrucible.MythicCrucible;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Loot {

    private ItemStack itemStack;
    private final int probability;
    private final int maxAmount;
    private LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config) {
        this.probability = config.containsKey("probability") ? (int) (1D / (double) config.get("probability")) : 1;
        this.maxAmount = config.containsKey("max_amount") ? (int) config.get("max_amount") : 1;
        this.config = config;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.maxAmount = 1;
    }

    public Loot(ItemStack itemStack, double probability, int maxAmount) {
        this.itemStack = itemStack;
        this.probability = (int) (1D / probability);
        this.maxAmount = maxAmount;
    }

    private ItemStack getItemStack() {
        if (itemStack != null) return itemStack;

        if (config.containsKey("oraxen_item")) {
            String itemId = config.get("oraxen_item").toString();
            itemStack = OraxenItems.getItemById(itemId).build();
        } else if (config.containsKey("crucible_item")) {
            String crucibleID = config.get("crucible_item").toString();
            itemStack = MythicCrucible.core().getItemManager().getItemStack(crucibleID);
        } else if (config.containsKey("minecraft_type")) {
            String itemType = config.get("minecraft_type").toString();
            Material material = Material.getMaterial(itemType);
            if (material == null) return null;
            itemStack = new ItemStack(material);
        } else itemStack = (ItemStack) config.get("minecraft_item");
        return itemStack;
    }

    public void dropNaturally(Location location, int amountMultiplier) {
        if (ThreadLocalRandom.current().nextInt(probability) == 0)
            dropItems(location, amountMultiplier);
    }

    private void dropItems(Location location, int amountMultiplier) {
        ItemStack stack = getItemStack().clone();
        stack.setAmount(stack.getAmount() * amountMultiplier);
        for (int i = 0; i < maxAmount; i++)
            location.getWorld().dropItemNaturally(location, stack);
    }
}
