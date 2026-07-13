package com.faboit.antimod.detection;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.faboit.antimod.AntiMod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Intercepts the client-bound-to-server UPDATE_SIGN packet.
 *
 * When we probe a player, we file a DetectionSession keyed by their UUID.
 * When the client replies with UPDATE_SIGN, we look up the session, check
 * each of the four returned lines against our stored mapping, and hand
 * detected mods off to the DetectionManager.
 *
 * We only intercept responses for our known phantom position; anything
 * else is passed through untouched.
 */
public final class SignResponseListener extends PacketAdapter {

    private final AntiMod plugin;
    private final DetectionManager manager;

    public SignResponseListener(AntiMod plugin, DetectionManager manager) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.UPDATE_SIGN);
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) return;

            BlockPosition pos = event.getPacket().getBlockPositionModifier().read(0);
            if (pos == null) return;

            // Fast-check: is this our phantom position?
            int expX = plugin.getConfigManager().getSignX();
            int expY = plugin.getConfigManager().getSignY();
            int expZ = plugin.getConfigManager().getSignZ();
            if (pos.getX() != expX || pos.getY() != expY || pos.getZ() != expZ) {
                return; // real sign - leave alone
            }

            // Cancel: this is our fake sign, we don't want vanilla handling.
            event.setCancelled(true);

            UUID uuid = player.getUniqueId();
            DetectionSession session = manager.consumeSession(uuid);
            if (session == null) return;
            session.markResponded();

            // Read the 4 lines. In 1.20+ the packet stores them as String[]
            // in the arrays modifier at index 0.
            String[] lines = event.getPacket().getStringArrays().read(0);
            if (lines == null) return;

            // Compare each line to the fallback marker; anything else means
            // the mod translated the key.
            Map<String, String> detected = new LinkedHashMap<>();
            for (int i = 0; i < lines.length && i < 4; i++) {
                if (!session.hasLine(i)) continue;
                String modId = session.getModAt(i);
                String probedKey = session.getKeyAt(i);
                String returned = lines[i];

                if (returned == null || returned.equals(session.getFallbackMarker())) {
                    continue; // fallback - mod not present
                }
                // Some clients render blank if the key doesn't exist AND there
                // is no fallback - be defensive: only flag when non-empty AND
                // not equal to the fallback.
                if (returned.isEmpty()) continue;

                // Record the detected mod and the key that gave it away.
                detected.merge(modId, probedKey, (a, b) -> a + "," + b);
            }

            if (detected.isEmpty()) return;

            // Hop back to the main thread for game actions.
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<String, String> e : detected.entrySet()) {
                    manager.handleDetection(player, e.getKey(), e.getValue());
                }
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error while processing UPDATE_SIGN packet.", e);
        }
    }
}
