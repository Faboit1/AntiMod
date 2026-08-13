val mm = MiniMessage.miniMessage()
val checkedPlayers = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Boolean>()
val JOIN_ORDER_THRESHOLD = 15000

on<PlayerJoinEvent> { event ->
    val player = event.player
    if (player.hasPermission("group.newog")) return@on
    if (checkedPlayers.containsKey(player.uniqueId)) return@on

    server.scheduler.runTask(20L) {
        if (!player.isOnline) return@runTask
        checkedPlayers[player.uniqueId] = true

        try {
            val placeholderApi = server.pluginManager.getPlugin("PlaceholderAPI") ?: return@runTask
            val papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            val method = papiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer::class.java, String::class.java)
            val result = method.invoke(null, player, "%firstlogin_join_order%") as? String ?: return@runTask
            val order = result.toIntOrNull() ?: return@runTask
            if (order < JOIN_ORDER_THRESHOLD) {
                server.dispatchCommand(server.consoleSender, "lp user ${player.name} parent add newog")
            }
        } catch (e: Exception) {
            logger.warning("Failed to check join order for ${player.name}: ${e.message}")
        }
    }
}
