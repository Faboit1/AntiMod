val mm = MiniMessage.miniMessage()

val sbElements = listOf("rank", "online", "money", "kills", "deaths", "playtime", "keyall", "team", "shards")

command("scoreboard") {
    aliases = listOf("sidebar", "sb")
    description = "Toggle scoreboard elements"

    execute { sender, args ->
        if (sender !is Player) return@execute

        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize(""))
            sender.sendMessage(mm.deserialize("<gold><bold>Scoreboard Settings"))
            sender.sendMessage(mm.deserialize("<gray>Click to toggle each element:"))

            sbElements.forEach { element ->
                val hidden = sender.scoreboardTags.contains("hide$element")
                val status = if (hidden) "<red>[OFF]" else "<green>[ON]"
                sender.sendMessage(mm.deserialize(
                    "<click:run_command:'/scoreboard toggle $element'>" +
                    "<hover:show_text:'<gray>Click to toggle'>" +
                    "  $status <yellow>${element.replaceFirstChar { it.uppercase() }}" +
                    "</hover></click>"
                ))
            }
            sender.sendMessage(mm.deserialize(""))
            return@execute
        }

        if (args[0].equals("toggle", ignoreCase = true) && args.size >= 2) {
            val element = args[1].lowercase()
            if (element !in sbElements) {
                sender.sendMessage(mm.deserialize("<red>Unknown element: $element"))
                return@execute
            }
            val tag = "hide$element"
            if (sender.scoreboardTags.contains(tag)) {
                sender.removeScoreboardTag(tag)
                sender.sendMessage(mm.deserialize("<green>${element.replaceFirstChar { it.uppercase() }} is now visible on your scoreboard."))
            } else {
                sender.addScoreboardTag(tag)
                sender.sendMessage(mm.deserialize("<red>${element.replaceFirstChar { it.uppercase() }} is now hidden from your scoreboard."))
            }
        }
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> listOf("toggle").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> sbElements.filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
