val mm = MiniMessage.miniMessage()

val prefixCommands = listOf(
    "lp group ownersis meta removeprefix 10099990",
    "lp group coowner meta removeprefix 99999",
    "lp group ouwesnuifduif meta removeprefix 9909",
    "lp group darkk meta removeprefix 9999",
    "lp group manager meta removeprefix 8888888",
    "lp group developer meta removeprefix 66999",
    "lp group jrmod meta removeprefix 150000",
    "lp group mod meta removeprefix 17000",
    "lp group admin meta removeprefix 9000",
    "lp group cold meta removeprefix 1834",
    "lp group cold meta removeprefix 8834",
    "lp group bugfinder meta removeprefix 9998",
    "lp group media meta removeprefix 2980",
    "lp group vibes meta removeprefix 5100",
    "lp group jrmediamanager meta removeprefix 100",
    "lp group wemmbu meta removeprefix 3905",
    "lp group dark_blade meta removeprefix 1900",
    "lp group jradmin meta removeprefix 1700",
    "lp group srmod meta removeprefix 155",
    "lp group helper meta removeprefix 19950",
    "lp group flamefrags meta removeprefix 1542",
    "lp group tuff meta removeprefix 2834",
    "lp group ashswagg meta removeprefix 1002",
    "lp group media+ meta removeprefix 10000",
    "lp group supporter+ meta removeprefix 300",
    "lp group supporter meta removeprefix 200",
    "lp group og meta removeprefix 80",
    "lp group og2 meta removeprefix 500",
    "lp group customexample meta removeprefix 9999",
    "lp group default meta removeprefix 1",
    "lp group izukvss meta removeprefix 99999",
    "lp group lucy_on_mc meta removeprefix 9999",
    "lp group tendedsoap meta removeprefix 99999"
)

command("deleteoldprefixes") {
    permission = "admin.deleteoldprefixes"
    description = "Remove legacy smallcaps prefixes from LuckPerms groups"

    execute { sender, _ ->
        val total = prefixCommands.size
        sender.sendMessage(mm.deserialize("<gold>[Cleanup] <yellow>Starting removal of <white>$total</white> old prefixes..."))

        var index = 0
        server.scheduler.runTaskTimer(40L, 0L) { task ->
            if (index >= total) {
                sender.sendMessage(mm.deserialize("<green>[Cleanup] Done! Removed all <white>$total</white> old prefixes."))
                task.cancel()
                return@runTaskTimer
            }
            server.dispatchCommand(server.consoleSender, prefixCommands[index])
            index++
            val remaining = total - index
            if (remaining > 0 && sender is Player && (sender as Player).isOnline) {
                sender.sendMessage(mm.deserialize("<gray>[Cleanup] <white>$index/$total</white> removed - <yellow>ETA: ${remaining * 2}s"))
            }
        }
    }
}
