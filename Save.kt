package ru.castle.game

import kotlin.math.pow
import kotlin.random.Random

enum class Slot(val rus: String) {
    WEAPON("Оружие"), ARMOR("Броня"), HELM("Шлем"), BOOTS("Сапоги"),
    SHIELD("Щит"), RING("Кольцо"), AMULET("Амулет")
}

enum class Rarity(val rus: String, val color: Int, val mult: Double, val affixes: Int) {
    COMMON("Обычный", 0xFFB9BDC7.toInt(), 1.0, 0),
    UNCOMMON("Необычный", 0xFF4CAF50.toInt(), 1.18, 1),
    RARE("Редкий", 0xFF42A5F5.toInt(), 1.40, 2),
    EPIC("Эпический", 0xFFAB47BC.toInt(), 1.68, 3),
    LEGENDARY("Легендарный", 0xFFFFA726.toInt(), 2.05, 4)
}

class ItemDef(
    val id: String,
    val name: String,
    val slot: Slot,
    val icon: String,
    val baseAtk: Int,
    val baseDef: Int,
    val scaleStat: Int, // 0-сила 1-ловкость 2-интеллект (только оружие)
    val basePrice: Int,
    val minFloor: Int
)

object ItemTable {
    val defs = listOf(
        ItemDef("w1","Ржавый меч",Slot.WEAPON,"ic_sword",7,0,0,40,1),
        ItemDef("w2","Короткий меч",Slot.WEAPON,"ic_sword",10,0,0,70,1),
        ItemDef("w3","Кинжал охотника",Slot.WEAPON,"ic_dagger",11,0,1,90,2),
        ItemDef("w4","Лук следопыта",Slot.WEAPON,"ic_bow",13,0,1,120,2),
        ItemDef("w5","Длинный меч",Slot.WEAPON,"ic_sword",15,0,0,150,3),
        ItemDef("w6","Посох ученика",Slot.WEAPON,"ic_staff",14,0,2,150,3),
        ItemDef("w7","Боевой топор",Slot.WEAPON,"ic_axe",19,0,0,230,4),
        ItemDef("w8","Посох мага",Slot.WEAPON,"ic_staff",20,0,2,260,5),
        ItemDef("w9","Молот гнева",Slot.WEAPON,"ic_axe",25,0,0,380,6),
        ItemDef("w10","Клинок рыцаря",Slot.WEAPON,"ic_sword",27,0,0,460,7),
        ItemDef("w11","Топор берсерка",Slot.WEAPON,"ic_axe",32,0,0,640,9),
        ItemDef("w12","Посох архимага",Slot.WEAPON,"ic_staff",34,0,2,700,9),
        ItemDef("w13","Клинок королей",Slot.WEAPON,"ic_sword",40,0,0,1000,11),
        ItemDef("a1","Тканевый доспех",Slot.ARMOR,"ic_armor",0,3,0,35,1),
        ItemDef("a2","Кожаный доспех",Slot.ARMOR,"ic_armor",0,5,0,70,1),
        ItemDef("a3","Кольчуга",Slot.ARMOR,"ic_armor",0,8,0,140,2),
        ItemDef("a4","Латы",Slot.ARMOR,"ic_armor",0,12,0,260,4),
        ItemDef("a5","Доспех паладина",Slot.ARMOR,"ic_armor",0,17,0,480,7),
        ItemDef("a6","Драконья чешуя",Slot.ARMOR,"ic_armor",0,23,0,820,10),
        ItemDef("h1","Капюшон",Slot.HELM,"ic_helm",0,1,0,20,1),
        ItemDef("h2","Кожаный шлем",Slot.HELM,"ic_helm",0,2,0,45,1),
        ItemDef("h3","Стальной шлем",Slot.HELM,"ic_helm",0,4,0,100,2),
        ItemDef("h4","Рогатый шлем",Slot.HELM,"ic_helm",0,6,0,210,4),
        ItemDef("h5","Корона власти",Slot.HELM,"ic_helm",0,9,0,420,8),
        ItemDef("b1","Сандалии",Slot.BOOTS,"ic_boots",0,1,0,20,1),
        ItemDef("b2","Кожаные сапоги",Slot.BOOTS,"ic_boots",0,2,0,45,1),
        ItemDef("b3","Сапоги странника",Slot.BOOTS,"ic_boots",0,3,0,90,2),
        ItemDef("b4","Латные сапоги",Slot.BOOTS,"ic_boots",0,5,0,190,4),
        ItemDef("b5","Сапоги ветра",Slot.BOOTS,"ic_boots",0,7,0,380,8),
        ItemDef("s1","Деревянный щит",Slot.SHIELD,"ic_shield",0,2,0,30,1),
        ItemDef("s2","Круглый щит",Slot.SHIELD,"ic_shield",0,4,0,80,2),
        ItemDef("s3","Турельный щит",Slot.SHIELD,"ic_shield",0,6,0,170,3),
        ItemDef("s4","Щит героя",Slot.SHIELD,"ic_shield",0,9,0,360,7),
        ItemDef("r1","Медное кольцо",Slot.RING,"ic_ring",0,0,0,30,1),
        ItemDef("r2","Серебряное кольцо",Slot.RING,"ic_ring",0,0,0,80,2),
        ItemDef("r3","Золотое кольцо",Slot.RING,"ic_ring",0,0,0,170,4),
        ItemDef("r4","Кольцо тени",Slot.RING,"ic_ring",0,0,0,340,8),
        ItemDef("m1","Оберег",Slot.AMULET,"ic_amulet",0,0,0,40,1),
        ItemDef("m2","Амулет мага",Slot.AMULET,"ic_amulet",0,0,0,110,3),
        ItemDef("m3","Амулет жизни",Slot.AMULET,"ic_amulet",0,0,0,230,5),
        ItemDef("m4","Амулет дракона",Slot.AMULET,"ic_amulet",0,0,0,450,9)
    )

    fun byId(id: String): ItemDef = defs.first { it.id == id }
}

val AFFIX_LABELS = mapOf(
    "str" to "Сила", "agi" to "Ловкость", "int" to "Интеллект", "vit" to "Выносливость",
    "hp" to "Здоровье", "mp" to "Мана", "atk" to "Атака", "def" to "Защита", "crit" to "Крит. шанс"
)

class ItemInstance(
    val def: ItemDef,
    val rarity: Rarity,
    val level: Int,
    val affixes: HashMap<String, Int>
) {
    fun powerMult(): Double = rarity.mult * (1.0 + level * 0.10)

    fun atkPower(): Int = (def.baseAtk * powerMult()).toInt()
    fun defValue(): Int = (def.baseDef * powerMult()).toInt()
    fun bonus(key: String): Int = affixes[key] ?: 0

    fun price(): Int = (def.basePrice * rarity.mult * (1.0 + level * 0.08)).toInt()

    fun displayName(): String =
        if (rarity == Rarity.COMMON) def.name else rarity.rus + " " + def.name

    fun statLines(): List<String> {
        val lines = ArrayList<String>()
        if (def.slot == Slot.WEAPON) {
            val statName = when (def.scaleStat) { 0 -> "Сила"; 1 -> "Ловкость"; else -> "Интеллект" }
            lines.add("Урон: $atkPower (масштаб: $statName)")
        }
        if (defValue() > 0) lines.add("Защита: ${defValue()}")
        val order = listOf("str","agi","int","vit","hp","mp","atk","def","crit")
        for (k in order) {
            val v = affixes[k] ?: continue
            lines.add("+${v} ${AFFIX_LABELS[k]}")
        }
        return lines
    }
}

object ItemGen {
    private val affixKeys = listOf("str","agi","int","vit","hp","mp","atk","def","crit")

    fun rollRarity(floor: Int, luck: Double = 1.0): Rarity {
        val bonus = 1.0 + floor * 0.06
        val r = Random.nextDouble() * 100.0 / (luck * bonus)
        return when {
            r < 1.6 -> Rarity.LEGENDARY
            r < 7.0 -> Rarity.EPIC
            r < 22.0 -> Rarity.RARE
            r < 55.0 -> Rarity.UNCOMMON
            else -> Rarity.COMMON
        }
    }

    fun roll(floor: Int, forceRarity: Rarity? = null, slot: Slot? = null): ItemInstance {
        val avail = ItemTable.defs.filter { it.minFloor <= floor + 1 }
        val def = if (slot != null) avail.filter { it.slot == slot }.random()
                  else avail.random()
        val rarity = forceRarity ?: rollRarity(floor)
        val level = (floor + Random.nextInt(0, 3)).coerceAtLeast(1)
        val aff = HashMap<String, Int>()
        val pool = affixKeys.toMutableList()
        repeat(rarity.affixes) {
            if (pool.isEmpty()) return@repeat
            val k = pool.removeAt(Random.nextInt(pool.size))
            val base = when (k) {
                "hp" -> 8 + level * 3
                "mp" -> 4 + level * 2
                "atk" -> 1 + (level * 0.7).toInt()
                "def" -> 1 + (level * 0.5).toInt()
                "crit" -> 1 + Random.nextInt(2)
                else -> 1 + (level * 0.55).toInt()
            }
            aff[k] = (base * (0.7 + Random.nextDouble() * 0.6) * rarity.mult).toInt().coerceAtLeast(1)
        }
        return ItemInstance(def, rarity, level, aff)
    }

    fun shopStock(floor: Int): List<ItemInstance> {
        val list = ArrayList<ItemInstance>()
        repeat(4) { list.add(roll(floor, forceRarity = rollRarity(floor, 0.8))) }
        return list
    }
}

// ---------- enemies ----------

class EnemyDef(
    val id: String,
    val name: String,
    val sprite: String,
    val frames: Int,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseXp: Int,
    val baseGold: Int,
    val minFloor: Int,
    val boss: Boolean = false,
    val canHeal: Boolean = false,
    val canBurn: Boolean = false
)

object Bestiary {
    val defs = listOf(
        EnemyDef("skel","Скелет-воин","skel_",3, 34, 9, 2, 20, 12, 1),
        EnemyDef("gob","Гоблин-разбойник","goblin_",3, 28, 11, 1, 18, 14, 1),
        EnemyDef("bat","Теневой нетопырь","bat_",3, 24, 13, 0, 22, 10, 2, canBurn = true),
        EnemyDef("golem","Каменный страж","golem_",3, 70, 14, 6, 42, 26, 3),
        EnemyDef("cult","Темный культист","cultist_",3, 48, 17, 3, 46, 34, 4, canHeal = true),
        EnemyDef("necro","Некромант","necro_",4, 260, 26, 8, 300, 220, 3, boss = true, canHeal = true),
        EnemyDef("lord","Каменный лорд","golem_",3, 460, 34, 14, 520, 420, 6, boss = true),
        EnemyDef("master","Повелитель культистов","cultist_",3, 640, 42, 12, 800, 640, 9, boss = true, canHeal = true, canBurn = true),
        EnemyDef("dragon","Дракон Теней","dragon_",4, 1100, 55, 18, 1500, 1500, 12, boss = true, canBurn = true)
    )

    fun byId(id: String) = defs.first { it.id == id }

    fun rollPack(floor: Int, elite: Boolean): List<EnemyDef> {
        val avail = defs.filter { !it.boss && it.minFloor <= floor }
        val count = if (elite) 2 else Random.nextInt(1, 3)
        return List(count) { avail.random() }
    }
}

object Balance {
    fun xpForLevel(level: Int): Int = (95.0 * level.toDouble().pow(1.55)).toInt()

    fun scaleHp(base: Int, floor: Int, boss: Boolean): Int {
        val v = base * (1.0 + (floor - 1) * 0.28)
        return (if (boss) v * 3.2 else v).toInt()
    }
    fun scaleAtk(base: Int, floor: Int): Int = (base * (1.0 + (floor - 1) * 0.20)).toInt()
    fun scaleDef(base: Int, floor: Int): Int = (base * (1.0 + (floor - 1) * 0.15)).toInt()
    fun scaleXp(base: Int, floor: Int, elite: Boolean): Int {
        val v = base * (1.0 + (floor - 1) * 0.22)
        return (if (elite) v * 2.2 else v).toInt()
    }
    fun scaleGold(base: Int, floor: Int): Int = (base * (1.0 + (floor - 1) * 0.25)).toInt()
}
