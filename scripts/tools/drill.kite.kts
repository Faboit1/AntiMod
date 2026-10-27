val mm = MiniMessage.miniMessage()
val drillKey = NamespacedKey(plugin, "drill-tool")
val durabilityKey = NamespacedKey(plugin, "drill-durability")
val MAX_DURABILITY = 500

fun isDrill(item: ItemStack): Boolean {
    return item.type == Material.NETHERITE_PICKAXE &&
        item.itemMeta?.persistentDataContainer?.has(drillKey) == true
}

fun updateDrillLore(item: ItemStack, durability: Int) {
    val meta = item.itemMeta ?: return
    meta.lore(listOf(
        mm.deserialize("<gray>Durability: <white>$durability/$MAX_DURABILITY"),
        mm.deserialize("<gray>Mines a 3x3 area")
    ))
    meta.persistentDataContainer.set(durabilityKey, PersistentDataType.INTEGER, durability)
    item.itemMeta = meta
}

command("drill") {
    permission = "admin.drill"
    description = "Get a 3x3 mining drill"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val item = ItemStack(Material.NETHERITE_PICKAXE)
        val meta = item.itemMeta!!
        meta.displayName(mm.deserialize("<light_purple><bold>Quartz Drill"))
        meta.persistentDataContainer.set(drillKey, PersistentDataType.BOOLEAN, true)
        meta.persistentDataContainer.set(durabilityKey, PersistentDataType.INTEGER, MAX_DURABILITY)
        meta.isUnbreakable = true
        item.itemMeta = meta
        updateDrillLore(item, MAX_DURABILITY)
        sender.inventory.addItem(item)
        sender.sendMessage(mm.deserialize("<green>You received a Quartz Drill!"))
    }
}

on<BlockBreakEvent> { event ->
    val player = event.player
    val item = player.inventory.itemInMainHand
    if (!isDrill(item)) return@on

    val durability = item.itemMeta?.persistentDataContainer?.get(durabilityKey, PersistentDataType.INTEGER) ?: return@on
    if (durability <= 0) {
        player.sendMessage(mm.deserialize("<red>Your drill is broken!"))
        event.isCancelled = true
        return@on
    }

    val center = event.block
    val face = player.getTargetBlockFace(5) ?: return@on
    var blocksDestroyed = 0

    val offsets = (-1..1).flatMap { a -> (-1..1).map { b -> a to b } }

    for ((a, b) in offsets) {
        val target = when (face) {
            BlockFace.NORTH, BlockFace.SOUTH -> center.getRelative(a, b, 0)
            BlockFace.EAST, BlockFace.WEST -> center.getRelative(0, b, a)
            BlockFace.UP, BlockFace.DOWN -> center.getRelative(a, 0, b)
            else -> continue
        }
        if (target.type == Material.AIR || target.type == Material.BEDROCK) continue
        if (target.location == center.location) continue
        target.breakNaturally(item)
        blocksDestroyed++
    }

    val newDurability = (durability - 1 - blocksDestroyed).coerceAtLeast(0)
    updateDrillLore(item, newDurability)
    if (newDurability <= 0) {
        player.sendMessage(mm.deserialize("<red>Your drill broke!"))
    }
}

on<PlayerInteractEvent> { event ->
    val item = event.player.inventory.itemInMainHand
    if (!isDrill(item)) return@on
    if (event.action == Action.RIGHT_CLICK_BLOCK) {
        val block = event.clickedBlock ?: return@on
        if (block.type == Material.ANVIL || block.type == Material.CHIPPED_ANVIL || block.type == Material.DAMAGED_ANVIL) {
            event.isCancelled = true
        }
    }
}

on<PrepareAnvilEvent> { event ->
    val first = event.inventory.getItem(0) ?: return@on
    val second = event.inventory.getItem(1)
    if (isDrill(first) || (second != null && isDrill(second))) {
        event.result = null
    }
}
