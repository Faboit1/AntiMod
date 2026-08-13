val mm = MiniMessage.miniMessage()
val warps = mutableMapOf<String, Location>()
val warpKey = NamespacedKey(plugin, "warps")
val pendingTeleports = mutableMapOf<java.util.UUID, Int>()

fun loadWarps() {
    val container = server.worlds[0].persistentDataContainer
    val data = container.get(warpKey, PersistentDataType.STRING) ?: return
    data.split(";").filter { it.isNotEmpty() }.forEach { entry ->
        val parts = entry.split(",")
        if (parts.size >= 6) {
            val name = parts[0]
            val world = server.getWorld(parts[1]) ?: return@forEach
            warps[name] = Location(world, parts[2].toDouble(), parts[3].toDouble(), parts[4].toDouble(), parts[5].toFloat(), parts.getOrNull(6)?.toFloat() ?: 0f)
        }
    }
}

fun saveWarps() {
    val container = server.worlds[0].persistentDataContainer
    val data = warps.entries.joinToString(";") { (name, loc) ->
        "$name,${loc.world.name},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"
    }
    container.set(warpKey, PersistentDataType.STRING, data)
}

onLoad {
    loadWarps()
}

onUnload {
    pendingTeleports.values.forEach { server.scheduler.cancelTask(it) }
    pendingTeleports.clear()
}

command("setwarp") {
    permission = "admin.setwarp"
    description = "Create a warp point"

    execute { sender, args ->
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."))
            return@execute
        }
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /setwarp <name>"))
            return@execute
        }
        val name = args[0].lowercase()
        warps[name] = sender.location.clone()
        saveWarps()
        sender.sendMessage(mm.deserialize("<green>Warp <white>$name</white> has been set!"))
    }

    tabComplete { _, args ->
        if (args.size == 1) warps.keys.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("delwarp") {
    permission = "admin.delwarp"
    description = "Delete a warp point"

    execute { sender, args ->
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /delwarp <name>"))
            return@execute
        }
        val name = args[0].lowercase()
        if (warps.remove(name) != null) {
            saveWarps()
            sender.sendMessage(mm.deserialize("<green>Warp <white>$name</white> has been deleted!"))
        } else {
            sender.sendMessage(mm.deserialize("<red>Warp <white>$name</white> does not exist!"))
        }
    }

    tabComplete { _, args ->
        if (args.size == 1) warps.keys.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("warp") {
    description = "Teleport to a warp"

    execute { sender, args ->
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."))
            return@execute
        }
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /warp <name>"))
            return@execute
        }
        val name = args[0].lowercase()
        val loc = warps[name]
        if (loc == null) {
            sender.sendMessage(mm.deserialize("<red>Warp <white>$name</white> does not exist!"))
            return@execute
        }
        if (pendingTeleports.containsKey(sender.uniqueId)) {
            sender.sendMessage(mm.deserialize("<red>You already have a pending teleport!"))
            return@execute
        }
        val startLoc = sender.location.clone()
        sender.sendMessage(mm.deserialize("<yellow>Teleporting to <white>$name</white> in 3 seconds... Don't move!"))

        var countdown = 3
        val taskId = server.scheduler.runTaskTimer(20L, 0L) { runnable ->
            if (!sender.isOnline) {
                pendingTeleports.remove(sender.uniqueId)
                runnable.cancel()
                return@runTaskTimer
            }
            if (sender.location.distanceSquared(startLoc) > 1.0) {
                sender.sendMessage(mm.deserialize("<red>Teleport cancelled! You moved."))
                pendingTeleports.remove(sender.uniqueId)
                runnable.cancel()
                return@runTaskTimer
            }
            countdown--
            if (countdown <= 0) {
                sender.teleport(loc)
                sender.sendMessage(mm.deserialize("<green>Warped to <white>$name</white>!"))
                pendingTeleports.remove(sender.uniqueId)
                runnable.cancel()
            } else {
                sender.sendMessage(mm.deserialize("<yellow>Teleporting in <white>$countdown</white>..."))
            }
        }
        pendingTeleports[sender.uniqueId] = taskId.taskId
    }

    tabComplete { _, args ->
        if (args.size == 1) warps.keys.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("warps") {
    description = "List all warps"

    execute { sender, _ ->
        if (warps.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>No warps have been set."))
            return@execute
        }
        sender.sendMessage(mm.deserialize("<gold><bold>Available Warps:"))
        warps.keys.sorted().forEach { name ->
            sender.sendMessage(mm.deserialize("<gray>- <click:run_command:'/warp $name'><hover:show_text:'<green>Click to warp'><yellow>$name</hover></click>"))
        }
    }
}
