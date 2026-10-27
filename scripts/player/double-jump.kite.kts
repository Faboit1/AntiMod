val mm = MiniMessage.miniMessage()
val jumpCooldowns = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long>()
val JUMP_COOLDOWN_MS = 1500L

fun getMaxJumps(player: Player): Int {
    return when {
        player.hasPermission("doublejump.5") -> 5
        player.hasPermission("doublejump.4") -> 4
        player.hasPermission("doublejump.3") -> 3
        player.hasPermission("doublejump.2") -> 2
        player.hasPermission("doublejump.1") -> 1
        else -> 0
    }
}

val jumpCounts = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Int>()

on<PlayerToggleFlightEvent> { event ->
    val player = event.player
    if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return@on
    if (!player.hasPermission("doublejump.use")) return@on

    val maxJumps = getMaxJumps(player)
    if (maxJumps <= 0) return@on

    val currentJumps = jumpCounts.getOrDefault(player.uniqueId, 0)
    if (currentJumps >= maxJumps) return@on

    val now = System.currentTimeMillis()
    val lastJump = jumpCooldowns.getOrDefault(player.uniqueId, 0L)
    if (now - lastJump < JUMP_COOLDOWN_MS) return@on

    event.isCancelled = true
    jumpCooldowns[player.uniqueId] = now
    jumpCounts[player.uniqueId] = currentJumps + 1

    val velocity = player.location.direction.multiply(0.5)
    velocity.y = 0.8
    player.velocity = velocity
    player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.2f)

    player.allowFlight = true
}

on<PlayerMoveEvent> { event ->
    val player = event.player
    if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return@on
    if (!player.hasPermission("doublejump.use")) return@on

    if (player.isOnGround) {
        jumpCounts[player.uniqueId] = 0
        val maxJumps = getMaxJumps(player)
        if (maxJumps > 0) {
            player.allowFlight = true
        }
    }
}

command("fly") {
    permission = "doublejump.fly"
    description = "Toggle flight mode"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        sender.allowFlight = !sender.allowFlight
        sender.isFlying = sender.allowFlight
        if (sender.allowFlight) {
            sender.sendMessage(mm.deserialize("<green>Flight enabled!"))
        } else {
            sender.sendMessage(mm.deserialize("<red>Flight disabled!"))
        }
    }
}

on<PlayerQuitEvent> { event ->
    jumpCooldowns.remove(event.player.uniqueId)
    jumpCounts.remove(event.player.uniqueId)
}
