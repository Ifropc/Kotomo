package net.kanjitomo

import java.awt.Point
import java.io.File
import javax.imageio.ImageIO

object Main {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val tomo = KanjiTomo()
        tomo.loadData()
        val image = ImageIO.read(File(args[0]))
        tomo.setTargetImage(image)
        val results = tomo.runOCR(Point(args[1].toInt(), args[2].toInt()))
        println(results)
    }
}
