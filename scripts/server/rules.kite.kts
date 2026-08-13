val mm = MiniMessage.miniMessage()

val rules = listOf(
    "Be respectful to all players.",
    "No cheating, hacking, or exploiting.",
    "No spamming or excessive caps.",
    "No advertising other servers.",
    "No griefing or stealing.",
    "Listen to staff members.",
    "Use common sense.",
    "No NSFW or offensive content.",
    "No real-money trading.",
    "Have fun!"
)

command("rules") {
    description = "View the server rules"

    execute { sender, _ ->
        sender.sendMessage(mm.deserialize(""))
        sender.sendMessage(mm.deserialize("<gold><bold>Server Rules"))
        sender.sendMessage(mm.deserialize("<gray><strikethrough>                                        </strikethrough>"))
        rules.forEachIndexed { index, rule ->
            sender.sendMessage(mm.deserialize("<yellow>${index + 1}. <gray>$rule"))
        }
        sender.sendMessage(mm.deserialize("<gray><strikethrough>                                        </strikethrough>"))
        sender.sendMessage(mm.deserialize("<green>By playing on this server, you agree to follow these rules."))
        sender.sendMessage(mm.deserialize(""))
    }
}
