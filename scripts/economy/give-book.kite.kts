val mm = MiniMessage.miniMessage()

command("givebook") {
    permission = "admin.givebook"
    description = "Give an enchanted book to a player"

    execute { sender, args ->
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /givebook <player> <enchantment> [level]"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found!"))
            return@execute
        }
        val enchantName = args[1]
        val level = args.getOrNull(2)?.toIntOrNull() ?: 1

        server.dispatchCommand(server.consoleSender,
            "give ${target.name} enchanted_book{StoredEnchantments:[{id:\"minecraft:$enchantName\",lvl:${level}s}]} 1"
        )
        sender.sendMessage(mm.deserialize("<green>Gave <white>${target.name}</white> an enchanted book with <white>$enchantName $level</white>!"))
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
