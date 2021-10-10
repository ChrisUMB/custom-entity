package me.chrisumb.customentity.listeners;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import me.chrisumb.customentity.packets.PacketInterceptor;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This listener is responsible for injecting our {@link PacketInterceptor} into the
 * player's packet handling {@link ChannelPipeline}.
 */
public final class PlayerPacketListener implements Listener {

    private final JavaPlugin plugin;

    public PlayerPacketListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Channel channel = ((CraftPlayer) player).getHandle().b.a.k;
        ChannelPipeline pipeline = channel.pipeline();

        //Does the player's packet pipeline already have custom_entity? If so, return.
        if (pipeline.get("custom_entity") != null) {
            return;
        }

        //Does the packet handler context exist? If not, return.
        if (pipeline.context("packet_handler") == null) {
            return;
        }

        //All clear, add the interceptor before the packet_handler context.
        pipeline.addBefore("packet_handler", "custom_entity", new PacketInterceptor(plugin, player));
    }

}
