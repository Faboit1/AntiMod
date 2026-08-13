val mm = MiniMessage.miniMessage()

data class GGWaveSession(
    val endTime: Long,
    val rewarded: java.util.concurrent.ConcurrentHashSet<java.util.UUID> = java.util.concurrent.ConcurrentHashSet()
)

var activeWave: GGWaveSession? = null
val WAVE_DURATION_MS = 60_000L
val SHARD_REWARD = 10

command("ggwave") {
    permission = "admin.ggwave"
    description = "Start a GG wave event"

    execute { sender, _ ->
        if (activeWave != null) {
            sender.sendMessage(mm.deserialize("<red>A GG wave is already active!"))
            return@execute
        }
        activeWave = GGWaveSession(System.currentTimeMillis() + WAVE_DURATION_MS)
        server.broadcast(mm.deserialize("<gold><bold>[GG Wave]</bold> <yellow>Type <white>GG</white> in chat within 60 seconds to earn <white>$SHARD_REWARD shards</white>!"))

        server.scheduler.runTask(20L * (WAVE_DURATION_MS / 1000)) {
            val wave = activeWave ?: return@runTask
            server.broadcast(mm.deserialize("<gold><bold>[GG Wave]</bold> <yellow>Wave ended! <white>${wave.rewarded.size}</white> players earned shards!"))
            activeWave = null
        }
    }
}

on<io.papermc.paper.event.player.AsyncChatEvent> { event ->
    val wave = activeWave ?: return@on
    if (System.currentTimeMillis() > wave.endTime) return@on

    val message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message())
    if (message.trim().equals("GG", ignoreCase = true) || message.trim().equals("gg", ignoreCase = true)) {
        val player = event.player
        if (wave.rewarded.contains(player.uniqueId)) return@on
        wave.rewarded.add(player.uniqueId)
        server.scheduler.runTask(0L) {
            server.dispatchCommand(server.consoleSender, "shards give ${player.name} $SHARD_REWARD")
        }
        player.sendMessage(mm.deserialize("<green>You earned <white>$SHARD_REWARD shards</white> from the GG wave!"))
    }
}
