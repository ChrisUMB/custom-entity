# Custom Entity

### This project was made as a simple utility for spawning entities that look one way to the client, but are internally something else on the server.

#### Example: This is actually a zombie, with zombie AI, but looks like a player. Neat!
![Zombie -> Player Example](img/zombie-player-example.gif)

## Dependency
##### Note: You need to also have the server as a dependency for NMS access.
```groovy
repository {
    maven 'https://sparse.blue/maven'
}

dependency {
    implementation 'me.chrisumb:custom-entity:1.0'
}
```

## Usage

### This is an example of a "shopkeeper", it's a zombie that looks like a player, with a right click functionality to say a phrase. He will also follow players around and try to attack them like a zombie would.

```java
public final class ShopkeeperEntityType extends CustomEntityType<Zombie> {
    
    private final String phrase;
    /*
    This will be a zombie that looks like a player, with the skin of player "Tom1024". 

    You can do any combination of spawnable entities, not just players, and there is an alternative constructor that doesn't take a skin in case you don't want to spawn a player on the client end.
    */
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

    public String getPhrase() {
        return phrase;
    }
}
```
## Registering a custom entity
```java
private static ShopkeeperEntityType foodShopkeeper;

@Override
public void onEnable() {
    // It's instantiated here in the onEnable() because it needs access to the plugin.
    foodShopkeeper = new ShopkeeperEntityType(this, "food", "Buy my food!");
    CustomEntityType.register(foodShopkeeper);
}
```

## Spawning a custom entity
```java
foodShopkeeper.spawn(player.getLocation());
```



# Notes

I don't plan on providing continued support on this project, I just made it for 1.17.1 because that's what I use currently.

The entities created are persistent across server restarts. Some use cases I think are good are dynamic shop NPC's that have some pathfinding on their base entity, but it's also fun to just make random mobs behave like others. Try a bat that looks like a horse, it's just funny.

This heavily relies on packet interception, so it's pretty prone to cause issues. There very well may be some client side exceptions that don't kick the client, but it complains about, which I tried my best to remedy the ones I found. Furthermore, there is some asyncronous entity accessing which probably shouldn't happen, but I have no idea what else I would do.

The skin format is just `value` `\n` `signature`. You can make a `.skin` file easily enough manually by going to https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false and replacing `%s` with the UUID you get from https://api.mojang.com/users/profiles/minecraft/%s where you replace `%s` with a username.

If you want to fork this, you need to run the `setupTestServer` task, then refresh gradle so that you have the test server and the `.jar` files necessary.