val mm = MiniMessage.miniMessage()

val queue = java.util.concurrent.ConcurrentLinkedQueue<java.util.UUID>()
val activeDuels = java.util.concurrent.ConcurrentHashMap<java.util.UUID, java.util.UUID>()
val duelInvites = java.util.concurrent.ConcurrentHashMap<java.util.UUID, java.util.UUID>()

val ARENA_RADIUS = 50
val COUNTDOWN_SECONDS = 5

fun findSafeLocation(world: World): Location {
    var attempts = 0
    while (attempts < 50) {
        val x = (Math.random() * ARENA_RADIUS * 2 - ARENA_RADIUS).toInt()
        val z = (Math.random() * ARENA_RADIUS * 2 - ARENA_RADIUS).toInt()
        val block = world.getHighestBlockAt(x, z)
        if (block.type.isSolid && block.type != Material.LAVA) {
            return block.location.add(0.5, 1.0, 0.5)
        }
        attempts++
    }
    return world.spawnLocation
}

fun startDuel(p1: Player, p2: Player) {
    activeDuels[p1.uniqueId] = p2.uniqueId
    activeDuels[p2.uniqueId] = p1.uniqueId

    val loc1 = findSafeLocation(p1.world)
    val loc2 = loc1.clone().add(10.0, 0.0, 0.0)

    p1.teleport(loc1)
    p2.teleport(loc2)

    var countdown = COUNTDOWN_SECONDS
    server.scheduler.runTaskTimer(20L, 0L) { task ->
        if (!p1.isOnline || !p2.isOnline) {
            activeDuels.remove(p1.uniqueId)
            activeDuels.remove(p2.uniqueId)
            task.cancel()
            return@runTaskTimer
        }
        if (countdown > 0) {
            val msg = mm.deserialize("<yellow>Duel starts in <white>$countdown</white>...")
            p1.sendMessage(msg)
            p2.sendMessage(msg)
            countdown--
        } else {
            val msg = mm.deserialize("<green><bold>FIGHT!")
            p1.sendTitle(mm.deserialize("<green><bold>FIGHT!"), mm.deserialize(""), 5, 20, 5)
            p2.sendTitle(mm.deserialize("<green><bold>FIGHT!"), mm.deserialize(""), 5, 20, 5)
            p1.playSound(p1.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f)
            p2.playSound(p2.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f)
            task.cancel()
        }
    }
}

command("rtpq") {
    description = "Join the random duel queue"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        if (activeDuels.containsKey(sender.uniqueId)) {
            sender.sendMessage(mm.deserialize("<red>You're already in a duel!"))
            return@execute
        }
        if (queue.contains(sender.uniqueId)) {
            queue.remove(sender.uniqueId)
            sender.sendMessage(mm.deserialize("<red>Left the duel queue."))
            return@execute
        }

        queue.add(sender.uniqueId)
        sender.sendMessage(mm.deserialize("<green>Joined the duel queue! Waiting for an opponent..."))

        if (queue.size >= 2) {
            val uuid1 = queue.poll() ?: return@execute
            val uuid2 = queue.poll() ?: run {
                queue.add(uuid1)
                return@execute
            }
            val p1 = server.getPlayer(uuid1)
            val p2 = server.getPlayer(uuid2)
            if (p1 != null && p2 != null) {
                startDuel(p1, p2)
            }
        }
    }
}

command("duel") {
    description = "Challenge a player to a duel"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /duel <player>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found!"))
            return@execute
        }
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(mm.deserialize("<red>You can't duel yourself!"))
            return@execute
        }

        duelInvites[target.uniqueId] = sender.uniqueId
        sender.sendMessage(mm.deserialize("<green>Duel invite sent to <white>${target.name}</white>!"))
        target.sendMessage(mm.deserialize("<green><white>${sender.name}</white> challenged you to a duel!"))
        target.sendMessage(mm.deserialize(
            "<click:run_command:'/duelaccept'><green><bold>[Accept]</bold></click> " +
            "<click:run_command:'/dueldecline'><red><bold>[Decline]</bold></click>"
        ))
    }

    tabComplete { _, args ->
        if (args.size == 1) server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("duelaccept") {
    description = "Accept a duel invite"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val challengerUuid = duelInvites.remove(sender.uniqueId)
        if (challengerUuid == null) {
            sender.sendMessage(mm.deserialize("<red>You have no pending duel invites."))
            return@execute
        }
        val challenger = server.getPlayer(challengerUuid)
        if (challenger == null) {
            sender.sendMessage(mm.deserialize("<red>The challenger is no longer online."))
            return@execute
        }
        startDuel(challenger, sender)
    }
}

command("dueldecline") {
    description = "Decline a duel invite"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val challengerUuid = duelInvites.remove(sender.uniqueId)
        if (challengerUuid == null) {
            sender.sendMessage(mm.deserialize("<red>You have no pending duel invites."))
            return@execute
        }
        server.getPlayer(challengerUuid)?.sendMessage(mm.deserialize("<red><white>${sender.name}</white> declined your duel invite."))
        sender.sendMessage(mm.deserialize("<red>Duel invite declined."))
    }
}

on<PlayerDeathEvent> { event ->
    val player = event.player
    val opponentUuid = activeDuels.remove(player.uniqueId) ?: return@on
    activeDuels.remove(opponentUuid)
    val opponent = server.getPlayer(opponentUuid) ?: return@on
    opponent.sendMessage(mm.deserialize("<green><bold>You won the duel against <white>${player.name}</white>!"))
    player.sendMessage(mm.deserialize("<red>You lost the duel against <white>${opponent.name}</white>."))
}

on<PlayerQuitEvent> { event ->
    queue.remove(event.player.uniqueId)
    duelInvites.remove(event.player.uniqueId)
    val opponentUuid = activeDuels.remove(event.player.uniqueId)
    if (opponentUuid != null) {
        activeDuels.remove(opponentUuid)
        server.getPlayer(opponentUuid)?.sendMessage(mm.deserialize("<green>Your duel opponent disconnected. You win!"))
    }
}
