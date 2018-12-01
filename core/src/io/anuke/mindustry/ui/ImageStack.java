package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;

public class ImageStack extends Stack{

    public ImageStack(TextureRegion... regions){
        for(TextureRegion region : regions){
            add(new Image(region));
        }
    }
}
