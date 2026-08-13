val mm = MiniMessage.miniMessage()

command("listah") {
    description = "List an item on the auction house"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage(mm.deserialize("<red>You need to be holding an item to sell."))
            return@execute
        }

        val amount = item.amount
        val itemName = item.type.name.lowercase().replace("_", " ")

        sender.sendMessage(mm.deserialize(""))
        sender.sendMessage(mm.deserialize("<gold><bold>List on Auction"))
        sender.sendMessage(mm.deserialize("<gray>Item: <white>$itemName x$amount"))
        sender.sendMessage(mm.deserialize(""))
        sender.sendMessage(mm.deserialize("<gray>Use the command to list:"))
        sender.sendMessage(mm.deserialize("<yellow>/ah sell <price> [amount] [currency]"))
        sender.sendMessage(mm.deserialize(""))
        sender.sendMessage(mm.deserialize("<gray>Currency options: <white>Money <gray>or <white>Shards"))
        sender.sendMessage(mm.deserialize("<gray>Example: <yellow>/ah sell 1000 $amount Money"))
        sender.sendMessage(mm.deserialize(""))
    }
}
