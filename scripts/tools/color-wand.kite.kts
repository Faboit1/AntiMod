val mm = MiniMessage.miniMessage()

val colorCycle = listOf(
    Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE, Material.MAGENTA_CONCRETE,
    Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
    Material.PINK_CONCRETE, Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
    Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
    Material.BROWN_CONCRETE, Material.GREEN_CONCRETE, Material.RED_CONCRETE,
    Material.BLACK_CONCRETE
)

fun getColorVariant(original: Material, targetColor: String): Material? {
    val baseName = original.name.split("_").dropWhile { it.lowercase() in listOf(
        "white", "orange", "magenta", "light", "yellow", "lime",
        "pink", "gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black"
    ) }.joinToString("_")
    if (baseName.isEmpty()) return null
    return Material.getMaterial("${targetColor}_$baseName")
}

on<PlayerInteractEvent> { event ->
    val player = event.player
    if (!player.isOp) return@on
    if (event.hand != EquipmentSlot.HAND) return@on
    val item = player.inventory.itemInMainHand
    if (item.type != Material.GOLDEN_AXE) return@on
    if (!item.hasItemMeta()) return@on
    val meta = item.itemMeta ?: return@on
    if (!meta.displayName().toString().contains("ColorWand", ignoreCase = true)) return@on

    val block = event.clickedBlock ?: return@on
    event.isCancelled = true

    val blockName = block.type.name
    val colorPrefix = blockName.split("_").takeWhile { it.lowercase() in listOf(
        "white", "orange", "magenta", "light", "yellow", "lime",
        "pink", "gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black"
    ) }.joinToString("_")

    if (colorPrefix.isEmpty()) {
        player.sendMessage(mm.deserialize("<red>This block doesn't have color variants!"))
        return@on
    }

    val colors = listOf(
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
        "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
        "BROWN", "GREEN", "RED", "BLACK"
    )

    val currentIndex = colors.indexOf(colorPrefix)
    if (currentIndex == -1) return@on

    val nextIndex = when (event.action) {
        Action.RIGHT_CLICK_BLOCK -> (currentIndex + 1) % colors.size
        Action.LEFT_CLICK_BLOCK -> (currentIndex - 1 + colors.size) % colors.size
        else -> return@on
    }

    val baseSuffix = blockName.removePrefix("${colorPrefix}_")
    val newMaterial = Material.getMaterial("${colors[nextIndex]}_$baseSuffix") ?: return@on
    block.type = newMaterial
}

command("colorwand") {
    permission = "op"
    description = "Get a ColorWand tool"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val wand = ItemStack(Material.GOLDEN_AXE)
        val meta = wand.itemMeta!!
        meta.displayName(mm.deserialize("<gold><bold>ColorWand"))
        meta.lore(listOf(
            mm.deserialize("<gray>Right-click: next color"),
            mm.deserialize("<gray>Left-click: previous color")
        ))
        wand.itemMeta = meta
        sender.inventory.addItem(wand)
        sender.sendMessage(mm.deserialize("<green>You received a ColorWand!"))
    }
}
