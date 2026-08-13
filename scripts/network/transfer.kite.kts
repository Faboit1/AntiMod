val mm = MiniMessage.miniMessage()

onLoad {
    server.messenger.registerOutgoingPluginChannel(plugin, "BungeeCord")
}

onUnload {
    server.messenger.unregisterOutgoingPluginChannel(plugin, "BungeeCord")
}

command("send") {
    permission = "admin.send"
    description = "Transfer a player to another server"

    execute { sender, args ->
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /send <player> <server>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player <white>${args[0]}</white> not found!"))
            return@execute
        }
        val serverName = args[1]
        val out = com.google.common.io.ByteStreams.newDataOutput()
        out.writeUTF("ConnectOther")
        out.writeUTF(target.name)
        out.writeUTF(serverName)
        target.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        sender.sendMessage(mm.deserialize("<green>Sending <white>${target.name}</white> to <white>$serverName</white>..."))
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
