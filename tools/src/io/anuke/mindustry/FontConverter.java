package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.backends.sdl.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;

public class FontConverter{
    static final String font = "5x5-latin.png";
    static final int size = 5;
    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static void main(String[] args){
        new SdlApplication(new ApplicationListener(){
            @Override
            public void init(){
                convert();
                System.exit(0);
            }
        }, new SdlConfig(){{
            initialVisible = false;
        }});
    }

    static void convert(){
        StringBuilder result = new StringBuilder();
        Pixmap pixmap = new Pixmap(Core.files.local("tools/resources/" + font));
        for(int i = 0; i < alphabet.length(); i++){
            int ix = i * size;
            int bit = 0;
            for(int y = 0; y < size; y++){
                for(int x = ix; x < ix + size; x++){
                    if(Tmp.c1.set(pixmap.getPixel(x, size - 1 - y)).a > 0.1f){
                        bit |= (1 << ((x - ix) + y*size));
                    }
                }
            }
            result.append((int)alphabet.charAt(i)).append(", ").append(bit).append(", ");
        }
        Log.info(result);
    }
}
