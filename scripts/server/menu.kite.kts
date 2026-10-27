val mm = MiniMessage.miniMessage()

command("menu") {
    description = "Opens the server menu"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        sender.sendMessage(mm.deserialize("<gold><bold>Server Menu"))
        sender.sendMessage(mm.deserialize("<gray>Select an option:"))
        sender.sendMessage(mm.deserialize("<click:run_command:'/spawn'><hover:show_text:'<green>Go to spawn'><yellow>  [Spawn]</hover></click>"))
        sender.sendMessage(mm.deserialize("<click:run_command:'/warps'><hover:show_text:'<green>View available warps'><yellow>  [Warps]</hover></click>"))
        sender.sendMessage(mm.deserialize("<click:run_command:'/store'><hover:show_text:'<green>Visit the store'><yellow>  [Store]</hover></click>"))
        sender.sendMessage(mm.deserialize("<click:run_command:'/rules'><hover:show_text:'<green>View server rules'><yellow>  [Rules]</hover></click>"))
    }
}
