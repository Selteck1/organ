package ru.castle.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

class BattleScene(
    private val gv: GameView,
    private val room: Room,
    private val onVictory: () -> Unit
) : Scene {
    private val floor = gv.floor
    private val elite = room.type == RoomType.ELITE
    private val isBoss = room.type == RoomType.BOSS
    private val enemies = ArrayList<Enemy>()
    private val fx = Fx()

    private var time = 0f
    private var phase = 0          // 0-ждём ввод, 1-анимация игрока, 10-ход врагов, 2-победа, 3-поражение
    private var timer = 0f
    private var enemyIdx = 0
    private var enemyActCount = 0
    private var logMsg = ""
    private var shake = 0f
    private var lunge = 0f
    private var showSkills = false
    private var pBurnTurns = 0
    private var pBurnDmg = 0
    private var bossHealCd = 0
    private var winPrepared = false
    private var winLines = listOf<String>()
    private var dropItem: ItemInstance? = null

    private val atkBtn = Btn(620f, 630f, 150f, 64f, "Атака", 0xFF7A3030.toInt())
    private val skillBtn = Btn(785f, 630f, 150f, 64f, "Навыки")
    private val potBtn = Btn(950f, 630f, 150f, 64f, "Зелье")
    private val fleeBtn = Btn(1115f, 630f, 145f, 64f, "Бежать", 0xFF444A5E.toInt())
    private val skill1 = Btn(620f, 550f, 200f, 60f, "Мощный удар 15")
    private val skill2 = Btn(840f, 550f, 200f, 60f, "Огненный шар 20")
    private val skill3 = Btn(1060f, 550f, 200f, 60f, "Исцеление 25")
    private val contBtn = Btn(500f, 520f, 280f, 70f, "Продолжить")

    private val p get() = gv.player

    init {
        if (isBoss) {
            val bossId = when { floor >= 12 -> "dragon"; floor >= 9 -> "master"; floor >= 6 -> "lord"; else -> "necro" }
            enemies.add(Enemy(Bestiary.byId(bossId), floor, true))
            logMsg = "Босс: ${enemies[0].name}!"
        } else {
            for (d in Bestiary.rollPack(floor, elite)) enemies.add(Enemy(d, floor, elite))
            logMsg = if (elite) "Элитный отряд преграждает путь!" else "Враги нападают!"
        }
    }

    private fun aliveEnemies() = enemies.filter { it.isAlive }

    override fun update(dt: Float, gv: GameView) {
        time += dt
        fx.update(dt)
        shake = (shake - dt).coerceAtLeast(0f)
        lunge = (lunge - dt).coerceAtLeast(0f)
        for (e in enemies) e.flash = (e.flash - dt).coerceAtLeast(0f)

        when (phase) {
            1 -> {
                timer -= dt
                if (timer <= 0f) {
                    if (aliveEnemies().isEmpty()) { prepareWin(); phase = 2; timer = 0.6f }
                    else { phase = 10; enemyIdx = 0; timer = 0.3f }
                }
            }
            10 -> {
                timer -= dt
                if (timer <= 0f) {
                    val alive = aliveEnemies()
                    if (enemyIdx < alive.size) {
                        actEnemy(alive[enemyIdx])
                        enemyIdx++
                        timer = 0.8f
                        if (p.hp <= 0) { prepareLose(); phase = 3; timer = 0.8f }
                    } else {
                        if (pBurnTurns > 0) {
                            pBurnTurns--
                            val d = p.takeDamage(pBurnDmg)
                            fx.dmg(playerX() + 60, playerY() - 40, d, false)
                        }
                        if (p.hp <= 0) { prepareLose(); phase = 3; timer = 0.8f }
                        else { phase = 0; showSkills = false }
                    }
                }
            }
            2 -> { timer -= dt }
            3 -> { timer -= dt }
        }
    }

    private fun playerX() = 230f
    private fun playerY() = 420f

    private fun enemyPos(idx: Int, count: Int): Pair<Float, Float> {
        if (isBoss) return 830f to 300f
        val xs = when (count) {
            1 -> listOf(880f)
            2 -> listOf(790f, 990f)
            else -> listOf(720f, 890f, 1060f)
        }
        return xs[idx.coerceAtMost(xs.size - 1)] to 330f
    }

    private fun actEnemy(e: Enemy) {
        enemyActCount++
        if (bossHealCd > 0) bossHealCd--
        if (e.def.boss && e.def.canHeal && e.hp < e.maxHp * 0.4 && bossHealCd == 0) {
            val h = e.maxHp / 6
            e.hp = (e.hp + h).coerceAtMost(e.maxHp)
            bossHealCd = 3
            fx.heal(enemyX(e), 260f, h)
            logMsg = "${e.name} восстанавливает силы!"
            return
        }
        val heavy = if (e.def.boss) enemyActCount % 3 == 0 else Random.nextInt(100) < 22
        val mult = if (heavy) 1.55 else 1.0
        val raw = (e.atk * mult * (0.85 + Random.nextDouble() * 0.3)).toInt()
        val dmg = p.takeDamage(raw)
        shake = 0.35f
        fx.dmg(playerX() + 60, playerY() - 60, dmg, false)
        fx.burst(playerX() + 60, playerY() - 30, 0xFFEF5350.toInt(), 8)
        logMsg = if (heavy) "${e.name}: мощный удар!" else "${e.name} атакует!"
        if (e.def.canBurn && Random.nextInt(100) < 30) {
            pBurnTurns = 2
            pBurnDmg = (e.atk * 0.25).toInt().coerceAtLeast(1)
            fx.info(playerX() + 60, playerY() - 110, "Вы горите!")
        }
    }

    private fun enemyX(e: Enemy): Float {
        val alive = aliveEnemies()
        val idx = enemies.indexOf(e)
        val pos = alive.indexOf(e)
        val (x, _) = enemyPos(if (pos >= 0) pos else idx, alive.size.coerceAtLeast(1))
        return x + 50
    }

    private fun strike(mult: Double, mpCost: Int, all: Boolean, burn: Boolean) {
        if (p.mp < mpCost) { logMsg = "Не хватает маны!"; return }
        p.mp -= mpCost
        lunge = 0.4f
        val targets = if (all) aliveEnemies() else listOf(aliveEnemies().first())
        var critAny = false
        for (e in targets) {
            val crit = Random.nextInt(100) < p.critChance()
            critAny = critAny || crit
            val dmg = (p.attackPower() * mult * (if (crit) 2.0 else 1.0) * (0.88 + Random.nextDouble() * 0.24)).toInt()
            val done = e.takeDamage(dmg)
            fx.dmg(enemyX(e), 240f, done, crit)
            fx.burst(enemyX(e), 300f, if (burn) 0xFFFF9800.toInt() else 0xFFFFD54F.toInt(), 10)
            if (burn) {
                e.burnTurns = 3
                e.burnDmg = (p.attackPower() * 0.22).toInt().coerceAtLeast(1)
            }
        }
        for (e in aliveEnemies().toList()) {
            val bd = e.tickBurn()
            if (bd > 0) { fx.dmg(enemyX(e), 280f, bd, false); fx.burst(enemyX(e), 320f, 0xFFFF9800.toInt(), 6) }
        }
        logMsg = when {
            burn -> "Огненный шар испепеляет врагов!"
            mult > 1.5 -> "Мощный удар!" + (if (critAny) " Крит!" else "")
            else -> "Вы атакуете." + (if (critAny) " Критический удар!" else "")
        }
        phase = 1; timer = 0.45f
    }

    private fun prepareWin() {
        if (winPrepared) return
        winPrepared = true
        var xp = 0; var gold = 0
        for (e in enemies) { xp += e.xpReward; gold += e.goldReward }
        p.gold += gold
        val lvls = p.addXp(xp)
        val lines = mutableListOf("Получено опыта: $xp", "Получено золота: $gold")
        if (lvls > 0) lines.add("НОВЫЙ УРОВЕНЬ! (+${lvls * 3} очков характеристик)")
        val dropChance = if (isBoss) 100 else if (elite) 85 else 48
        if (Random.nextInt(100) < dropChance) {
            dropItem = ItemGen.roll(floor, luck = if (isBoss) 2.2 else 1.3)
            val it = dropItem!!
            if (p.addItem(it)) lines.add("Добыча: ${it.displayName()}")
            else { p.gold += it.price() / 2; lines.add("Добыча продана (инвентарь полон): +${it.price() / 2} з.") }
        }
        if (isBoss) lines.add("Путь вниз открыт!")
        winLines = lines
    }

    private fun prepareLose() {
        p.gold -= p.gold * 3 / 10
        p.hp = p.maxHp() / 2
        p.mp = p.maxMp() / 2
        pBurnTurns = 0
    }

    override fun render(c: Canvas, gv: GameView) {
        Ui.bg(c, 1280f, 720f)
        val ox = if (shake > 0f) (Random.nextFloat() - 0.5f) * 18f else 0f
        val oy = if (shake > 0f) (Random.nextFloat() - 0.5f) * 12f else 0f
        c.save()
        c.translate(ox, oy)
        // walls + floor
        var wx = 0f
        while (wx < 1280f) { Ui.sprite(c, "wall", wx, 40f, 5f); wx += 160f }
        var fxx = -40f
        while (fxx < 1280f) { Ui.sprite(c, "floor", fxx, 500f, 6f); fxx += 192f }
        // torches
        Ui.sprite(c, if (time % 0.4f < 0.2f) "torch_0" else "torch_1", 60f, 220f, 3f)
        Ui.sprite(c, if (time % 0.4f < 0.2f) "torch_1" else "torch_0", 1140f, 220f, 3f)

        // enemies
        val alive = aliveEnemies()
        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.isAlive) continue
            val (ex, ey) = enemyPos(alive.indexOf(e), alive.size.coerceAtLeast(1))
            val frame = ((time * 3).toInt() + i) % e.def.frames
            val scale = if (e.def.boss) { if (e.def.id == "dragon") 3.4f else 3.2f } else 3.4f
            val bob = if (e.def.id == "necro") kotlin.math.sin(time * 2).toFloat() * 8f else 0f
            Ui.spriteFlash(c, e.def.sprite + frame, ex, ey + bob, scale, e.flash)
            // hp bar
            Ui.bar(c, ex - 10, ey - 34 + bob, 140f, 20f, e.hp.toFloat() / e.maxHp,
                if (e.def.boss) 0xFFAB47BC.toInt() else 0xFFEF5350.toInt(),
                "${e.name} ${e.hp}")
            if (e.burnTurns > 0) Ui.text(c, "🔥".let { "" } + "Горит (${e.burnTurns})", ex + 60, ey - 44 + bob, 18f, 0xFFFF9800.toInt(), center = true)
        }
        // player
        val pframe = when {
            lunge > 0.25f -> "hero_atk_0"
            lunge > 0f -> "hero_atk_1"
            else -> "hero_idle_" + ((time * 2).toInt() % 4)
        }
        val lungeDx = if (lunge > 0f) (0.4f - lunge) * 260f else 0f
        Ui.sprite(c, pframe, playerX() + lungeDx, playerY() - 80, 4.4f)
        c.restore()

        // HUD
        Ui.bar(c, 20f, 16f, 300f, 30f, p.hp.toFloat() / p.maxHp(), 0xFFEF5350.toInt(), "HP ${p.hp}/${p.maxHp()}")
        Ui.bar(c, 20f, 52f, 300f, 30f, p.mp.toFloat() / p.maxMp(), 0xFF42A5F5.toInt(), "MP ${p.mp}/${p.maxMp()}")
        if (pBurnTurns > 0) Ui.text(c, "Горение: $pBurnTurns ходов", 20f, 110f, 22f, 0xFFFF9800.toInt())
        Ui.text(c, logMsg, 640f, 120f, 30f, Color.WHITE, center = true)

        fx.draw(c)

        when (phase) {
            0 -> {
                potBtn.label = "Зелье (${p.potions})"
                potBtn.enabled = p.potions > 0
                fleeBtn.enabled = !isBoss
                skill1.enabled = p.mp >= 15
                skill2.enabled = p.mp >= 20
                skill3.enabled = p.mp >= 25
                atkBtn.draw(c); skillBtn.draw(c); potBtn.draw(c); fleeBtn.draw(c)
                if (showSkills) { skill1.draw(c); skill2.draw(c); skill3.draw(c) }
            }
            2 -> {
                c.drawRect(0f, 0f, 1280f, 720f, P.dim)
                Ui.panel(c, 340f, 160f, 600f, 460f)
                Ui.text(c, "ПОБЕДА!", 640f, 230f, 52f, 0xFF66BB6A.toInt(), center = true)
                var y = 300f
                for (ln in winLines) { Ui.text(c, ln, 640f, y, 27f, Color.WHITE, center = true); y += 42f }
                contBtn.draw(c)
            }
            3 -> {
                c.drawRect(0f, 0f, 1280f, 720f, P.dim)
                Ui.panel(c, 340f, 200f, 600f, 340f)
                Ui.text(c, "ВЫ ПОГИБЛИ", 640f, 280f, 48f, 0xFFEF5350.toInt(), center = true)
                Ui.text(c, "Тьма отступает... на время.", 640f, 350f, 28f, Color.WHITE, center = true)
                Ui.text(c, "Потеряно 30% золота. Вы очнетесь с половиной здоровья.", 640f, 395f, 22f, 0xFFAAB2C8.toInt(), center = true)
                contBtn.draw(c)
            }
        }
    }

    override fun touch(x: Float, y: Float, gv: GameView) {
        when (phase) {
            2 -> { if (timer <= 0f && contBtn.hit(x, y)) { gv.pop(); onVictory() } }
            3 -> { if (timer <= 0f && contBtn.hit(x, y)) { gv.pop() } }
            0 -> {
                if (atkBtn.hit(x, y)) { strike(1.0, 0, all = false, burn = false) }
                else if (skillBtn.hit(x, y)) { showSkills = !showSkills }
                else if (potBtn.hit(x, y)) {
                    if (p.potions > 0) { p.potions--; val h = p.maxHp() * 2 / 5; p.heal(h); fx.heal(playerX() + 60, playerY() - 100, h); phase = 1; timer = 0.4f; logMsg = "Вы выпиваете зелье." }
                }
                else if (fleeBtn.hit(x, y)) {
                    if (!isBoss && Random.nextInt(100) < 55) { gv.pop() }
                    else { logMsg = "Не удалось сбежать!"; phase = 10; enemyIdx = 0; timer = 0.3f }
                }
                else if (showSkills && skill1.hit(x, y)) { strike(2.1, 15, all = false, burn = false) }
                else if (showSkills && skill2.hit(x, y)) { strike(1.3, 20, all = true, burn = true) }
                else if (showSkills && skill3.hit(x, y)) {
                    if (p.mp >= 25) { p.mp -= 25; val h = p.maxHp() * 35 / 100; p.heal(h); fx.heal(playerX() + 60, playerY() - 100, h); phase = 1; timer = 0.4f; logMsg = "Исцеление!" }
                }
            }
        }
    }
}
