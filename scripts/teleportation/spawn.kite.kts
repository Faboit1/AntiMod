val mm = MiniMessage.miniMessage()
val spawnKey = NamespacedKey(plugin, "spawn-location")

fun getSpawnLocation(): Location? {
    val container = server.worlds[0].persistentDataContainer
    val data = container.get(spawnKey, PersistentDataType.STRING) ?: return null
    val parts = data.split(",")
    if (parts.size < 5) return null
    val world = server.getWorld(parts[0]) ?: return null
    return Location(world, parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble(), parts[4].toFloat(), parts.getOrNull(5)?.toFloat() ?: 0f)
}

fun saveSpawnLocation(loc: Location) {
    val container = server.worlds[0].persistentDataContainer
    val data = "${loc.world.name},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"
    container.set(spawnKey, PersistentDataType.STRING, data)
}

command("setspawn") {
    permission = "admin.setspawn"
    description = "Sets the server spawn point"

    execute { sender, _ ->
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."))
            return@execute
        }
        saveSpawnLocation(sender.location)
        sender.sendMessage(mm.deserialize("<green>Spawn location has been set!"))
    }
}

command("spawn") {
    description = "Teleport to the server spawn"

    execute { sender, _ ->
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command."))
            return@execute
        }
        val spawn = getSpawnLocation()
        if (spawn == null) {
            sender.sendMessage(mm.deserialize("<red>No spawn location has been set!"))
            return@execute
        }
        sender.teleport(spawn)
        sender.sendMessage(mm.deserialize("<green>Teleported to spawn!"))
    }
}
