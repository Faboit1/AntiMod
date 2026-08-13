val mm = MiniMessage.miniMessage()

on<PrepareItemCraftEvent> { event ->
    val result = event.recipe?.result ?: return@on
    if (result.type != Material.MAP && result.type != Material.FILLED_MAP) return@on

    val matrix = event.inventory.matrix ?: return@on
    for (item in matrix) {
        if (item == null) continue
        if (item.type == Material.FILLED_MAP) {
            val mapMeta = item.itemMeta as? org.bukkit.inventory.meta.MapMeta ?: continue
            if (mapMeta.isLocked) {
                event.inventory.result = null
                val viewers = event.viewers.filterIsInstance<Player>()
                viewers.forEach { player ->
                    player.sendMessage(mm.deserialize("<red>Locked maps cannot be duplicated!"))
                }
                return@on
            }
        }
    }
}

on<InventoryClickEvent> { event ->
    val inv = event.inventory
    if (inv.type != InventoryType.CARTOGRAPHY) return@on
    val player = event.whoClicked as? Player ?: return@on

    server.scheduler.runTask(1L) {
        val firstSlot = inv.getItem(0)
        if (firstSlot != null && firstSlot.type == Material.FILLED_MAP) {
            val meta = firstSlot.itemMeta as? org.bukkit.inventory.meta.MapMeta
            if (meta?.isLocked == true) {
                inv.setItem(2, null)
                player.sendMessage(mm.deserialize("<red>Locked maps cannot be cloned in the cartography table!"))
            }
        }
    }
}
