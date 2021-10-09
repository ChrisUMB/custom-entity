package me.chrisumb.customentitytest;

import me.chrisumb.customentity.CustomEntityType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class CustomEntityTestPlugin extends JavaPlugin {

    public static final ShopkeeperEntityType FOOD_SHOPKEEPER = new ShopkeeperEntityType("food", "Buy my food!");
    public static final ShopkeeperEntityType REDSTONE_SHOPKEEPER = new ShopkeeperEntityType("redstone", "I sell redstone.");

    @Override
    public void onEnable() {
        getLogger().info("Registering our test custom entities...");
        CustomEntityType.register(FOOD_SHOPKEEPER);
        CustomEntityType.register(REDSTONE_SHOPKEEPER);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        FOOD_SHOPKEEPER.spawn(player.getLocation());
        return true;
    }
}
