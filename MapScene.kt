package ru.castle.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object Assets {
    private val map = HashMap<String, Bitmap>()
    private val paintOpts = BitmapFactory.Options().apply { inScaled = false }

    fun loadAll(ctx: Context) {
        val names = listOf(
            "hero_idle_0","hero_idle_1","hero_idle_2","hero_idle_3",
            "hero_walk_0","hero_walk_1","hero_walk_2","hero_walk_3",
            "hero_atk_0","hero_atk_1",
            "skel_0","skel_1","skel_2",
            "goblin_0","goblin_1","goblin_2",
            "bat_0","bat_1","bat_2",
            "golem_0","golem_1","golem_2",
            "cultist_0","cultist_1","cultist_2",
            "necro_0","necro_1","necro_2","necro_3",
            "dragon_0","dragon_1","dragon_2","dragon_3",
            "floor","wall","door","stairs","throne",
            "chest_0","chest_1","fire_0","fire_1","torch_0","torch_1",
            "ic_sword","ic_axe","ic_dagger","ic_bow","ic_staff",
            "ic_armor","ic_helm","ic_boots","ic_shield","ic_ring","ic_amulet",
            "ic_potion","ic_scroll"
        )
        for (n in names) {
            val id = ctx.resources.getIdentifier(n, "drawable", ctx.packageName)
            if (id != 0) {
                map[n] = BitmapFactory.decodeResource(ctx.resources, id, paintOpts)
            }
        }
    }

    fun get(name: String): Bitmap = map[name] ?: map["ic_scroll"]!!
}
