val mm = MiniMessage.miniMessage()

onLoad {
    server.globalRegionScheduler.runAtFixedRate(plugin, 20L * 1800, 20L * 1800) { _ ->
        server.onlinePlayers.forEach { player ->
            server.dispatchCommand(server.consoleSender, "crates give physical ${player.name} Common 1")
        }
        server.broadcast(mm.deserialize("<gold><bold>[KeyAll]</bold> <yellow>Everyone received a <white>Common</white> crate key!"))
    }
}
