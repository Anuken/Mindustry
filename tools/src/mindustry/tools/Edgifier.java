package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

public class Edgifier{

    public static void main(String[] args){
        ArcNativesLoader.load();

        Pixmap pixmap = new Pixmap(Fi.get("/home/anuke/Projects/Mindustry/core/assets-raw/sprites/units/reaper.png"));

        Fi.get("/home/anuke/out.png").writePNG(edgify(pixmap, 5));
    }

    private static Pixmap edgify(Pixmap in, int chunk){
        Pixmap out = new Pixmap(in.getWidth(), in.getHeight());
        IntSeq side1 = new IntSeq(), side2 = new IntSeq();

        for(int x = 0; x < in.getWidth(); x += chunk){
            for(int y = 0; y < in.getHeight(); y += chunk){
                int bestErrors = Integer.MAX_VALUE;
                int bestRotation = 0;
                int bestSide1 = 0, bestSide2 = 0;

                for(int rotation = 0; rotation < 8; rotation++){
                    side1.clear();
                    side2.clear();

                    //assign pixels present on each side
                    for(int cx = 0; cx < chunk; cx++){
                        for(int cy = 0; cy < chunk; cy++){
                            boolean side = classify(rotation, cx, cy, chunk);

                            int pixel = in.getPixel(x + cx, y + cy);
                            if(Pixmaps.empty(pixel)) pixel = 0; //all alpha=0 pixels are treated as 0

                            (side ? side1 : side2).add(pixel);
                        }
                    }

                    //find most popular element here
                    int mode1 = side1.mode(), mode2 = side2.mode();
                    //total errors; 'incorrect' pixels
                    int errors = (side1.size - side1.count(mode1)) + (side2.size - side2.count(mode2));

                    //Log.info("errors for rotation={0}: {1}", rotation, errors);

                    //update if better
                    if(errors < bestErrors){
                        bestRotation = rotation;
                        bestSide1 = mode1;
                        bestSide2 = mode2;
                        bestErrors = errors;
                    }
                }

                //Log.info("Best result for {0},{1}: rotation={2} 1={3} 2={4} errors={5}", x, y, bestRotation, bestSide1, bestSide2, bestErrors);

                //draw with the best result
                for(int cx = 0; cx < chunk; cx++){
                    for(int cy = 0; cy < chunk; cy++){
                        boolean side = classify(bestRotation, cx, cy, chunk);
                        out.draw(x + cx, y + cy, side ? bestSide1 : bestSide2);
                    }
                }
            }
        }

        return out;
    }

    private static boolean classify(int rotation, int x, int y, int chunk){
        switch(rotation){
            case 0: //return y >= chunk / 2;
            case 1: return y < x;
            case 2: //return x <= chunk / 2;
            case 3: return (chunk - 1 - y) < x;
            case 4: //return (y > chunk / 2);
            case 5: return (y <= x);
            case 6: //return (x < chunk / 2);
            case 7: return ((chunk - 1 - y) <= x);
            default: throw new IllegalArgumentException("Invalid rotation: " + rotation);
        }
    }
}
