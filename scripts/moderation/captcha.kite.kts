val mm = MiniMessage.miniMessage()

val pendingCaptcha = java.util.concurrent.ConcurrentHashMap<java.util.UUID, String>()
val HEAD_TEXTURES = listOf(
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFiYzJiY2ZiMmJkMzc1OWU2YjFlODZmYzdhNzk1ODVlMTEyN2RkMzU3ZmMyMDI4OTNmOWRlMjQxYmM2ZTUzMCJ9fX0=",
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNkOWVlZWU4ODM0Njg4ODFkODM4NDhhNDZiZjMwMTI0ODVjMGYwODljMGU4MTYxMDM2MzlmODlkMGUyMmVmYiJ9fX0=",
    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI2NjRhZTU4OGE1N2I1NjBjMmNkMWYwZTBlZjQ5MzRhZmQ2MjBiMzJiZDMwMzliMzI4NjVmZjliMjU5YSJ9fX0="
)

fun generateCaptchaCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

on<PlayerJoinEvent> { event ->
    val player = event.player
    if (player.hasPlayedBefore()) return@on

    val code = generateCaptchaCode()
    pendingCaptcha[player.uniqueId] = code

    server.scheduler.runTask(20L) {
        if (!player.isOnline) return@runTask
        player.sendMessage(mm.deserialize(""))
        player.sendMessage(mm.deserialize("<gold><bold>Verification Required"))
        player.sendMessage(mm.deserialize("<gray>Please type the following code in chat to verify you are not a bot:"))
        player.sendMessage(mm.deserialize("<yellow><bold>$code"))
        player.sendMessage(mm.deserialize(""))
    }
}

on<io.papermc.paper.event.player.AsyncChatEvent> { event ->
    val player = event.player
    val expected = pendingCaptcha[player.uniqueId] ?: return@on

    val message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message())
    event.isCancelled = true

    if (message.trim().equals(expected, ignoreCase = true)) {
        pendingCaptcha.remove(player.uniqueId)
        server.scheduler.runTask(0L) {
            player.sendMessage(mm.deserialize("<green><bold>Verified! <gray>Welcome to the server!"))
        }
    } else {
        server.scheduler.runTask(0L) {
            player.sendMessage(mm.deserialize("<red>Incorrect code! Please try again."))
            player.sendMessage(mm.deserialize("<yellow><bold>$expected"))
        }
    }
}

on<PlayerCommandPreprocessEvent> { event ->
    if (pendingCaptcha.containsKey(event.player.uniqueId)) {
        event.isCancelled = true
        event.player.sendMessage(mm.deserialize("<red>Please complete the captcha first!"))
    }
}

on<PlayerMoveEvent> { event ->
    if (!pendingCaptcha.containsKey(event.player.uniqueId)) return@on
    if (event.from.blockX != event.to.blockX || event.from.blockZ != event.to.blockZ) {
        event.isCancelled = true
    }
}

on<PlayerQuitEvent> { event ->
    pendingCaptcha.remove(event.player.uniqueId)
}
