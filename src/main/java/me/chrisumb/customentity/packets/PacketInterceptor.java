package me.chrisumb.customentity.packets;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import me.chrisumb.customentity.CustomEntityType;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * This is responsible for masking the custom entities as {@link EntityPlayer} for clients.
 */
public final class PacketInterceptor extends ChannelDuplexHandler {

    private final Player player;
    private final CraftPlayer craftPlayer;

    /**
     * @param player The player to handle the packet interception for.
     */
    public PacketInterceptor(Player player) {
        this.player = player;
        this.craftPlayer = (CraftPlayer) player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Packet<?> packet)) {
            return;
        }

        if (packet instanceof PacketPlayOutSpawnEntityLiving spawnEntityLivingPacket) {
            int entityID = spawnEntityLivingPacket.b();
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

            EntityType displayType = customEntityType.getDisplayType();

            if (displayType == EntityType.PLAYER) {

                //TODO: Lift GameProfile stuff into custom entity, as well as skins.
                EntityPlayer fakePlayer = new EntityPlayer(
                        minecraftServer,
                        entity.getWorld().getMinecraftWorld(),
                        new GameProfile(UUID.randomUUID(), "custom-entity")
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

                super.write(ctx, infoPacket, promise);
                super.write(ctx, spawnPacket, new DefaultChannelPromise(ctx.channel()));

                //TODO: Skins.

            } else {

                CraftWorld world = (CraftWorld) player.getWorld();
                Location location = new Location(world, entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
                //TODO: Research `randomizeData` and see if this might need to be configurable per CustomEntityType.
                Entity fakeEntity = world.createEntity(location, customEntityType.getDisplayEntityClass(), false);
                PacketPlayOutSpawnEntity spawnPacket = new PacketPlayOutSpawnEntity(fakeEntity);

                super.write(ctx, spawnPacket, promise);
            }

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

        //If it reaches this point, we didn't want to intercept or modify this packet, so we call super.write
        //to make sure it still gets to the client.
        super.write(ctx, msg, promise);
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
}
