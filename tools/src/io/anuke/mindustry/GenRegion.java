package io.anuke.mindustry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class GenRegion extends TextureRegion {
    public String name;
    public boolean invalid;
    public ImageContext context;

    public static void validate(TextureRegion region){
        if(((GenRegion)region).invalid){
            ((GenRegion) region).context.err("Region does not exist: {0}", ((GenRegion)region).name);
        }
    }
}
