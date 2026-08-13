val mm = MiniMessage.miniMessage()

command("store") {
    description = "Visit the web store"

    execute { sender, _ ->
        sender.sendMessage(mm.deserialize(
            "<dark_gray>[<green>Store<dark_gray>] <gray>Visit our web store: " +
            "<click:open_url:'https://cheesesmp-shop.tebex.io/'><hover:show_text:'<yellow>Click to visit!'><green><underlined>store.cheesesmp.top</underlined></hover></click>"
        ))
    }
}

command("ip") {
    permission = "staff.checkip"
    description = "Check a player's IP address"

    execute { sender, args ->
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /ip <player>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<dark_gray>[<red><bold>!</bold><dark_gray>] <red>Could not find player ${args[0]}."))
            return@execute
        }
        val ip = target.address?.address?.hostAddress ?: "unknown"
        sender.sendMessage(mm.deserialize("<dark_gray>[<aqua>IP Check<dark_gray>] <gray>Player: <white>${target.name}"))
        sender.sendMessage(mm.deserialize(
            "<dark_gray>  <gray>IP: " +
            "<click:copy_to_clipboard:'$ip'><hover:show_text:'<green>Click to copy!'><aqua>$ip</hover></click>" +
            " <dark_gray>| " +
            "<click:open_url:'https://whatismyipaddress.com/ip/$ip'><hover:show_text:'<yellow>Click for lookup!'><gray><underlined>[Lookup]</underlined></hover></click>"
        ))
    }

    tabComplete { _, args ->
        if (args.size == 1) server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("nv") {
    aliases = listOf("nightvision")
    description = "Toggles Night Vision for 1 hour"
    permission = "skript.nightvision"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        if (sender.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            sender.removePotionEffect(PotionEffectType.NIGHT_VISION)
            sender.sendMessage(mm.deserialize("<red>Night vision disabled."))
        } else {
            sender.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 3600, 0, false, false))
            sender.sendMessage(mm.deserialize("<green>Night vision enabled for 1 hour."))
        }
    }
}

command("clearmychat") {
    description = "Clear your own chat"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        repeat(100) { sender.sendMessage("") }
        sender.sendMessage(mm.deserialize("<green>Your chat has been cleared!"))
    }
}
