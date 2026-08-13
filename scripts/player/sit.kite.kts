val mm = MiniMessage.miniMessage()
val sittingPlayers = java.util.concurrent.ConcurrentHashMap<java.util.UUID, ArmorStand>()

val sittableBlocks = setOf(
    "STAIRS", "SLAB"
)

fun isSittable(block: Block): Boolean {
    val name = block.type.name
    return sittableBlocks.any { name.contains(it) } && block.type != Material.AIR
}

on<PlayerInteractEvent> { event ->
    if (event.action != Action.RIGHT_CLICK_BLOCK) return@on
    val player = event.player
    if (player.isSneaking) return@on
    if (event.hand != EquipmentSlot.HAND) return@on
    if (player.inventory.itemInMainHand.type != Material.AIR) return@on
    if (sittingPlayers.containsKey(player.uniqueId)) return@on

    val block = event.clickedBlock ?: return@on
    if (!isSittable(block)) return@on
    if (!player.hasPermission("sit.use")) return@on

    val loc = block.location.add(0.5, 0.0, 0.5)
    val stand = block.world.spawn(loc, ArmorStand::class.java) { entity ->
        entity.isVisible = false
        entity.setGravity(false)
        entity.isSmall = true
        entity.isMarker = true
        entity.isInvulnerable = true
        entity.isPersistent = false
    }
    stand.addPassenger(player)
    sittingPlayers[player.uniqueId] = stand
}

on<org.spigotmc.event.entity.EntityDismountEvent> { event ->
    val player = event.entity as? Player ?: return@on
    val stand = sittingPlayers.remove(player.uniqueId) ?: return@on
    server.scheduler.runTask(1L) {
        stand.remove()
        player.teleport(player.location.add(0.0, 0.5, 0.0))
    }
}

on<PlayerQuitEvent> { event ->
    val stand = sittingPlayers.remove(event.player.uniqueId) ?: return@on
    stand.remove()
}

onUnload {
    sittingPlayers.values.forEach { it.remove() }
    sittingPlayers.clear()
}
