package com.faboit.antimod.detection;

import com.faboit.antimod.AntiMod;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Sends the packet sequence that primes a "phantom sign" on the client
 * and forces the sign editor open so the client will echo back the
 * translated (or fallback) text.
 *
 * Sequence per detection:
 *   1. BLOCK_CHANGE      - claim a sign now exists at the phantom position
 *   2. BLOCK_ENTITY_DATA - fill the sign with translation-key components
 *   3. OPEN_SIGN_EDITOR  - trick the client into opening the sign editor
 *   4. CLOSE_WINDOW      - after a tick, force-close so the client submits
 *                          its current sign text via UPDATE_SIGN
 *
 * Uses PacketEvents (shared netty pipeline with anti-cheats like Nemesis)
 * so we don't collide with other packet-manipulating plugins.
 */
public final class SignPacketSender {

    private final AntiMod plugin;

    public SignPacketSender(AntiMod plugin) {
        this.plugin = plugin;
    }

    /**
     * Computes the phantom sign position relative to the player.
     * Same X/Z as the player, Y offset downward by the configured amount.
     */
    public Vector3i computeSignPosition(Player player) {
        int offset = plugin.getConfigManager().getSignYOffset();
        return new Vector3i(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY() - offset,
                player.getLocation().getBlockZ()
        );
    }

    /**
     * Sends the full detection sequence to the player for the given session.
     * The session must already contain its line-to-key mappings and position.
     */
    public void send(Player player, DetectionSession session) {
        try {
            Vector3i pos = session.getSignPosition();
            ClientVersion clientVersion = clientVersionOf(player);

            // 1. Block change - convince the client a sign exists here
            int oakSignId = WrappedBlockState
                    .getDefaultState(clientVersion, StateTypes.OAK_SIGN)
                    .getGlobalId();
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerBlockChange(pos, oakSignId));

            // 2. Block entity data - the sign's text with translation components
            NBTCompound tag = buildSignNbt(session);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, tag));

            // 3. Open sign editor (boolean is "is_front_text" as of 1.20)
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerOpenSignEditor(pos, true));

            // 4. Close the window (immediately or after a small delay)
            int closeDelay = plugin.getConfigManager().getCloseDelayTicks();
            if (closeDelay <= 0) {
                sendCloseWindow(player);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> sendCloseWindow(player),
                        closeDelay);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to send detection packets for " + player.getName(), e);
        }
    }

    /**
     * Sends a BLOCK_CHANGE for AIR at the session's phantom position,
     * removing the ghost sign from the client's view.
     */
    public void sendCleanup(Player player, Vector3i pos) {
        try {
            int airId = WrappedBlockState
                    .getDefaultState(clientVersionOf(player), StateTypes.AIR)
                    .getGlobalId();
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerBlockChange(pos, airId));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to send cleanup block for " + player.getName(), e);
        }
    }

    private void sendCloseWindow(Player player) {
        try {
            // Container id 0 corresponds to the player-inventory / sign editor.
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerCloseWindow(0));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to send close-window to " + player.getName(), e);
        }
    }

    /**
     * Resolves the client's protocol version. Falls back to the server version
     * if the client version is unknown (should be rare — PacketEvents caches it
     * on login).
     */
    private ClientVersion clientVersionOf(Player player) {
        try {
            ClientVersion cv = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
            if (cv != null && cv != ClientVersion.UNKNOWN) {
                return cv;
            }
        } catch (Exception ignored) {}
        return PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    }

    // ------------------------------------------------------------------
    //   NBT builder
    // ------------------------------------------------------------------

    /**
     * Builds the sign block-entity NBT with translation components for each
     * probed key. Layout for 1.20.5+:
     *
     * {
     *   front_text: { color:"black", has_glowing_text:0, messages:[ ... ] },
     *   back_text:  { color:"black", has_glowing_text:0, messages:[ empty ] },
     *   is_waxed: 0
     * }
     *
     * Each entry in messages is a text-component compound. For probed lines:
     *   { translate:"key.mod.foo", fallback:"am_fb" }
     * For empty lines:
     *   { text:"" }
     */
    private NBTCompound buildSignNbt(DetectionSession session) {
        NBTCompound tag = new NBTCompound();

        NBTCompound frontText = new NBTCompound();
        frontText.setTag("color", new NBTString("black"));
        frontText.setTag("has_glowing_text", new NBTByte((byte) 0));
        frontText.setTag("messages", buildMessagesList(session, session.getFallbackMarker()));
        tag.setTag("front_text", frontText);

        NBTCompound backText = new NBTCompound();
        backText.setTag("color", new NBTString("black"));
        backText.setTag("has_glowing_text", new NBTByte((byte) 0));
        backText.setTag("messages", buildEmptyMessagesList());
        tag.setTag("back_text", backText);

        tag.setTag("is_waxed", new NBTByte((byte) 0));
        return tag;
    }

    private NBTList<NBTCompound> buildMessagesList(DetectionSession session, String fallback) {
        NBTList<NBTCompound> list = new NBTList<>(NBTType.COMPOUND);
        for (int line = 0; line < 4; line++) {
            NBTCompound msg = new NBTCompound();
            if (session.hasLine(line)) {
                msg.setTag("translate", new NBTString(session.getKeyAt(line)));
                msg.setTag("fallback", new NBTString(fallback));
            } else {
                msg.setTag("text", new NBTString(""));
            }
            list.addTag(msg);
        }
        return list;
    }

    private NBTList<NBTCompound> buildEmptyMessagesList() {
        NBTList<NBTCompound> list = new NBTList<>(NBTType.COMPOUND);
        for (int i = 0; i < 4; i++) {
            NBTCompound msg = new NBTCompound();
            msg.setTag("text", new NBTString(""));
            list.addTag(msg);
        }
        return list;
    }
}
