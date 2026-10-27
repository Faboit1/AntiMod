import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.query.QueryOptions

val mm = MiniMessage.miniMessage()
val selectedRanks = java.util.concurrent.ConcurrentHashMap<java.util.UUID, String>()
val selectedRanksKey = NamespacedKey(plugin, "selected-ranks")

fun lp() = LuckPermsProvider.get()

fun groupExists(name: String): Boolean = lp().groupManager.getGroup(name) != null

fun getGroupDisplayName(name: String): String {
    val group = lp().groupManager.getGroup(name) ?: return name
    return group.displayName ?: name.replaceFirstChar { it.uppercase() }
}

fun lpGroupPrefix(name: String): String {
    val group = lp().groupManager.getGroup(name) ?: return ""
    val meta = group.cachedData.getMetaData(QueryOptions.nonContextual())
    return meta.prefix ?: ""
}

fun lpPlayerGroup(player: org.bukkit.OfflinePlayer): String {
    val user = lp().userManager.getUser(player.uniqueId) ?: return "default"
    return user.primaryGroup
}

fun getActiveRank(player: org.bukkit.OfflinePlayer): String {
    return selectedRanks[player.uniqueId] ?: lpPlayerGroup(player)
}

fun refreshDisplay(player: Player) {
    val group = getActiveRank(player)
    val prefix = lpGroupPrefix(group)
    val component = if (prefix.isEmpty() || prefix == "<none>") {
        mm.deserialize("<gray>[${getGroupDisplayName(group)}<gray>] ")
    } else {
        mm.deserialize(prefix)
    }
    val scoreboard = server.scoreboardManager.mainScoreboard
    var team = scoreboard.getTeam("rank_$group")
    if (team == null) team = scoreboard.registerNewTeam("rank_$group")
    team.prefix(component)
    team.addEntry(player.name)
    player.playerListName(component.append(net.kyori.adventure.text.Component.text(player.name)))
}

command("rank") {
    aliases = listOf("rankswitch")
    description = "Switch your display rank or manage ranks"

    execute { sender, args ->
        if (sender !is Player && (args.isEmpty() || args[0] != "set")) return@execute

        if (args.isEmpty()) {
            val player = sender as Player
            val currentRank = getActiveRank(player)
            val currentDisplay = getGroupDisplayName(currentRank)

            player.sendMessage(mm.deserialize(""))
            player.sendMessage(mm.deserialize("<gold><bold>Select Your Rank"))
            player.sendMessage(mm.deserialize("<gray>Currently displaying as <white>$currentDisplay</white>."))
            player.sendMessage(mm.deserialize(""))

            val user = lp().userManager.getUser(player.uniqueId)
            if (user != null) {
                val groups = user.getInheritedGroups(QueryOptions.nonContextual())
                for (group in groups) {
                    val name = group.name
                    val prefix = lpGroupPrefix(name)
                    val displayName = getGroupDisplayName(name)
                    val label = if (prefix.isNotEmpty() && prefix != "<none>") "$prefix <white>$displayName" else "<white>$displayName"
                    player.sendMessage(mm.deserialize(
                        "<click:run_command:'/rank switch $name'><hover:show_text:'<gray>Click to display this rank'>  $label</hover></click>"
                    ))
                }
            }

            player.sendMessage(mm.deserialize(""))
            player.sendMessage(mm.deserialize("<click:run_command:'/rank reset'><hover:show_text:'<gray>Reset to default'><yellow>  [Reset to Default]</hover></click>"))
            return@execute
        }

        when (args[0].lowercase()) {
            "switch" -> {
                if (sender !is Player) return@execute
                if (args.size < 2) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /rank switch <rank>"))
                    return@execute
                }
                val group = args[1]
                if (!groupExists(group)) {
                    sender.sendMessage(mm.deserialize("<red>That rank does not exist."))
                    return@execute
                }
                selectedRanks[sender.uniqueId] = group
                refreshDisplay(sender)
                sender.sendMessage(mm.deserialize("<green>Your display rank has been updated to <white>${getGroupDisplayName(group)}</white>!"))
            }
            "set" -> {
                if (!sender.hasPermission("ranks.admin")) {
                    sender.sendMessage(mm.deserialize("<red>You don't have permission to do that."))
                    return@execute
                }
                if (args.size < 3) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /rank set <player> <rank>"))
                    return@execute
                }
                val target = server.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>Player not online."))
                    return@execute
                }
                if (!groupExists(args[2])) {
                    sender.sendMessage(mm.deserialize("<red>Unknown rank: ${args[2]}"))
                    return@execute
                }
                selectedRanks[target.uniqueId] = args[2]
                refreshDisplay(target)
                sender.sendMessage(mm.deserialize("<green>Set ${target.name}'s display rank to <white>${args[2]}</white>."))
            }
            "reset" -> {
                if (sender !is Player) return@execute
                selectedRanks.remove(sender.uniqueId)
                refreshDisplay(sender)
                sender.sendMessage(mm.deserialize("<green>Your display rank has been reset to your default rank."))
            }
            else -> sender.sendMessage(mm.deserialize("<red>Unknown subcommand. Use /rank, /rank reset, or /rank set <player> <rank>."))
        }
    }

    tabComplete { _, args ->
        when (args.size) {
            1 -> listOf("switch", "set", "reset").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("set", ignoreCase = true))
                server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            else emptyList()
            else -> emptyList()
        }
    }
}

on<PlayerJoinEvent> { event ->
    server.scheduler.runTask(5L) {
        refreshDisplay(event.player)
    }
}
