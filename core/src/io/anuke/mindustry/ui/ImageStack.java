package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;

public class ImageStack extends Stack{

    public ImageStack(TextureRegion... regions){
        for(TextureRegion region : regions){
            add(new Image(region));
        }
    }
}
