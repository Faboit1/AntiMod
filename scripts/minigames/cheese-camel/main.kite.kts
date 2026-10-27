@file:Import("camel-config.kite.kts")
@file:Import("camel-effects.kite.kts")

val mm = MiniMessage.miniMessage()

val CHANCE_MYTHIC = 1
val CHANCE_LEGENDARY = 4
val CHANCE_EPIC = 10
val CHANCE_RARE = 60
val MAX_CAMELS = 10
val SPAWN_PADDING = 15

val pos1Key = NamespacedKey(plugin, "cheese-camel-pos1")
val pos2Key = NamespacedKey(plugin, "cheese-camel-pos2")
val camelUidKey = NamespacedKey(plugin, "cheese-camel-uid")
val camelTierKey = NamespacedKey(plugin, "cheese-camel-tier")
val camelMaxHpKey = NamespacedKey(plugin, "cheese-camel-maxhp")

val killCounts = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Int>()
val killNames = java.util.concurrent.ConcurrentHashMap<java.util.UUID, String>()

fun getPos(key: NamespacedKey): Location? {
    val data = server.worlds[0].persistentDataContainer.get(key, PersistentDataType.STRING) ?: return null
    val parts = data.split(",")
    if (parts.size < 4) return null
    val world = server.getWorld(parts[0]) ?: return null
    return Location(world, parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
}

fun setPos(key: NamespacedKey, loc: Location) {
    server.worlds[0].persistentDataContainer.set(key, PersistentDataType.STRING,
        "${loc.world.name},${loc.x},${loc.y},${loc.z}")
}

fun getRandomCamelLocation(): Location {
    val p1 = getPos(pos1Key) ?: return server.worlds[0].spawnLocation
    val p2 = getPos(pos2Key) ?: return server.worlds[0].spawnLocation

    val minX = minOf(p1.blockX, p2.blockX) + SPAWN_PADDING
    val maxX = maxOf(p1.blockX, p2.blockX) - SPAWN_PADDING
    val minZ = minOf(p1.blockZ, p2.blockZ) + SPAWN_PADDING
    val maxZ = maxOf(p1.blockZ, p2.blockZ) - SPAWN_PADDING

    val x = (minX..maxX).random()
    val z = (minZ..maxZ).random()
    val loc = p1.world.getHighestBlockAt(x, z).location
    return loc.add(0.5, 2.0, 0.5)
}

fun determineTier(forced: String = "random"): String {
    if (forced != "random") return forced
    val roll = (1..1000).random()
    return when {
        roll <= CHANCE_MYTHIC -> "Mythic"
        roll <= CHANCE_MYTHIC + CHANCE_LEGENDARY -> "Legendary"
        roll <= CHANCE_MYTHIC + CHANCE_LEGENDARY + CHANCE_EPIC -> "Epic"
        roll <= CHANCE_MYTHIC + CHANCE_LEGENDARY + CHANCE_EPIC + CHANCE_RARE -> "Rare"
        else -> "Common"
    }
}

fun getTierColor(tier: String): String = when (tier) {
    "Mythic" -> "<light_purple>"
    "Legendary" -> "<gold>"
    "Epic" -> "<dark_purple>"
    "Rare" -> "<aqua>"
    else -> "<white>"
}

fun getTierStats(tier: String): Pair<Int, Double> = when (tier) {
    "Mythic" -> 16000 to 5.0
    "Legendary" -> 5000 to 2.5
    "Epic" -> 2600 to 1.8
    "Rare" -> 600 to 1.3
    else -> 100 to 1.0
}

fun updateCamelName(camel: org.bukkit.entity.Camel) {
    val tier = camel.persistentDataContainer.get(camelTierKey, PersistentDataType.STRING) ?: "Common"
    val maxHp = camel.persistentDataContainer.get(camelMaxHpKey, PersistentDataType.INTEGER) ?: 100
    val pct = camel.health / camel.getAttribute(Attribute.MAX_HEALTH)!!.value
    val display = (pct * maxHp).toInt()
    val color = getTierColor(tier)
    camel.customName(mm.deserialize("$color$tier Cheese Camel <red>${display}❤"))
    camel.isCustomNameVisible = true
}

fun spawnCheeseCamel(loc: Location, forcedTier: String = "random") {
    val tier = determineTier(forcedTier)
    val (trueHP, size) = getTierStats(tier)
    val realMax = minOf(trueHP, 1000).toDouble()

    val camel = loc.world.spawn(loc, org.bukkit.entity.Camel::class.java) { entity ->
        entity.addScoreboardTag("cheese_dropper")
        entity.addScoreboardTag("cheese_tier_$tier")
        entity.persistentDataContainer.set(camelTierKey, PersistentDataType.STRING, tier)
        entity.persistentDataContainer.set(camelMaxHpKey, PersistentDataType.INTEGER, trueHP)
        entity.persistentDataContainer.set(camelUidKey, PersistentDataType.STRING, java.util.UUID.randomUUID().toString())
        entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = realMax
        entity.health = realMax
        entity.getAttribute(Attribute.SCALE)?.baseValue = size
    }
    updateCamelName(camel)

    if (tier in listOf("Mythic", "Legendary", "Epic")) {
        camel.addScoreboardTag("has_bossbar")
        val barColor = when (tier) {
            "Mythic" -> BarColor.PURPLE
            "Legendary" -> BarColor.YELLOW
            else -> BarColor.BLUE
        }
        val bar = server.createBossBar("$tier Cheese Camel", barColor, BarStyle.SOLID)
        bar.progress = 1.0
        val barKey = NamespacedKey(plugin, "camel-bar-${camel.uniqueId}")
        camel.persistentDataContainer.set(barKey, PersistentDataType.STRING, "active")
    }
}

onLoad {
    server.scheduler.runTaskTimer(100L, 100L) {
        val p1 = getPos(pos1Key)
        val p2 = getPos(pos2Key)
        var count = 0

        server.worlds[0].getEntitiesByClass(org.bukkit.entity.Camel::class.java).forEach { camel ->
            if (camel.scoreboardTags.contains("cheese_dropper")) {
                count++
                if (p1 != null && p2 != null) {
                    val minX = minOf(p1.blockX, p2.blockX)
                    val maxX = maxOf(p1.blockX, p2.blockX)
                    val minZ = minOf(p1.blockZ, p2.blockZ)
                    val maxZ = maxOf(p1.blockZ, p2.blockZ)
                    val loc = camel.location
                    if (loc.blockX < minX || loc.blockX > maxX || loc.blockZ < minZ || loc.blockZ > maxZ) {
                        camel.teleport(getRandomCamelLocation())
                    }
                }
            }
        }

        if (count < MAX_CAMELS) {
            spawnCheeseCamel(getRandomCamelLocation())
        }
    }
}

on<EntityDamageEvent> { event ->
    val camel = event.entity as? org.bukkit.entity.Camel ?: return@on
    if (!camel.scoreboardTags.contains("cheese_dropper")) return@on

    if (event.cause == EntityDamageEvent.DamageCause.FALL) {
        event.isCancelled = true
        return@on
    }

    val maxHp = camel.persistentDataContainer.get(camelMaxHpKey, PersistentDataType.INTEGER) ?: 100
    if (maxHp > 1000) {
        val ratio = 1000.0 / maxHp
        event.damage = event.damage * ratio
    }

    server.scheduler.runTask(1L) {
        if (camel.isDead) return@runTask
        updateCamelName(camel)
    }
}

on<EntityDeathEvent> { event ->
    val camel = event.entity as? org.bukkit.entity.Camel ?: return@on
    if (!camel.scoreboardTags.contains("cheese_dropper")) return@on
    event.drops.clear()

    val tier = camel.persistentDataContainer.get(camelTierKey, PersistentDataType.STRING) ?: "Common"
    val attacker = camel.killer

    if (attacker != null) {
        val uuid = attacker.uniqueId
        killCounts[uuid] = (killCounts[uuid] ?: 0) + 1
        killNames[uuid] = attacker.name
    }

    val loc = camel.location.add(0.0, 1.2, 0.0)
    val amount = when (tier) {
        "Mythic" -> 5000
        "Legendary" -> 1200
        "Epic" -> 150
        "Rare" -> 20
        else -> 2
    }

    val cheeseItem = ItemStack(Material.BEE_SPAWN_EGG).apply {
        itemMeta = itemMeta?.also {
            it.displayName(mm.deserialize("<yellow>Cheese Bee"))
            it.lore(listOf(mm.deserialize("<gray>A Currency")))
        }
    }

    if (tier == "Mythic") {
        server.broadcast(mm.deserialize(""))
        server.broadcast(mm.deserialize("<dark_purple><bold><obfuscated>|||</obfuscated> <light_purple><bold>MYTHIC CHEESE CAMEL SLAIN</bold> <dark_purple><bold><obfuscated>|||</obfuscated>"))
        server.broadcast(mm.deserialize("<gray>Defeated by <white>${attacker?.name ?: "Unknown"}</white> in an epic battle!"))
        server.broadcast(mm.deserialize(""))
        loc.world.strikeLightningEffect(loc)
        loc.world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.5f)
    } else if (tier == "Legendary") {
        loc.world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f)
        loc.world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
    } else if (tier == "Epic") {
        loc.world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f)
    } else if (tier == "Rare") {
        loc.world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f)
    }

    var dropped = 0
    val perTick = when (tier) {
        "Mythic" -> 3
        "Legendary" -> 30
        "Epic" -> 5
        "Rare" -> 1
        else -> 1
    }
    val loops = amount / perTick
    val vPower = when (tier) {
        "Mythic" -> 0.95
        "Legendary" -> 0.7
        "Epic" -> 0.5
        "Rare" -> 0.3
        else -> 0.15
    }

    server.scheduler.runTaskTimer(1L, 0L) { task ->
        if (dropped >= loops) {
            val rem = amount - (loops * perTick)
            if (rem > 0) loc.world.dropItem(loc, cheeseItem.clone().apply { this.amount = rem })
            task.cancel()
            return@runTaskTimer
        }

        val dropItem = loc.world.dropItem(loc, cheeseItem.clone().apply { this.amount = perTick })
        val angle = Math.toRadians((dropped * 35).toDouble())
        val vx = Math.cos(angle) * vPower
        val vz = Math.sin(angle) * vPower
        val vy = if (tier == "Mythic") 0.8 else if (tier == "Legendary") 0.6 else Math.random() * vPower
        dropItem.velocity = org.bukkit.util.Vector(vx, vy, vz)
        dropped++
    }
}

command("cpos1") {
    permission = "op"
    description = "Set cheese camel boundary position 1"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        setPos(pos1Key, sender.location)
        sender.sendMessage(mm.deserialize("<green>Position 1 set."))
    }
}

command("cpos2") {
    permission = "op"
    description = "Set cheese camel boundary position 2"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        setPos(pos2Key, sender.location)
        sender.sendMessage(mm.deserialize("<green>Position 2 set."))
    }
}

command("summoncheesecamel") {
    permission = "op"
    description = "Summon a cheese camel"

    execute { sender, args ->
        if (sender !is Player) return@execute
        val tier = args.getOrNull(0) ?: "random"
        spawnCheeseCamel(sender.location, tier)
        sender.sendMessage(mm.deserialize("<green>Summoned a cheese camel!"))
    }

    tabComplete { _, args ->
        if (args.size == 1) listOf("Mythic", "Legendary", "Epic", "Rare", "Common").filter { it.startsWith(args[0], ignoreCase = true) }
        else emptyList()
    }
}

command("camelkills") {
    description = "Check your cheese camel kills"

    execute { sender, _ ->
        if (sender !is Player) return@execute
        val kills = killCounts[sender.uniqueId] ?: 0
        sender.sendMessage(mm.deserialize("<yellow>You have killed <gold>$kills</gold> Cheese Camels."))
    }
}

command("killallcheesecamels") {
    permission = "op"
    description = "Kill all cheese camels"

    execute { sender, _ ->
        server.worlds[0].getEntitiesByClass(org.bukkit.entity.Camel::class.java).forEach { camel ->
            if (camel.scoreboardTags.contains("cheese_dropper")) {
                camel.remove()
            }
        }
        sender.sendMessage(mm.deserialize("<red>All cheese camels killed."))
    }
}
