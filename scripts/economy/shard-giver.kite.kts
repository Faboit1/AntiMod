onLoad {
    server.globalRegionScheduler.runAtFixedRate(plugin, 20L * 300, 20L * 300) { _ ->
        server.onlinePlayers.forEach { player ->
            server.dispatchCommand(server.consoleSender, "shards give ${player.name} 5")
        }
        logger.info("Gave 5 shards to all online players")
    }
}
