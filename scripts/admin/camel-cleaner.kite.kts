val mm = MiniMessage.miniMessage()

command("cleancamelbossbars") {
    permission = "op"
    description = "Remove all registered cheese camel bossbars"

    execute { sender, _ ->
        sender.sendMessage(mm.deserialize("<yellow>[Cleaner] Scanning for ghost camel bossbars..."))
        var count = 0
        server.worlds[0].getEntitiesByClass(org.bukkit.entity.Camel::class.java).forEach { camel ->
            if (camel.scoreboardTags.contains("cheese_dropper")) {
                camel.scoreboardTags.remove("has_bossbar")
                count++
            }
        }
        sender.sendMessage(mm.deserialize("<green>[Cleaner] Attempted cleanup on <white>$count</white> camel entities."))
    }
}
