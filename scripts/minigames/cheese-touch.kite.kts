val mm = MiniMessage.miniMessage()

var cheeseHolder: java.util.UUID? = null
var gameActive = false
val INFECTION_RADIUS = 3.0
val PUNCH_TRANSFER = true

command("cheesetouch") {
    permission = "admin.cheesetouch"
    description = "Start/stop the Cheese Touch game"

    execute { sender, args ->
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /cheesetouch <start|stop|give>"))
            return@execute
        }
        when (args[0].lowercase()) {
            "start" -> {
                if (gameActive) {
                    sender.sendMessage(mm.deserialize("<red>Game is already running!"))
                    return@execute
                }
                val players = server.onlinePlayers.toList()
                if (players.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Need at least 2 players!"))
                    return@execute
                }
                gameActive = true
                val chosen = players.random()
                cheeseHolder = chosen.uniqueId
                server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <white>${chosen.name}</white> has the Cheese Touch! Run!"))
                chosen.sendMessage(mm.deserialize("<gold><bold>YOU have the Cheese Touch! Touch someone to pass it on!"))
                chosen.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
            }
            "stop" -> {
                if (!gameActive) {
                    sender.sendMessage(mm.deserialize("<red>No game is running!"))
                    return@execute
                }
                val holder = cheeseHolder?.let { server.getPlayer(it) }
                holder?.removePotionEffect(PotionEffectType.GLOWING)
                gameActive = false
                cheeseHolder = null
                server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <gray>Game has been stopped."))
            }
            "give" -> {
                if (args.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /cheesetouch give <player>"))
                    return@execute
                }
                val target = server.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>Player not found!"))
                    return@execute
                }
                val oldHolder = cheeseHolder?.let { server.getPlayer(it) }
                oldHolder?.removePotionEffect(PotionEffectType.GLOWING)
                cheeseHolder = target.uniqueId
                gameActive = true
                target.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
                server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <white>${target.name}</white> now has the Cheese Touch!"))
            }
            else -> sender.sendMessage(mm.deserialize("<red>Usage: /cheesetouch <start|stop|give>"))
        }
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> listOf("start", "stop", "give").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("give", ignoreCase = true))
                server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            else emptyList()
            else -> emptyList()
        }
    }
}

on<EntityDamageByEntityEvent> { event ->
    if (!gameActive) return@on
    val attacker = event.damager as? Player ?: return@on
    val victim = event.entity as? Player ?: return@on
    if (attacker.uniqueId != cheeseHolder) return@on

    val oldHolder = attacker
    oldHolder.removePotionEffect(PotionEffectType.GLOWING)
    cheeseHolder = victim.uniqueId
    victim.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))

    server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <white>${attacker.name}</white> passed the Cheese Touch to <white>${victim.name}</white>!"))
    victim.playSound(victim.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 1.5f)
}

onLoad {
    server.scheduler.runTaskTimer(10L, 10L) {
        if (!gameActive) return@runTaskTimer
        val holderUuid = cheeseHolder ?: return@runTaskTimer
        val holder = server.getPlayer(holderUuid) ?: return@runTaskTimer

        for (nearby in holder.getNearbyEntities(INFECTION_RADIUS, INFECTION_RADIUS, INFECTION_RADIUS)) {
            if (nearby is Player && nearby.uniqueId != holderUuid) {
                holder.removePotionEffect(PotionEffectType.GLOWING)
                cheeseHolder = nearby.uniqueId
                nearby.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
                server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <white>${nearby.name}</white> got too close and caught the Cheese Touch!"))
                nearby.playSound(nearby.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 1.5f)
                break
            }
        }
    }
}

on<PlayerQuitEvent> { event ->
    if (!gameActive) return@on
    if (event.player.uniqueId != cheeseHolder) return@on

    event.player.removePotionEffect(PotionEffectType.GLOWING)
    val remaining = server.onlinePlayers.filter { it.uniqueId != event.player.uniqueId }
    if (remaining.size < 2) {
        gameActive = false
        cheeseHolder = null
        server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <gray>Not enough players. Game stopped."))
    } else {
        val newHolder = remaining.random()
        cheeseHolder = newHolder.uniqueId
        newHolder.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false))
        server.broadcast(mm.deserialize("<yellow><bold>[Cheese Touch]</bold> <white>${event.player.name}</white> left! <white>${newHolder.name}</white> now has it!"))
    }
}
