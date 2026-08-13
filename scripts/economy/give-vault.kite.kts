val mm = MiniMessage.miniMessage()

command("givevault") {
    permission = "admin.givevault"
    description = "Unlock a vault slot for a player"

    execute { sender, args ->
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /givevault <player> <slot>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found!"))
            return@execute
        }
        val slot = args[1]
        server.dispatchCommand(server.consoleSender, "lp user ${target.name} permission set playervaults.amount.$slot")
        sender.sendMessage(mm.deserialize("<green>Unlocked vault slot <white>$slot</white> for <white>${target.name}</white>!"))
        target.sendMessage(mm.deserialize("<green>You now have access to vault slot <white>$slot</white>!"))
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> (1..10).map { it.toString() }.filter { it.startsWith(args[1]) }
            else -> emptyList()
        }
    }
}
