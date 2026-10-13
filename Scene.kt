package ru.castle.game

import org.json.JSONArray
import org.json.JSONObject
import java.util.EnumMap
import kotlin.math.min
import kotlin.random.Random

class Player {
    var level = 1
    var xp = 0
    var gold = 60
    var str = 5; var agi = 5; var intl = 5; var vit = 5
    var freePoints = 0
    var potions = 3

    var hp = 100
    var mp = 50

    val equip = EnumMap<Slot, ItemInstance>(Slot::class.java)
    val inventory = ArrayList<ItemInstance>()

    fun maxHp(): Int {
        var v = 60 + vit * 9 + level * 7
        for (it in equip.values) v += it.bonus("hp")
        return v
    }
    fun maxMp(): Int {
        var v = 30 + intl * 5 + level * 3
        for (it in equip.values) v += it.bonus("mp")
        return v
    }

    fun statTotal(key: String): Int {
        var base = 0
        when (key) {
            "str" -> base = str; "agi" -> base = agi
            "int" -> base = intl; "vit" -> base = vit
        }
        for (it in equip.values) base += it.bonus(key)
        return base
    }

    fun attackPower(): Int {
        val w = equip[Slot.WEAPON]
        val base = w?.atkPower() ?: (3 + level)
        val stat = when (w?.def?.scaleStat ?: 0) { 0 -> statTotal("str"); 1 -> statTotal("agi"); else -> statTotal("int") }
        var extra = 0
        for (it in equip.values) extra += it.bonus("atk")
        return base + stat * 2 + extra
    }

    fun defense(): Int {
        var v = vit / 2
        for (it in equip.values) v += it.defValue() + it.bonus("def")
        return v
    }

    fun critChance(): Double {
        var c = 6.0 + statTotal("agi") * 0.4
        for (it in equip.values) c += it.bonus("crit") * 2.0
        return c.coerceAtMost(60.0)
    }

    fun healFull() { hp = maxHp(); mp = maxMp() }

    fun heal(v: Int) { hp = min(maxHp(), hp + v) }
    fun restoreMp(v: Int) { mp = min(maxMp(), mp + v) }

    fun takeDamage(raw: Int): Int {
        val dmg = (raw - defense()).coerceAtLeast(1)
        hp = (hp - dmg).coerceAtLeast(0)
        return dmg
    }

    // returns number of levels gained
    fun addXp(amount: Int): Int {
        xp += amount
        var gained = 0
        while (xp >= Balance.xpForLevel(level)) {
            xp -= Balance.xpForLevel(level)
            level++
            gained++
            freePoints += 3
        }
        if (gained > 0) {
            hp = maxHp()
            mp = maxMp()
        }
        return gained
    }

    fun addItem(item: ItemInstance): Boolean {
        if (inventory.size >= 24) return false
        inventory.add(item)
        return true
    }

    fun equipItem(item: ItemInstance): ItemInstance? {
        val old = equip[item.def.slot]
        equip[item.def.slot] = item
        inventory.remove(item)
        if (old != null) inventory.add(old)
        hp = min(hp, maxHp()); mp = min(mp, maxMp())
        return old
    }

    fun unequip(slot: Slot) {
        val it = equip[slot] ?: return
        if (inventory.size < 24) {
            equip.remove(slot)
            inventory.add(it)
            hp = min(hp, maxHp()); mp = min(mp, maxMp())
        }
    }

    fun sellPrice(item: ItemInstance): Int = item.price() / 3

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("level", level); o.put("xp", xp); o.put("gold", gold)
        o.put("str", str); o.put("agi", agi); o.put("int", intl); o.put("vit", vit)
        o.put("freePoints", freePoints); o.put("potions", potions)
        o.put("hp", hp); o.put("mp", mp)
        val inv = JSONArray()
        for (it in inventory) inv.put(itemToJson(it))
        o.put("inventory", inv)
        val eq = JSONObject()
        for ((s, it) in equip) eq.put(s.name, itemToJson(it))
        o.put("equip", eq)
        return o
    }

    private fun itemToJson(it: ItemInstance): JSONObject {
        val o = JSONObject()
        o.put("id", it.def.id)
        o.put("rarity", it.rarity.ordinal)
        o.put("level", it.level)
        val aff = JSONObject()
        for ((k, v) in it.affixes) aff.put(k, v)
        o.put("affixes", aff)
        return o
    }

    private fun itemFromJson(o: JSONObject): ItemInstance {
        val aff = HashMap<String, Int>()
        val keys = o.getJSONObject("affixes").keys()
        while (keys.hasNext()) {
            val k = keys.next()
            aff[k] = o.getJSONObject("affixes").getInt(k)
        }
        return ItemInstance(
            ItemTable.byId(o.getString("id")),
            Rarity.values()[o.getInt("rarity")],
            o.getInt("level"), aff
        )
    }

    fun fromJson(o: JSONObject) {
        level = o.getInt("level"); xp = o.getInt("xp"); gold = o.getInt("gold")
        str = o.getInt("str"); agi = o.getInt("agi"); intl = o.getInt("int"); vit = o.getInt("vit")
        freePoints = o.getInt("freePoints"); potions = o.getInt("potions")
        hp = o.getInt("hp"); mp = o.getInt("mp")
        inventory.clear()
        val inv = o.getJSONArray("inventory")
        for (i in 0 until inv.length()) inventory.add(itemFromJson(inv.getJSONObject(i)))
        equip.clear()
        val eq = o.getJSONObject("equip")
        val keys = eq.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            equip[Slot.valueOf(k)] = itemFromJson(eq.getJSONObject(k))
        }
        hp = hp.coerceAtMost(maxHp()); mp = mp.coerceAtMost(maxMp())
    }
}

class Enemy(val def: EnemyDef, floor: Int, private val elite: Boolean) {
    val name = def.name
    val maxHp: Int = Balance.scaleHp(def.baseHp, floor, def.boss)
    var hp = maxHp
    val atk: Int = Balance.scaleAtk(def.baseAtk, floor)
    val defV: Int = Balance.scaleDef(def.baseDef, floor)
    val xpReward: Int = Balance.scaleXp(def.baseXp, floor, elite)
    val goldReward: Int = Balance.scaleGold(def.baseGold, floor)

    var burnTurns = 0
    var burnDmg = 0
    var flash = 0f

    val isAlive: Boolean get() = hp > 0

    fun takeDamage(raw: Int): Int {
        val dmg = (raw - defV).coerceAtLeast(1)
        hp = (hp - dmg).coerceAtLeast(0)
        flash = 0.25f
        return dmg
    }

    fun tickBurn(): Int {
        if (burnTurns > 0 && hp > 0) {
            burnTurns--
            hp = (hp - burnDmg).coerceAtLeast(0)
            return burnDmg
        }
        return 0
    }
}
