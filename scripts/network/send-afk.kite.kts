val mm = MiniMessage.miniMessage()

onLoad {
    server.messenger.registerOutgoingPluginChannel(plugin, "BungeeCord")
    logger.info("AFK transfer channel registered")
}

onUnload {
    server.messenger.unregisterOutgoingPluginChannel(plugin, "BungeeCord")
}

command("afk") {
    description = "Transfer to the AFK server"
    permission = "server.afk"

    execute { sender, _ ->
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."))
            return@execute
        }
        val out = com.google.common.io.ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF("afk2")
        sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        sender.sendMessage(mm.deserialize("<green>Sending you to the AFK server..."))
    }
}
