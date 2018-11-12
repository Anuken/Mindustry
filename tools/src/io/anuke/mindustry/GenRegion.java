package io.anuke.mindustry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class GenRegion extends TextureRegion {
    public String name;
    public boolean invalid;

    public GenRegion(String name){
        this.name = name;
    }

    public static void validate(TextureRegion region){
        if(((GenRegion)region).invalid){
            ImageContext.err("Region does not exist: {0}", ((GenRegion)region).name);
        }
    }
}
