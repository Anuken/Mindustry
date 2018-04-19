package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.sun.media.jfxmediaimpl.MediaDisposer.Disposable;
import io.anuke.mindustry.world.Tile;

public class MinimapRenderer implements Disposable{
    private Pixmap pixmap;
    private Texture texture;

    public Texture getTexture(){
        return texture;
    }

    public void reset(){

    }

    public void updated(Tile tile){

    }

    @Override
    public void dispose() {
        pixmap.dispose();
        texture.dispose();
    }
}
