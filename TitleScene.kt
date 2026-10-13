package ru.castle.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.SurfaceHolder

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    var player = Player()
    var floor = 1

    private val scenes = ArrayList<Scene>()
    private var mapRef: MapScene? = null
    private var thread: GameThread? = null
    private var scaleF = 1f
    private var lastTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun mapScene(): MapScene = mapRef ?: (scenes.firstOrNull { it is MapScene } as? MapScene)!!

    fun push(s: Scene) { scenes.add(s) }
    fun pop() { if (scenes.size > 1) scenes.removeAt(scenes.size - 1) }

    fun resetTo(s: Scene) {
        scenes.clear()
        scenes.add(s)
        if (s is MapScene) mapRef = s
    }

    fun resetPlayer() { player = Player() }

    fun backPressed(): Boolean {
        val top = scenes.lastOrNull() ?: return false
        return top.onBack(this)
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        thread = GameThread().apply { running = true; start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        scaleF = Math.min(width / 1280f, height / 720f)
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        thread?.running = false
        thread = null
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val x = ev.x / scaleF
            val y = ev.y / scaleF
            scenes.lastOrNull()?.touch(x, y, this)
        }
        return true
    }

    private inner class GameThread : Thread() {
        var running = false
        override fun run() {
            lastTime = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                lastTime = now
                val canvas: Canvas? = try { holder.lockCanvas() } catch (e: Exception) { null }
                if (canvas != null) {
                    try {
                        synchronized(holder) {
                            canvas.drawColor(Color.BLACK)
                            canvas.save()
                            canvas.scale(scaleF, scaleF)
                            scenes.lastOrNull()?.update(dt, this@GameView)
                            scenes.lastOrNull()?.render(canvas, this@GameView)
                            canvas.restore()
                        }
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
                try { sleep(16) } catch (e: InterruptedException) {}
            }
        }
    }
}
