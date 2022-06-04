package net.kanjitomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        KanjiTomo tomo = new KanjiTomo();
        tomo.loadData();
        BufferedImage image = ImageIO.read(new File(args[0]));
        tomo.setTargetImage(image);
        OCRResults results = tomo.runOCR(new Point(Integer.parseInt(args[1]), Integer.parseInt(args[2])));
        System.out.println(results);
    }
}
