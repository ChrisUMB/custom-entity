package me.chrisumb.customentity;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.chrisumb.customentity.listeners.CustomEntityListener;
import me.chrisumb.customentity.listeners.PlayerPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @param <T> The type of {@link Entity} that will be the true, internal, server understood representation.
 */
public abstract class CustomEntityType<T extends Entity> {

    /**
     * The {@link JavaPlugin} retrieved through the ClassLoader. This is used to register the listeners and
     * for handling persistent data on the entities.
     */
    private static JavaPlugin plugin = null;

    /**
     * The {@link NamespacedKey} used for interfacing with {@link PersistentDataContainer}.
     */
    private static NamespacedKey entityKey = null;

    /**
     * The internal registry of {@link CustomEntityType}'s as a {@link ConcurrentHashMap} for safe multi-threaded access.
     */
    @NotNull
    private static final Map<String, CustomEntityType<?>> REGISTRY = new ConcurrentHashMap<>();

    @NotNull
    private final String id;

    @Nullable
    private final Skin skin;

    @NotNull
    private final Class<T> internalEntityClass;

    @NotNull
    private final EntityType internalType;

    @NotNull
    private final Class<? extends Entity> displayEntityClass;

    @NotNull
    private final EntityType displayType;

    /**
     * Neither the internal nor display can be non-spawnable entities.<br>
     * You can verify if the entity you are using us spawnable by checking {@link EntityType}'s "independent" variable, it should be true/unset.
     *
     * @param id                  The ID to be used in the registry.
     * @param skin                The Skin to use for the display, only works if  the display entity is a {@link Player}.
     * @param internalEntityClass The {@link Class<T> class} of the {@link Entity} type to be truly created on the server.
     * @param displayEntityClass  The {@link Class<T> class} of the {@link Entity} type to be sent to the client.
     */
    public CustomEntityType(
            @NotNull String id,
            @Nullable Skin skin,
            @NotNull Class<T> internalEntityClass,
            @NotNull Class<? extends Entity> displayEntityClass
    ) {
        this.id = id;
        this.skin = skin;
        this.internalEntityClass = internalEntityClass;
        this.displayEntityClass = displayEntityClass;

        EntityType internalType = getEntityType(internalEntityClass);

        if (internalType == null) {
            throw new IllegalArgumentException(
                    "CustomEntityType[%s] Internal Class<Entity> \"%s\" has no EntityType.".formatted(
                            this.id,
                            internalEntityClass
                    )
            );
        }

        this.internalType = internalType;

        if (!this.internalType.isSpawnable()) {
            throw new IllegalArgumentException(
                    "CustomEntityType[%s] Internal EntityType \"%s\" is not spawnable.".formatted(
                            this.id,
                            internalType
                    )
            );
        }

        EntityType displayType = getEntityType(displayEntityClass);

        if (displayType == null) {
            throw new IllegalArgumentException(
                    "CustomEntityType[%s] Display Class<Entity> \"%s\" has no EntityType.".formatted(
                            this.id,
                            displayEntityClass
                    )
            );
        }

        this.displayType = displayType;
    }

    /**
     * Neither the internal nor display can be non-spawnable entities.<br>
     * You can verify if the entity you are using us spawnable by checking {@link EntityType}'s "independent" variable, it should be true/unset.<br><br>
     * Defaults the {@link Skin} to null.
     *
     * @param id                  The ID to be used in the registry.
     * @param internalEntityClass The {@link Class<T> class} of the {@link Entity} type to be truly created on the server.
     * @param displayEntityClass  The {@link Class<T> class} of the {@link Entity} type to be sent to the client.
     */
    public CustomEntityType(
            @NotNull String id,
            @NotNull Class<T> internalEntityClass,
            @NotNull Class<? extends Entity> displayEntityClass
    ) {
        this(id, null, internalEntityClass, displayEntityClass);
    }

    @NotNull
    public String getID() {
        return id;
    }

    @NotNull
    public EntityType getInternalType() {
        return internalType;
    }

    @NotNull
    public EntityType getDisplayType() {
        return displayType;
    }

    @NotNull
    public Class<? extends Entity> getInternalEntityClass() {
        return internalEntityClass;
    }

    @NotNull
    public Class<? extends Entity> getDisplayEntityClass() {
        return displayEntityClass;
    }

    @Nullable
    public Skin getSkin() {
        return skin;
    }

    /**
     * This will be called whenever this {@link CustomEntityType} is right-clicked by a {@link Player}.<br>
     * <b>Note</b>: This will be called twice for each hand in the {@link PlayerInteractAtEntityEvent event}.<br><br>
     * For most cases, it's fine to check if {@link PlayerInteractAtEntityEvent event}.getHand() == {@link EquipmentSlot EquipmentSlot.HAND}.
     *
     * @param entity The {@link T entity} that was right-clicked.
     * @param player The {@link Player} that right-clicked the entity.
     * @param event  The {@link PlayerInteractAtEntityEvent event} instance.
     */
    public void onRightClick(@NotNull T entity, @NotNull Player player, @NotNull PlayerInteractAtEntityEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} is damaged.
     *
     * @param entity The {@link T entity} that was damaged.
     * @param event  The {@link EntityDamageEvent event} instance.
     */
    public void onDamage(@NotNull T entity, @NotNull EntityDamageEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} deals damage to another {@link Entity}.
     *
     * @param entity  The {@link T entity} that dealt damage.
     * @param damaged The {@link Entity entity} that was damaged.
     * @param event   The {@link EntityDamageByEntityEvent event} instance.
     */
    public void onDamageToEntity(@NotNull T entity, @NotNull Entity damaged, @NotNull EntityDamageByEntityEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} is damaged by another {@link Entity}.
     *
     * @param entity  The {@link T entity} that was damaged.
     * @param damager the {@link Entity entity} that dealt damage.
     * @param event   The {@link EntityDamageByEntityEvent event} instance.
     */
    public void onDamageByEntity(@NotNull T entity, @NotNull Entity damager, @NotNull EntityDamageByEntityEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} gets damaged by a {@link Block}.
     *
     * @param entity The {@link T entity} that was damaged.
     * @param block  The {@link Block} that damaged the entity.
     * @param event  The {@link EntityDamageByBlockEvent event} instance.
     */
    public void onDamageByBlock(@NotNull T entity, @Nullable Block block, @NotNull EntityDamageByBlockEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} dies.
     *
     * @param entity The {@link T entity} that died.
     * @param event  The {@link EntityDeathEvent event} instance.
     */
    public void onDeath(@NotNull T entity, @NotNull EntityDeathEvent event) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} is spawned.
     *
     * @param entity The {@link T Entity} that was spawned.
     */
    public void onSpawn(@NotNull T entity) {

    }

    /**
     * This will be called whenever this {@link CustomEntityType} is spawned.
     *
     * @param entity - The {@link Entity} that was spawned.
     */
    public void onPreSpawn(@NotNull T entity) {

    }

    /**
     * Spawns the {@link CustomEntityType} at the given {@link Location}, and allowing for pre-spawn {@link Consumer<T>} to be passed.
     *
     * @param location         The {@link Location} to spawn the {@link T entity} at.
     * @param preSpawnFunction Passed to the world.spawn() function that gets executed before the {@link Entity} is in the world.
     * @return The {@link T entity}.
     */
    @NotNull
    public final T spawn(Location location, Consumer<T> preSpawnFunction) {
        World world = location.getWorld();
        T entity = world.spawn(location, this.internalEntityClass, CreatureSpawnEvent.SpawnReason.CUSTOM, (preEntity) -> {
            CustomEntityType.set(preEntity, this);
            preSpawnFunction.accept(preEntity);
            onPreSpawn(preEntity);
        });
        this.onSpawn(entity);
        return entity;
    }

    /**
     * Spawns the {@link CustomEntityType} at the given {@link Location}.
     *
     * @param location The{@link Location} to spawn the {@link T entity} at.
     * @return The {@link T entity}.
     */
    @NotNull
    public final T spawn(Location location) {
        return this.spawn(location, (entity) -> {
        });
    }

    /**
     * A dynamic spawn function for when you just want to spawn an entity that looks like another entity on the client.<br><br>
     * <b>Note:</b> entities spawned with this function will NOT be persistent!<br>
     * This is an experimental functionality, use at your own risk.
     *
     * @param internal The internal {@link EntityType} to use for this {@link Entity}.
     * @param display  The display {@link EntityType} that clients will see.
     * @param location The spawn {@link Location} for the entity.
     * @return The {@link Entity} spawned entity, or null if either the display or internal don't have an entity class.
     */
    public static Entity spawn(EntityType internal, EntityType display, Location location) {
        String dynamicID = internal.name() + "-" + display.name();

        if (internal.getEntityClass() == null) {
            return null;
        }

        if (display.getEntityClass() == null) {
            return null;
        }

        CustomEntityType<?> type = new CustomEntityType(dynamicID, internal.getEntityClass(), display.getEntityClass()) {
        };

        register(type);

        return location.getWorld().spawn(location, internal.getEntityClass(), (entity) -> {
            set(entity, type);
        });
    }

    /**
     * This will be invoked when register() is called and the {@link JavaPlugin plugin} is not initialized.
     * This is responsible for registering the events and constructing the key, this solely exists this way
     * to allow for static initialization of {@link CustomEntityType} outside of onEnable.
     */
    private static void initialize() {
        plugin = JavaPlugin.getProvidingPlugin(CustomEntityType.class);
        entityKey = new NamespacedKey(plugin, "custom_entity");
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new PlayerPacketListener(plugin), plugin);
        pluginManager.registerEvents(new CustomEntityListener(), plugin);
    }

    /**
     * This will register the {@link CustomEntityType} and MUST be called in the onEnable of your plugin before
     * you do anything else. This is also responsible for proper initialization of the {@link Listener listeners}.
     *
     * @param type The {@link CustomEntityType} to register.
     * @return The {@link CustomEntityType} that was overridden, if it exists, due to duplicate ID's.
     */
    @Nullable
    public static CustomEntityType<?> register(CustomEntityType<?> type) {
        if (plugin == null) {
            initialize();
        }

        return REGISTRY.put(type.getID(), type);
    }

    /**
     * Get the registered {@link CustomEntityType} for the given {@link String} ID.
     *
     * @param id The {@link String} ID of the {@link CustomEntityType} to get from the registry.
     * @return The {@link CustomEntityType} for the given ID, or null if it doesn't exist.
     */
    @Nullable
    public static CustomEntityType<?> get(String id) {
        return REGISTRY.get(id);
    }

    /**
     * A convenience method for getting the {@link CustomEntityType} from the {@link Entity} metadata.
     *
     * @param entity the {@link Entity} to get the {@link CustomEntityType} from;
     * @return The proper {@link CustomEntityType}, or null if it doesn't have one, or the ID is invalid.
     */
    @Nullable
    public static CustomEntityType<?> get(Entity entity) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (!data.has(entityKey, PersistentDataType.STRING)) {
            return null;
        }

        String customEntityID = data.get(entityKey, PersistentDataType.STRING);

        //TODO: Perhaps pass a boolean for whether this should throw an exception or something when this is returning null?
        return REGISTRY.get(customEntityID);
    }

    /**
     * A convenience method for giving an {@link Entity} a {@link CustomEntityType}.
     *
     * @param entity           The {@link Entity} to receive the {@link CustomEntityType}.
     * @param customEntityType The {@link CustomEntityType} to give the {@link Entity}.
     */
    public static void set(Entity entity, CustomEntityType<?> customEntityType) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        data.set(entityKey, PersistentDataType.STRING, customEntityType.getID());
    }

    /**
     * Convenience function for getting the {@link EntityType} from a {@link Class<Entity>}.
     *
     * @param clazz The {@link Class<Entity>} to find the {@link EntityType} for.
     * @return The appropriate {@link EntityType}, or null if not found.
     */
    @Nullable
    private static EntityType getEntityType(Class<? extends Entity> clazz) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.getEntityClass() != clazz) {
                continue;
            }

            return entityType;
        }

        return null;
    }
}
