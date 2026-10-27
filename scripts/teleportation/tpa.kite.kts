val mm = MiniMessage.miniMessage()

data class TpaRequest(
    val sender: java.util.UUID,
    val target: java.util.UUID,
    val timestamp: Long = System.currentTimeMillis()
)

val pendingRequests = java.util.concurrent.ConcurrentHashMap<java.util.UUID, TpaRequest>()
val cooldowns = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long>()
val autoAccept = java.util.concurrent.ConcurrentHashSet<java.util.UUID>()
val COOLDOWN_MS = 30_000L
val EXPIRY_MS = 120_000L

onLoad {
    server.scheduler.runTaskTimer(20L * 30, 20L * 30) {
        val now = System.currentTimeMillis()
        pendingRequests.entries.removeIf { (_, req) ->
            if (now - req.timestamp > EXPIRY_MS) {
                server.getPlayer(req.sender)?.sendMessage(mm.deserialize("<red>Your TPA request has expired."))
                true
            } else false
        }
    }
}

command("tpa") {
    description = "Send a teleport request to a player"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /tpa <player>"))
            return@execute
        }
        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found!"))
            return@execute
        }
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(mm.deserialize("<red>You can't send a TPA request to yourself!"))
            return@execute
        }
        val now = System.currentTimeMillis()
        val lastCooldown = cooldowns[sender.uniqueId] ?: 0L
        if (now - lastCooldown < COOLDOWN_MS) {
            val remaining = (COOLDOWN_MS - (now - lastCooldown)) / 1000
            sender.sendMessage(mm.deserialize("<red>Please wait <white>${remaining}s</white> before sending another request."))
            return@execute
        }

        if (autoAccept.contains(target.uniqueId)) {
            sender.teleport(target)
            sender.sendMessage(mm.deserialize("<green>Teleported to <white>${target.name}</white>! (auto-accept)"))
            target.sendMessage(mm.deserialize("<green><white>${sender.name}</white> teleported to you. (auto-accept)"))
            cooldowns[sender.uniqueId] = now
            return@execute
        }

        pendingRequests[target.uniqueId] = TpaRequest(sender.uniqueId, target.uniqueId)
        cooldowns[sender.uniqueId] = now
        sender.sendMessage(mm.deserialize("<green>TPA request sent to <white>${target.name}</white>!"))
        target.sendMessage(mm.deserialize("<green><white>${sender.name}</white> wants to teleport to you!"))
        target.sendMessage(mm.deserialize(
            "<click:run_command:'/tpaccept'><green><bold>[Accept]</bold></click> " +
            "<click:run_command:'/tpdeny'><red><bold>[Deny]</bold></click>"
        ))
    }

    tabComplete { _, args ->
        if (args.size == 1) server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("tpaccept") {
    description = "Accept a pending TPA request"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val request = pendingRequests.remove(sender.uniqueId)
        if (request == null) {
            sender.sendMessage(mm.deserialize("<red>You have no pending TPA requests."))
            return@execute
        }
        val requester = server.getPlayer(request.sender)
        if (requester == null) {
            sender.sendMessage(mm.deserialize("<red>The requesting player is no longer online."))
            return@execute
        }
        requester.teleport(sender)
        requester.sendMessage(mm.deserialize("<green>Teleported to <white>${sender.name}</white>!"))
        sender.sendMessage(mm.deserialize("<green>Accepted TPA request from <white>${requester.name}</white>!"))
    }
}

command("tpdeny") {
    description = "Deny a pending TPA request"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val request = pendingRequests.remove(sender.uniqueId)
        if (request == null) {
            sender.sendMessage(mm.deserialize("<red>You have no pending TPA requests."))
            return@execute
        }
        val requester = server.getPlayer(request.sender)
        requester?.sendMessage(mm.deserialize("<red><white>${sender.name}</white> denied your TPA request."))
        sender.sendMessage(mm.deserialize("<red>TPA request denied."))
    }
}

command("tpauto") {
    description = "Toggle auto-accept for TPA requests"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        if (autoAccept.contains(sender.uniqueId)) {
            autoAccept.remove(sender.uniqueId)
            sender.sendMessage(mm.deserialize("<red>Auto-accept for TPA requests disabled."))
        } else {
            autoAccept.add(sender.uniqueId)
            sender.sendMessage(mm.deserialize("<green>Auto-accept for TPA requests enabled."))
        }
    }
}

on<PlayerQuitEvent> { event ->
    pendingRequests.remove(event.player.uniqueId)
    pendingRequests.entries.removeIf { it.value.sender == event.player.uniqueId }
}
