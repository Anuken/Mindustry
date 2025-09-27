package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.serialization.*;

import java.io.*;

/** Java port of https://github.com/GglLfr/tile-gen/blob/master/src/main.rs */
public class ImageTileGenerator{
    private static final int layoutWidth = 384, layoutHeight = 128;
    private static final Pixmap layout = new Pixmap(Base64Coder.decode("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"));

    public static void generate(Fi path, String name, Fi outputDir) throws IOException{
        Pixmap image = new Pixmap(path);

        try{
            int width = image.width, height = image.height;

            if(width % 4 != 0 || height % 4 != 0) throw new IOException("Image dimensions are not divisible by 4: " + width + "x" + height);
            if(width != height) throw new IOException("Image is not square: " + width + "x" + height);

            int cellSize = width / 4;

            IntIntMap colorToPosition = new IntIntMap();

            for(int x = 0; x < 4; x++){
                for(int y = 0; y < 4; y++){
                    colorToPosition.put(layout.get(x * layoutWidth / 12, y * layoutHeight / 4), Point2.pack(x * width / 4, y * height / 4));
                }
            }

            int outWidth = width / 4 * 12, outHeight = height;
            Pixmap out = new Pixmap(outWidth, outHeight);

            for(int cx = 0; cx < 12; cx++){
                for(int cy = 0; cy < 4; cy++){
                    for(int rx = 0; rx < cellSize; rx++){
                        for(int ry = 0; ry < cellSize; ry++){
                            int point = colorToPosition.get(layout.get(
                                (cx * cellSize + rx) * layoutWidth / (width * 3),
                                (cy * cellSize + ry) * layoutHeight / height
                            ), -1);

                            if(point != -1){
                                int sx = Point2.x(point), sy = Point2.y(point);
                                out.set(cx * cellSize + rx, cy * cellSize + ry, image.get(sx + rx, sy + ry));
                            }
                        }
                    }
                }
            }

            for(int i = 0; i < 47; i++){
                int cx = i % 12, cy = i / 12;
                Pixmap cropped = out.crop(cx * cellSize, cy * cellSize, cellSize, cellSize);
                outputDir.child(name + "-" + i + ".png").writePng(cropped);
                cropped.dispose();
            }

            out.dispose();
        }finally{
            image.dispose();
        }
    }
}
