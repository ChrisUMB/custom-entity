package me.chrisumb.customentitytest;

import com.destroystokyo.paper.entity.ai.MobGoals;
import me.chrisumb.customentity.CustomEntityType;
import me.chrisumb.customentity.Skin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
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
        entity.setCustomName("Food Shopkeeper");

        EntityEquipment equipment = entity.getEquipment();
        equipment.setItem(EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SWORD));
    }

    @Override
    public void onDamageByBlock(@NotNull Zombie entity, @NotNull Block block, @NotNull EntityDamageByBlockEvent event) {

    }

    public String getPhrase() {
        return phrase;
    }
}
