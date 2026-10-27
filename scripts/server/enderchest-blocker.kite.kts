val mm = MiniMessage.miniMessage()

on<PlayerCommandPreprocessEvent> { event ->
    val cmd = event.message.lowercase().split(" ")[0]
    if (cmd == "/enderchest" || cmd == "/ec") {
        event.isCancelled = true
        event.player.sendMessage(mm.deserialize("<red><bold>[!]</bold> Enderchest commands have been disabled on this server."))
        event.player.sendMessage(mm.deserialize("<gray>If you want to access your stuff, you need to buy a physical enderchest from <yellow>/shop<gray>."))
    }
}
