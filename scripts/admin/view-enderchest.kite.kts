val mm = MiniMessage.miniMessage()

command("viewec") {
    permission = "admin.viewec"
    description = "View a player's ender chest"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /viewec <player>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target != null) {
            sender.openInventory(target.enderChest)
            sender.sendMessage(mm.deserialize("<green>Viewing <white>${target.name}</white>'s ender chest."))
        } else {
            val offline = server.getOfflinePlayer(args[0])
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(mm.deserialize("<red>Player has never played on this server!"))
                return@execute
            }
            sender.sendMessage(mm.deserialize("<red>Player is offline. Ender chest viewing for offline players requires NMS access."))
        }
    }

    tabComplete { _, args ->
        if (args.size == 1) server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}
