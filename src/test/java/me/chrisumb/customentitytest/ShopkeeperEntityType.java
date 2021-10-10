package me.chrisumb.customentitytest;

import me.chrisumb.customentity.CustomEntityType;
import me.chrisumb.customentity.Skin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class ShopkeeperEntityType extends CustomEntityType<Zombie> {

    private final String phrase;

    public ShopkeeperEntityType(JavaPlugin plugin, String id, String phrase) {
        super(id + "-shopkeeper", Skin.load(new File(plugin.getDataFolder(), "tom1024.skin")), Zombie.class, Player.class);
        this.phrase = phrase;
    }

    @Override
    public void onRightClick(@NotNull Zombie entity, @NotNull Player player, @NotNull PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        player.sendMessage(phrase);
    }

    @Override
    public void onSpawn(@NotNull Zombie entity) {
        entity.setSilent(true);
        entity.setShouldBurnInDay(false);

        entity.setCustomNameVisible(true);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lFood Shopkeeper"));

        EntityEquipment equipment = entity.getEquipment();
        equipment.setItem(EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SWORD));
    }

    public String getPhrase() {
        return phrase;
    }
}