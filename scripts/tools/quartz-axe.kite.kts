val mm = MiniMessage.miniMessage()
val axeKey = NamespacedKey(plugin, "quartz-axe")
val durabilityKey = NamespacedKey(plugin, "axe-durability")
val MAX_DURABILITY = 300
val MAX_LOGS = 64

fun isQuartzAxe(item: ItemStack): Boolean {
    return item.type == Material.NETHERITE_AXE &&
        item.itemMeta?.persistentDataContainer?.has(axeKey) == true
}

fun updateAxeLore(item: ItemStack, durability: Int) {
    val meta = item.itemMeta ?: return
    meta.lore(listOf(
        mm.deserialize("<gray>Durability: <white>$durability/$MAX_DURABILITY"),
        mm.deserialize("<gray>Fells entire trees")
    ))
    meta.persistentDataContainer.set(durabilityKey, PersistentDataType.INTEGER, durability)
    item.itemMeta = meta
}

val LOG_TYPES = Material.entries.filter {
    it.name.endsWith("_LOG") || it.name.endsWith("_WOOD") ||
    it.name == "CRIMSON_STEM" || it.name == "WARPED_STEM" ||
    it.name == "CRIMSON_HYPHAE" || it.name == "WARPED_HYPHAE"
}.toSet()

fun fellTree(start: Block, item: ItemStack): Int {
    if (start.type !in LOG_TYPES) return 0
    val logType = start.type
    val toBreak = mutableSetOf<Block>()
    val queue = ArrayDeque<Block>()
    queue.add(start)

    while (queue.isNotEmpty() && toBreak.size < MAX_LOGS) {
        val current = queue.removeFirst()
        if (current.type != logType) continue
        if (!toBreak.add(current)) continue

        for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            val neighbor = current.getRelative(dx, dy, dz)
            if (neighbor.type == logType && neighbor !in toBreak) {
                queue.add(neighbor)
            }
        }
    }

    toBreak.forEach { it.breakNaturally(item) }
    return toBreak.size
}

command("quartzaxe") {
    permission = "admin.quartzaxe"
    description = "Get a tree-felling Quartz Axe"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val item = ItemStack(Material.NETHERITE_AXE)
        val meta = item.itemMeta!!
        meta.displayName(mm.deserialize("<light_purple><bold>Quartz Axe"))
        meta.persistentDataContainer.set(axeKey, PersistentDataType.BOOLEAN, true)
        meta.persistentDataContainer.set(durabilityKey, PersistentDataType.INTEGER, MAX_DURABILITY)
        meta.isUnbreakable = true
        item.itemMeta = meta
        updateAxeLore(item, MAX_DURABILITY)
        sender.inventory.addItem(item)
        sender.sendMessage(mm.deserialize("<green>You received a Quartz Axe!"))
    }
}

on<BlockBreakEvent> { event ->
    val player = event.player
    val item = player.inventory.itemInMainHand
    if (!isQuartzAxe(item)) return@on
    val block = event.block
    if (block.type !in LOG_TYPES) return@on

    val durability = item.itemMeta?.persistentDataContainer?.get(durabilityKey, PersistentDataType.INTEGER) ?: return@on
    if (durability <= 0) {
        player.sendMessage(mm.deserialize("<red>Your axe is broken!"))
        event.isCancelled = true
        return@on
    }

    event.isCancelled = true
    val felled = fellTree(block, item)
    val newDurability = (durability - felled).coerceAtLeast(0)
    updateAxeLore(item, newDurability)

    if (newDurability <= 0) {
        player.sendMessage(mm.deserialize("<red>Your Quartz Axe broke!"))
    }
}

on<PrepareAnvilEvent> { event ->
    val first = event.inventory.getItem(0) ?: return@on
    val second = event.inventory.getItem(1)
    if (isQuartzAxe(first) || (second != null && isQuartzAxe(second))) {
        event.result = null
    }
}
