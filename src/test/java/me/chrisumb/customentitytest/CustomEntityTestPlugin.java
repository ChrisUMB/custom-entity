package me.chrisumb.customentitytest;

import me.chrisumb.customentity.CustomEntityType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class CustomEntityTestPlugin extends JavaPlugin {

    public static ShopkeeperEntityType foodShopkeeper;
    public static ShopkeeperEntityType redstoneShopkeeper;
    public static final FlyingFishEntityType FLYING_FISH = new FlyingFishEntityType("flying-fish");

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "tom1024.skin").exists()) {
            getSLF4JLogger().error("Trying to load example, but tom1024.skin doesn't exist! Replace it with something else.");
        }

        // These have to be instantiated like this because they need the plugin instance.
        foodShopkeeper = new ShopkeeperEntityType(this, "food", "Buy my food!");
        redstoneShopkeeper = new ShopkeeperEntityType(this, "redstone", "I sell redstone.");

        getLogger().info("Registering our test custom entities...");
        CustomEntityType.register(foodShopkeeper);
        CustomEntityType.register(redstoneShopkeeper);
        CustomEntityType.register(FLYING_FISH);
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

        foodShopkeeper.spawn(player.getLocation());
        return true;
    }
}
