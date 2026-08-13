@file:Repository("https://jitpack.io")
@file:Dependency("com.github.MilkBowl:VaultAPI:1.7.1", withTransitiveDependencies = false)

val mm = MiniMessage.miniMessage()
val dailyPaid = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Double>()

fun parseAmount(input: String): Double? {
    val cleaned = input.lowercase().trim()
    val multiplier = when {
        cleaned.endsWith("k") -> 1_000.0
        cleaned.endsWith("m") -> 1_000_000.0
        cleaned.endsWith("b") -> 1_000_000_000.0
        else -> 1.0
    }
    val numberPart = if (multiplier > 1.0) cleaned.dropLast(1) else cleaned
    return numberPart.toDoubleOrNull()?.let { it * multiplier }
}

fun getDailyLimit(player: Player): Double {
    return when {
        player.hasPermission("pay.limit.unlimited") -> Double.MAX_VALUE
        player.hasPermission("pay.limit.1000000") -> 1_000_000.0
        player.hasPermission("pay.limit.500000") -> 500_000.0
        player.hasPermission("pay.limit.100000") -> 100_000.0
        else -> 50_000.0
    }
}

fun getEconomy(): net.milkbowl.vault.economy.Economy? {
    val rsp = server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
    return rsp?.provider
}

command("pay") {
    description = "Pay another player"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.size < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /pay <player> <amount>"))
            return@execute
        }

        val target = server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found!"))
            return@execute
        }
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(mm.deserialize("<red>You can't pay yourself!"))
            return@execute
        }

        val amount = parseAmount(args[1])
        if (amount == null || amount <= 0) {
            sender.sendMessage(mm.deserialize("<red>Invalid amount! Use a number, optionally with k/m/b suffix."))
            return@execute
        }

        val econ = getEconomy()
        if (econ == null) {
            sender.sendMessage(mm.deserialize("<red>Economy system not available!"))
            return@execute
        }

        val limit = getDailyLimit(sender)
        val alreadyPaid = dailyPaid.getOrDefault(sender.uniqueId, 0.0)
        if (alreadyPaid + amount > limit && limit != Double.MAX_VALUE) {
            val remaining = (limit - alreadyPaid).coerceAtLeast(0.0)
            sender.sendMessage(mm.deserialize("<red>Daily pay limit reached! You can still pay <white>$${"%.2f".format(remaining)}</white> today."))
            return@execute
        }

        if (!econ.has(sender, amount)) {
            sender.sendMessage(mm.deserialize("<red>You don't have enough money!"))
            return@execute
        }

        econ.withdrawPlayer(sender, amount)
        econ.depositPlayer(target, amount)
        dailyPaid[sender.uniqueId] = alreadyPaid + amount

        val formatted = "${"%.2f".format(amount)}"
        sender.sendMessage(mm.deserialize("<green>You paid <white>${target.name}</white> <gold>$$formatted</gold>!"))
        target.sendMessage(mm.deserialize("<green>You received <gold>$$formatted</gold> from <white>${sender.name}</white>!"))
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}

onLoad {
    server.globalRegionScheduler.runAtFixedRate(plugin, 20L * 86400, 20L * 86400) { _ ->
        dailyPaid.clear()
        logger.info("Daily pay limits reset")
    }
}
