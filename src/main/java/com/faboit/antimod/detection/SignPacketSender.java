package com.faboit.antimod.detection;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.faboit.antimod.AntiMod;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Sends the packet sequence that primes a "phantom sign" on the client
 * and forces the sign editor open so the client will echo back the
 * translated (or fallback) text.
 *
 * Sequence per detection:
 *   1. BLOCK_CHANGE     - claim a sign now exists at the phantom position
 *   2. TILE_ENTITY_DATA - fill the sign with translation-key components
 *   3. OPEN_SIGN_EDITOR - trick the client into opening the sign editor
 *   4. CLOSE_WINDOW     - after a tick, force-close so the client submits
 *                         its current sign text via UPDATE_SIGN
 */
public final class SignPacketSender {

    private final AntiMod plugin;
    private final ProtocolManager protocol;

    private volatile Object cachedSignBlockEntityType;

    public SignPacketSender(AntiMod plugin) {
        this.plugin = plugin;
        this.protocol = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Sends the full detection sequence to the player for the given session.
     * The session must already contain its line-to-key mappings.
     */
    public void send(Player player, DetectionSession session) {
        try {
            int x = plugin.getConfigManager().getSignX();
            int y = plugin.getConfigManager().getSignY();
            int z = plugin.getConfigManager().getSignZ();
            BlockPosition pos = new BlockPosition(x, y, z);

            // 1. Block change - convince the client a sign exists here
            protocol.sendServerPacket(player, buildBlockChange(pos));

            // 2. Block entity data - the sign's text with translation components
            protocol.sendServerPacket(player, buildBlockEntityData(pos, session));

            // 3. Open sign editor
            protocol.sendServerPacket(player, buildOpenSignEditor(pos));

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

    private void sendCloseWindow(Player player) {
        try {
            PacketContainer close = protocol.createPacket(PacketType.Play.Server.CLOSE_WINDOW);
            // The container id when closing the sign editor: it's 0 for
            // the sign editor (which behaves like the player inventory
            // slot 0 from a container perspective).
            StructureModifier<Integer> ints = close.getIntegers();
            if (ints.size() > 0) {
                ints.write(0, 0);
            }
            protocol.sendServerPacket(player, close);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to send close-window to " + player.getName(), e);
        }
    }

    // ------------------------------------------------------------------
    //   Packet builders
    // ------------------------------------------------------------------

    private PacketContainer buildBlockChange(BlockPosition pos) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, pos);
        // Use OAK_SIGN as the block; the client's sign editor doesn't
        // care about the specific wood type as long as it's a sign.
        packet.getBlockData().write(0, WrappedBlockData.createData(Material.OAK_SIGN));
        return packet;
    }

    private PacketContainer buildBlockEntityData(BlockPosition pos, DetectionSession session) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

        // Position
        packet.getBlockPositionModifier().write(0, pos);

        // Block entity type - resolve minecraft:sign BlockEntityType.
        // ProtocolLib's modifier for this field takes an NMS BlockEntityType
        // instance directly.
        Object signType = resolveSignBlockEntityType();
        if (signType != null) {
            try {
                StructureModifier<Object> raw = packet.getModifier();
                // The block entity type field is the second field of the
                // packet (index 1) in vanilla layout: [BlockPos, BlockEntityType, NBT].
                for (int i = 0; i < raw.size(); i++) {
                    Object current = raw.read(i);
                    if (current != null && current.getClass().getName().endsWith("BlockEntityType")) {
                        raw.write(i, signType);
                        break;
                    }
                }
                // Fallback: try writing to index 1 blindly if no match.
                if (raw.size() > 1) {
                    try { raw.write(1, signType); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE,
                        "Couldn't set BlockEntityType via reflection; relying on default.", e);
            }
        }

        // NBT payload
        NbtCompound tag = buildSignNbt(session);
        packet.getNbtModifier().write(0, tag);

        return packet;
    }

    private PacketContainer buildOpenSignEditor(BlockPosition pos) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        packet.getBlockPositionModifier().write(0, pos);
        // In 1.20+ this packet has a boolean 'is_front_text'. Set true.
        StructureModifier<Boolean> booleans = packet.getBooleans();
        if (booleans.size() > 0) {
            booleans.write(0, true);
        }
        return packet;
    }

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
    private NbtCompound buildSignNbt(DetectionSession session) {
        NbtCompound tag = NbtFactory.ofCompound("");

        NbtCompound frontText = NbtFactory.ofCompound("front_text");
        frontText.put("color", "black");
        frontText.put("has_glowing_text", (byte) 0);
        frontText.put(buildMessagesList("messages", session, session.getFallbackMarker()));
        tag.put(frontText);

        NbtCompound backText = NbtFactory.ofCompound("back_text");
        backText.put("color", "black");
        backText.put("has_glowing_text", (byte) 0);
        backText.put(buildEmptyMessagesList("messages"));
        tag.put(backText);

        tag.put("is_waxed", (byte) 0);
        return tag;
    }

    @SuppressWarnings("unchecked")
    private NbtList<NbtBase<?>> buildMessagesList(String name,
                                                  DetectionSession session,
                                                  String fallback) {
        List<NbtBase<?>> entries = new ArrayList<>(4);
        for (int line = 0; line < 4; line++) {
            NbtCompound msg = NbtFactory.ofCompound("");
            if (session.hasLine(line)) {
                msg.put("translate", session.getKeyAt(line));
                msg.put("fallback", fallback);
            } else {
                msg.put("text", "");
            }
            entries.add((NbtBase<?>) (NbtBase) msg);
        }
        return (NbtList<NbtBase<?>>) (NbtList) NbtFactory.ofList(name, entries);
    }

    @SuppressWarnings("unchecked")
    private NbtList<NbtBase<?>> buildEmptyMessagesList(String name) {
        List<NbtBase<?>> entries = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            NbtCompound msg = NbtFactory.ofCompound("");
            msg.put("text", "");
            entries.add((NbtBase<?>) (NbtBase) msg);
        }
        return (NbtList<NbtBase<?>>) (NbtList) NbtFactory.ofList(name, entries);
    }

    /**
     * Resolves the NMS BlockEntityType.SIGN singleton via reflection.
     * Cached for reuse.
     */
    private Object resolveSignBlockEntityType() {
        if (cachedSignBlockEntityType != null) return cachedSignBlockEntityType;
        try {
            Class<?> clazz = MinecraftReflection.getMinecraftClass(
                    "world.level.block.entity.BlockEntityType",
                    "world.level.block.entity.TileEntityTypes");
            for (Field f : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && f.getType().equals(clazz)
                        && f.getName().equalsIgnoreCase("SIGN")) {
                    f.setAccessible(true);
                    cachedSignBlockEntityType = f.get(null);
                    return cachedSignBlockEntityType;
                }
            }
            // Fallback: search by name pattern
            for (Field f : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && f.getName().toUpperCase().contains("SIGN")
                        && !f.getName().toUpperCase().contains("HANGING")) {
                    f.setAccessible(true);
                    cachedSignBlockEntityType = f.get(null);
                    return cachedSignBlockEntityType;
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not resolve NMS BlockEntityType.SIGN via reflection.", t);
        }
        return null;
    }
}
