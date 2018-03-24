package io.anuke.mindustry.server.mapgen;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.ucore.util.Mathf;

public class Colorizer {
    Color tmp = new Color();
    float[] hsv1 = new float[3];
    float[] hsv2 = new float[3];
    float target = 240f;
    float shift = 12f;
    float e = 0.05f;

    public void process(FileHandle in, FileHandle out){
        for(FileHandle child : in.list()){
            if(child.isDirectory()){
                process(child, out);
            }else if(child.extension().equals("png")){
                PixmapIO.writePNG(out.child(child.name()), colorize(new Pixmap(child)));
            }
        }
    }

    public Pixmap colorize(Pixmap pixmap){
        Array<Array<Color>> colors = new Array<>();
        IntSet used = new IntSet();

        for(int x = 0; x < pixmap.getWidth(); x ++){
            for(int y = 0; y < pixmap.getHeight(); y ++){
                tmp.set(pixmap.getPixel(x, y));

                if(tmp.a <= 0.1f || used.contains(Color.rgba8888(tmp))) continue;

                used.add(Color.rgba8888(tmp));

                boolean found = false;

                outer:
                for(Array<Color> arr : colors){
                    for(Color color : arr){
                        if(isSameShade(color, tmp)){
                            arr.add(tmp.cpy());
                            found = true;
                            break outer;
                        }
                    }
                }

                if(!found){
                    colors.add(Array.with(tmp.cpy()));
                }
            }
        }

        colors.forEach(a -> a.sort((c1, c2) -> Float.compare(c1.toHsv(hsv1)[2], c2.toHsv(hsv2)[2])));

        IntIntMap map = new IntIntMap();

        for(Array<Color> arr : colors){
            for(int i = 0; i < arr.size; i ++){
                int shift = arr.size - 1 - i;
                map.put(Color.rgba8888(arr.get(i)), Color.rgba8888(shift(arr.get(i), shift)));
            }
        }

        Pixmap result = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());

        for(int x = 0; x < pixmap.getWidth(); x ++) {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                result.drawPixel(x, y, map.get(pixmap.getPixel(x, y), 0));
            }
        }

        return result;
    }

    Color shift(Color color, int amount){
        color.toHsv(hsv1);
        float h = hsv1[0];
        /*if(hsv1[1] < e){
            hsv1[1] += amount * 0.1f;
            h = Mathf.lerp(0f, target, amount * 0.08f);
        }*/
        float s = amount * shift;
        if(Math.abs(h - target) < s){
            h = target;
        }else{
            if(h > target) h -= s;
            if(h < target) h += s;
        }
        hsv1[0] = h;
        tmp.fromHsv(hsv1);
        tmp.a = color.a;
        return tmp;
    }

    boolean isSameShade(Color a, Color b){
        a.toHsv(hsv1);
        b.toHsv(hsv2);

        return Mathf.near(hsv1[0], hsv2[0], e*360f) && Mathf.near(hsv1[1], hsv2[1], e);
    }
}
