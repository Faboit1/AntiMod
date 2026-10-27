val mm = MiniMessage.miniMessage()

command("minimessage") {
    description = "Sends a MiniMessage-formatted message to yourself"
    permission = "minimessage.use"

    execute { sender, args ->
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Please provide a message! Example: /minimessage <green>Hello <bold>World!"))
            return@execute
        }
        val message = args.joinToString(" ")
        sender.sendMessage(mm.deserialize(message))
    }
}
