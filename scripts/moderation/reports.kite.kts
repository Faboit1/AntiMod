val mm = MiniMessage.miniMessage()

data class Report(
    val reporter: java.util.UUID,
    val reported: String,
    val category: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

val activeReports = java.util.Collections.synchronizedList(mutableListOf<Report>())

val reportCategories = listOf(
    "Cheating/Hacking",
    "Harassment/Toxicity",
    "Spamming",
    "Inappropriate Content",
    "Bug Abuse",
    "Other"
)

val reportCooldowns = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long>()
val COOLDOWN_MS = 60_000L

command("report") {
    description = "Report a player"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.isEmpty()) {
            sender.sendMessage(mm.deserialize("<red>Usage: /report <player> [reason]"))
            return@execute
        }

        val now = System.currentTimeMillis()
        val lastReport = reportCooldowns[sender.uniqueId] ?: 0L
        if (now - lastReport < COOLDOWN_MS) {
            val remaining = (COOLDOWN_MS - (now - lastReport)) / 1000
            sender.sendMessage(mm.deserialize("<red>Please wait <white>${remaining}s</white> before submitting another report."))
            return@execute
        }

        val targetName = args[0]
        sender.sendMessage(mm.deserialize(""))
        sender.sendMessage(mm.deserialize("<gold><bold>Report Player: <white>$targetName"))
        sender.sendMessage(mm.deserialize("<gray>Select a category:"))
        reportCategories.forEachIndexed { i, category ->
            val reason = if (args.size > 1) args.drop(1).joinToString(" ") else "No reason specified"
            sender.sendMessage(mm.deserialize(
                "<click:run_command:'/reportsubmit $targetName ${i} $reason'><hover:show_text:'<green>Click to select'><yellow>  [${i+1}] $category</hover></click>"
            ))
        }
        sender.sendMessage(mm.deserialize(""))
    }

    tabComplete { _, args ->
        if (args.size == 1) server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("reportsubmit") {
    description = "Submit a report (internal)"

    execute { sender, args ->
        if (sender !is Player) return@execute
        if (args.size < 3) return@execute

        val targetName = args[0]
        val categoryIndex = args[1].toIntOrNull() ?: return@execute
        if (categoryIndex !in reportCategories.indices) return@execute
        val reason = args.drop(2).joinToString(" ")

        val report = Report(sender.uniqueId, targetName, reportCategories[categoryIndex], reason)
        activeReports.add(report)
        reportCooldowns[sender.uniqueId] = System.currentTimeMillis()

        sender.sendMessage(mm.deserialize("<green>Report submitted against <white>$targetName</white> for <white>${reportCategories[categoryIndex]}</white>."))

        server.onlinePlayers.filter { it.hasPermission("reports.view") }.forEach { staff ->
            staff.sendMessage(mm.deserialize(
                "<red><bold>[Report]</bold> <white>${sender.name}</white> reported <white>$targetName</white>: " +
                "<gray>${reportCategories[categoryIndex]} - $reason"
            ))
        }
    }

    tabComplete { _, _ -> emptyList() }
}

command("reports") {
    permission = "reports.view"
    description = "View pending reports"

    execute { sender, _ ->
        if (activeReports.isEmpty()) {
            sender.sendMessage(mm.deserialize("<green>No pending reports."))
            return@execute
        }
        sender.sendMessage(mm.deserialize("<gold><bold>Pending Reports (${activeReports.size}):"))
        activeReports.takeLast(10).forEachIndexed { i, report ->
            val reporterName = server.getOfflinePlayer(report.reporter).name ?: "Unknown"
            val ago = (System.currentTimeMillis() - report.timestamp) / 1000
            sender.sendMessage(mm.deserialize(
                "<yellow>#${i+1} <gray>$reporterName -> <white>${report.reported}</white> " +
                "<gray>[${report.category}] <dark_gray>${ago}s ago"
            ))
        }
    }
}

command("reportclear") {
    permission = "reports.manage"
    description = "Clear all reports"

    execute { sender, _ ->
        val count = activeReports.size
        activeReports.clear()
        sender.sendMessage(mm.deserialize("<green>Cleared <white>$count</white> reports."))
    }
}
