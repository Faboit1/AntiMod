val mm = MiniMessage.miniMessage()

data class TotemSession(
    val player: java.util.UUID,
    var combo: Int = 0,
    var bestCombo: Int = 0,
    var startTime: Long = System.currentTimeMillis(),
    var clicks: Int = 0
)

val sessions = java.util.concurrent.ConcurrentHashMap<java.util.UUID, TotemSession>()
val GUI_TITLE = mm.deserialize("<gold><bold>Totem Practice")

fun openTotemGui(player: Player) {
    val inv = server.createInventory(null, 54, GUI_TITLE)

    for (i in 0 until 54) {
        inv.setItem(i, ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.also { it.displayName(mm.deserialize("<gray>")) }
        })
    }

    val totemSlot = (0 until 54).random()
    inv.setItem(totemSlot, ItemStack(Material.TOTEM_OF_UNDYING).apply {
        itemMeta = itemMeta?.also { it.displayName(mm.deserialize("<gold><bold>Click me!")) }
    })

    sessions[player.uniqueId] = sessions.getOrDefault(player.uniqueId, TotemSession(player.uniqueId))
    player.openInventory(inv)
}

command("totempractice") {
    description = "Practice totem clicking"
    aliases = listOf("tp")

    execute { sender, _ ->
        if (sender !is Player) return@execute
        sessions[sender.uniqueId] = TotemSession(sender.uniqueId)
        openTotemGui(sender)
    }
}

on<InventoryClickEvent> { event ->
    val player = event.whoClicked as? Player ?: return@on
    val session = sessions[player.uniqueId] ?: return@on
    val view = event.view
    val title = view.title()

    if (title != GUI_TITLE) return@on
    event.isCancelled = true

    val clicked = event.currentItem ?: return@on

    if (clicked.type == Material.TOTEM_OF_UNDYING) {
        session.combo++
        session.clicks++
        if (session.combo > session.bestCombo) {
            session.bestCombo = session.combo
        }

        val elapsed = (System.currentTimeMillis() - session.startTime) / 1000.0
        val cps = if (elapsed > 0) session.clicks / elapsed else 0.0

        player.sendActionBar(mm.deserialize(
            "<green>Combo: <white>${session.combo}</white> | Best: <white>${session.bestCombo}</white> | CPS: <white>${"%.1f".format(cps)}</white>"
        ))

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f + (session.combo * 0.05f).coerceAtMost(1.0f))

        openTotemGui(player)
    } else {
        session.combo = 0
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f)
        player.sendActionBar(mm.deserialize("<red>Missed! Combo reset. Best: <white>${session.bestCombo}</white>"))
    }
}

on<InventoryCloseEvent> { event ->
    val player = event.player as? Player ?: return@on
    val session = sessions.remove(player.uniqueId) ?: return@on
    player.sendMessage(mm.deserialize(
        "<gold><bold>[Totem Practice]</bold> <yellow>Session ended! Best combo: <white>${session.bestCombo}</white>"
    ))
}
