package me.chrisumb.customentity.packets;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.chrisumb.customentity.CustomEntityType;
import me.chrisumb.customentity.Skin;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This is responsible for masking the custom entities as {@link EntityPlayer} for clients.
 */
public final class PacketInterceptor extends ChannelDuplexHandler {

    private final JavaPlugin plugin;

    private final Player player;
    private final CraftPlayer craftPlayer;

    private final IntList entityIDs = new IntArrayList();

    /**
     * @param player The player to handle the packet interception for.
     */
    public PacketInterceptor(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.craftPlayer = (CraftPlayer) player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Packet<?> packet)) {
            return;
        }

        if (packet instanceof PacketPlayOutSpawnEntityExperienceOrb xpOrbPacket) {
            int entityID = xpOrbPacket.b();
            handleEntitySpawn(ctx, msg, promise, entityID);
            return;
        }

        if (packet instanceof PacketPlayOutSpawnEntity spawnEntityPacket) {
            int entityID = spawnEntityPacket.b();
            handleEntitySpawn(ctx, msg, promise, entityID);
            return;
        }

        if (packet instanceof PacketPlayOutSpawnEntityLiving spawnEntityLivingPacket) {
            int entityID = spawnEntityLivingPacket.b();
            handleEntitySpawn(ctx, msg, promise, entityID);
            return;
        }

        if (packet instanceof PacketPlayOutEntityHeadRotation entityHeadRotationPacket) {
            Entity entity = getEntity(entityHeadRotationPacket);

            if (entity == null) {
                super.write(ctx, msg, promise);
                return;
            }

            CustomEntityType<?> customEntityType = CustomEntityType.get(entity.getBukkitEntity());

            //If the custom entity was null, we don't want this packet either.
            if (customEntityType == null) {
                super.write(ctx, msg, promise);
                return;
            }

            byte yaw = entityHeadRotationPacket.b();

            entity.setLocation(
                    entity.locX(),
                    entity.locY(),
                    entity.locZ(),
                    (yaw / 256f) * 360f,
                    entity.getXRot()
            );

            PacketPlayOutEntityTeleport entityTeleport = new PacketPlayOutEntityTeleport(entity);

            super.write(ctx, entityTeleport, promise);
            super.write(ctx, packet, new DefaultChannelPromise(ctx.channel()));
            return;
        }

        if (packet instanceof PacketPlayOutEntity.PacketPlayOutEntityLook lookPacket) {
            Entity entity = getEntity(lookPacket);

            if (entity == null) {
                super.write(ctx, msg, promise);
                return;
            }

            CustomEntityType<?> customEntityType = CustomEntityType.get(entity.getBukkitEntity());

            //If the custom entity was null, we don't want this packet either.
            if (customEntityType == null) {
                super.write(ctx, msg, promise);
            }

            //We want to ignore these packets as we manage them ourselves with Teleport packets in the HeadRotation listener.
            return;
        }

        if (packet instanceof PacketPlayOutEntityMetadata metadataPacket) {
            int entityID = metadataPacket.c();
            Entity entity = getEntity(entityID);

            if (entity == null) {
                super.write(ctx, msg, promise);
                return;
            }

            List<DataWatcher.Item<?>> watchers = metadataPacket.b();
            List<DataWatcher.Item<?>> remaining = new ArrayList<>();
            if (watchers != null) {

                DataWatcher.Item<?> customName = null;

                for (DataWatcher.Item<?> watcher : watchers) {
                    int id = watcher.a().a();
                    if (id < 15 || id > 20) {
                        remaining.add(watcher);
                    }

                    if (id == 2) {
                        customName = watcher;
                    }
                }

//                watchers.removeAll(remaining);

                if (customName != null) {
                    Optional<IChatBaseComponent> name = (Optional<IChatBaseComponent>) customName.b();

                    if (entity.getCustomNameVisible()) {
                        Scoreboard scoreboard = new Scoreboard();
                        ScoreboardTeam team = new ScoreboardTeam(scoreboard, getInvisibleName(entityID));
                        if (name.isPresent()) {
                            team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.a);
                            team.setPrefix(name.get());
                        } else {
                            team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.b);
                        }

                        team.getPlayerNameSet().add(getInvisibleName(entityID));

                        PacketPlayOutScoreboardTeam teamPacket = PacketPlayOutScoreboardTeam.a(team, false);
                        super.write(ctx, teamPacket, new DefaultChannelPromise(ctx.channel()));
                    }
                }

                if (remaining.isEmpty()) {
                    return;
                }

//                if (!remaining.isEmpty()) {
                PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
                serializer.d(entityID);
                DataWatcher.a(remaining, serializer);
                PacketPlayOutEntityMetadata newPacket = new PacketPlayOutEntityMetadata(serializer);
                super.write(ctx, newPacket, promise);
                return;
//                }
            }
        }

        if (packet instanceof PacketPlayOutEntityDestroy destroyPacket) {
            IntList destroyedEntityIDs = destroyPacket.b();

            for (int entityID : destroyedEntityIDs) {
                if (!entityIDs.contains(entityID)) {
                    continue;
                }

                Scoreboard scoreboard = new Scoreboard();
                ScoreboardTeam team = new ScoreboardTeam(scoreboard, getInvisibleName(entityID));
                PacketPlayOutScoreboardTeam removeTeamPacket = PacketPlayOutScoreboardTeam.a(team);
                super.write(ctx, removeTeamPacket, new DefaultChannelPromise(ctx.channel()));
            }

            entityIDs.removeAll(destroyedEntityIDs);
        }

        //If it reaches this point, we didn't want to intercept or modify this packet, so we call super.write
        //to make sure it still gets to the client.
        super.write(ctx, msg, promise);
    }

    private void handleEntitySpawn(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, int entityID) throws Exception {
        Entity entity = getEntity(entityID);

        //If the entity was null, we don't want anything to do with this packet.
        if (entity == null) {
            super.write(ctx, msg, promise);
            return;
        }

        MinecraftServer minecraftServer = entity.getMinecraftServer();

        if (minecraftServer == null) {
            super.write(ctx, msg, promise);
            return;
        }

        CustomEntityType<?> customEntityType = CustomEntityType.get(entity.getBukkitEntity());

        //If the custom entity was null, we don't want this packet either.
        if (customEntityType == null) {
            super.write(ctx, msg, promise);
            return;
        }

        entityIDs.add(entityID);
        EntityType displayType = customEntityType.getDisplayType();

        if (displayType == EntityType.PLAYER) {

            GameProfile profile = new GameProfile(UUID.randomUUID(), getInvisibleName(entityID));

            Skin skin = customEntityType.getSkin();
            if (skin != null) {
                profile.getProperties().put("textures", new Property("textures", skin.getValue(), skin.getSignature()));
            }

            EntityPlayer fakePlayer = new EntityPlayer(
                    minecraftServer,
                    entity.getWorld().getMinecraftWorld(),
                    profile
            );

            fakePlayer.setLocation(
                    entity.locX(), entity.locY(), entity.locZ(),
                    entity.getYRot(), entity.getXRot()
            );

            //This is the money, make the client associate all incoming packets from
            //the real, server managed entity as a fake player instead.
            fakePlayer.e(entityID);

            PacketPlayOutPlayerInfo infoPacket
                    = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, fakePlayer);

            PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(fakePlayer);

            Scoreboard scoreboard = new Scoreboard();
            ScoreboardTeam team = new ScoreboardTeam(scoreboard, getInvisibleName(entityID));
            team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.b);
            team.getPlayerNameSet().add(getInvisibleName(entityID));

            PacketPlayOutScoreboardTeam teamPacket = PacketPlayOutScoreboardTeam.a(team, true);

            super.write(ctx, infoPacket, promise);
            super.write(ctx, spawnPacket, new DefaultChannelPromise(ctx.channel()));
            super.write(ctx, teamPacket, new DefaultChannelPromise(ctx.channel()));

            //This is done to get the fake player name out of tablist. If it's not delayed, the skin won't download.
            int delay = skin == null ? 0 : 40;

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                PacketPlayOutPlayerInfo removeTablistPacket
                        = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, fakePlayer);

                sendPacket(player, removeTablistPacket);
            }, delay);

        } else {

            CraftWorld world = (CraftWorld) player.getWorld();
            Location location = new Location(world, entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
            Entity fakeEntity = world.createEntity(location, customEntityType.getDisplayEntityClass(), false);
            fakeEntity.e(entityID);

            PacketPlayOutSpawnEntity spawnPacket = new PacketPlayOutSpawnEntity(fakeEntity);

            super.write(ctx, spawnPacket, promise);
        }
    }

    private static Field entityPacketEntityIDField;
    private static Field headRotationPacketEntityIDField;

    static {
        try {
            entityPacketEntityIDField = PacketPlayOutEntity.class.getDeclaredField("a");
            entityPacketEntityIDField.setAccessible(true);

            headRotationPacketEntityIDField = PacketPlayOutEntityHeadRotation.class.getDeclaredField("a");
            headRotationPacketEntityIDField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    /**
     * A convenience method for getting the {@link Entity} by ID from the internal {@link LevelEntityGetter},
     * as this *should* be a thread safe approach to getting the entity instance.
     *
     * @param entityID The ID of the {@link Entity} to find.
     * @return The {@link Entity} if found, otherwise, null.
     */
    @Nullable
    private Entity getEntity(int entityID) {
        EntityPlayer entityPlayer = craftPlayer.getHandle();
        return ((WorldServer) entityPlayer.t).G.d().a(entityID);
    }

    @Nullable
    private Entity getEntity(PacketPlayOutEntity packet) {
        try {
            int entityID = entityPacketEntityIDField.getInt(packet);
            return getEntity(entityID);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    private Entity getEntity(PacketPlayOutEntityHeadRotation packet) {
        try {
            int entityID = headRotationPacketEntityIDField.getInt(packet);
            return getEntity(entityID);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getInvisibleName(int id) {
        return Integer.toHexString(id).replaceAll("(.)", "\u00a7$1");
    }
}
