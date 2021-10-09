package me.chrisumb.customentity.listeners;

import me.chrisumb.customentity.CustomEntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class CustomEntityListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        CustomEntityType<Entity> customEntityType = getCustomEntityType(entity);

        if (customEntityType == null) {
            return;
        }

        customEntityType.onRightClick(entity, player, event);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();

        CustomEntityType<Entity> customEntityType = getCustomEntityType(entity);

        if (customEntityType == null) {
            return;
        }

        customEntityType.onDamage(entity, event);

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        CustomEntityType<Entity> damagedCustomEntityType = getCustomEntityType(entity);

        if (damagedCustomEntityType != null) {
            damagedCustomEntityType.onDamageByEntity(entity, damager, event);
        }

        CustomEntityType<Entity> damagerCustomEntityType = getCustomEntityType(damager);

        if (damagerCustomEntityType != null) {
            damagerCustomEntityType.onDamageToEntity(damager, entity, event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        CustomEntityType<Entity> customEntityType = getCustomEntityType(entity);

        if (customEntityType == null) {
            return;
        }

        customEntityType.onDeath(entity, event);
    }

    @Nullable
    private CustomEntityType<Entity> getCustomEntityType(Entity entity) {
        CustomEntityType<Entity> customEntityType = (CustomEntityType<Entity>) CustomEntityType.get(entity);

        if (customEntityType == null) {
            return null;
        }

        Class<? extends Entity> internalType = customEntityType.getInternalEntityClass();

        if (!internalType.isInstance(entity)) {
            return null;
        }

        return customEntityType;
    }
}
