val mm = MiniMessage.miniMessage()
val activePlayers = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Boolean>()

val discs = listOf(
    Sound.MUSIC_DISC_13, Sound.MUSIC_DISC_CAT, Sound.MUSIC_DISC_BLOCKS,
    Sound.MUSIC_DISC_CHIRP, Sound.MUSIC_DISC_FAR, Sound.MUSIC_DISC_MALL,
    Sound.MUSIC_DISC_MELLOHI, Sound.MUSIC_DISC_STAL, Sound.MUSIC_DISC_STRAD,
    Sound.MUSIC_DISC_WARD, Sound.MUSIC_DISC_11, Sound.MUSIC_DISC_WAIT,
    Sound.MUSIC_DISC_PIGSTEP, Sound.MUSIC_DISC_OTHERSIDE, Sound.MUSIC_DISC_5,
    Sound.MUSIC_DISC_RELIC
)

fun playRandomDisc(player: Player) {
    if (!activePlayers.containsKey(player.uniqueId)) return
    if (!player.isOnline) {
        activePlayers.remove(player.uniqueId)
        return
    }
    val disc = discs.random()
    player.playSound(player.location, disc, SoundCategory.RECORDS, 1.0f, 1.0f)
}

command("music") {
    description = "Toggle the music player"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        if (activePlayers.containsKey(sender.uniqueId)) {
            activePlayers.remove(sender.uniqueId)
            sender.stopAllSounds()
            sender.sendMessage(mm.deserialize("<red>Music player stopped."))
        } else {
            activePlayers[sender.uniqueId] = true
            playRandomDisc(sender)
            sender.sendMessage(mm.deserialize("<green>Music player started! Use <white>/music</white> again to stop."))
        }
    }
}

onLoad {
    server.scheduler.runTaskTimer(20L * 180, 20L * 60) {
        activePlayers.keys.forEach { uuid ->
            val player = server.getPlayer(uuid)
            if (player != null && player.isOnline) {
                playRandomDisc(player)
            } else {
                activePlayers.remove(uuid)
            }
        }
    }
}

on<PlayerQuitEvent> { event ->
    activePlayers.remove(event.player.uniqueId)
}
