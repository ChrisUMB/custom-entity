package me.chrisumb.customentitytest;

import com.destroystokyo.paper.entity.ai.MobGoals;
import me.chrisumb.customentity.CustomEntityType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ShopkeeperEntityType extends CustomEntityType<Zombie> {

    private final String phrase;

    public ShopkeeperEntityType(String id, String phrase) {
        super(id + "-shopkeeper", Zombie.class, Player.class);
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

        EntityEquipment equipment = entity.getEquipment();
        equipment.setItem(EquipmentSlot.HAND, new ItemStack(Material.DIAMOND_SWORD));
    }

    public String getPhrase() {
        return phrase;
    }
}
