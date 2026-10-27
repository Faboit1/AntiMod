val mm = MiniMessage.miniMessage()

command("spectator") {
    permission = "admin.spectator"
    description = "Toggle spectator mode"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        if (sender.gameMode == GameMode.SPECTATOR) {
            sender.gameMode = GameMode.SURVIVAL
            sender.sendMessage(mm.deserialize("<green>Switched to Survival mode."))
        } else {
            sender.gameMode = GameMode.SPECTATOR
            sender.sendMessage(mm.deserialize("<green>Switched to Spectator mode."))
        }
    }
}
