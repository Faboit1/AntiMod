val mm = MiniMessage.miniMessage()
val dupeTagKey = NamespacedKey(plugin, "item-uid")

on<EntityPickupItemEvent> { event ->
    val player = event.entity as? Player ?: return@on
    val item = event.item.itemStack
    val meta = item.itemMeta ?: return@on
    val container = meta.persistentDataContainer

    if (!container.has(dupeTagKey)) {
        container.set(dupeTagKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString())
        item.itemMeta = meta
    }
}

on<InventoryClickEvent> { event ->
    val player = event.whoClicked as? Player ?: return@on
    val current = event.currentItem ?: return@on
    if (current.type == Material.AIR) return@on

    val meta = current.itemMeta ?: return@on
    val container = meta.persistentDataContainer
    val uid = container.get(dupeTagKey, PersistentDataType.STRING) ?: return@on

    var dupeCount = 0
    for (invItem in player.inventory.contents.filterNotNull()) {
        val invMeta = invItem.itemMeta ?: continue
        val invUid = invMeta.persistentDataContainer.get(dupeTagKey, PersistentDataType.STRING) ?: continue
        if (invUid == uid && invItem !== current) {
            dupeCount++
        }
    }

    if (dupeCount > 0) {
        logger.warning("Potential dupe detected: ${player.name} has $dupeCount duplicate items with UID $uid")
        // NOTE: Original script sent a Discord webhook alert here.
        // Configure your own alerting system (e.g., a webhook URL in a config file).
    }
}
