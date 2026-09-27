package mindustry.game.markers;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.blocks.logic.CanvasBlock.*;
import mindustry.world.blocks.logic.LogicDisplay.*;

import static mindustry.Vars.*;

/** Displays a texture with specified name. */
public class TextureMarker extends PosMarker{
    public float rotation = 0f, width = 0f, height = 0f; // Zero width/height scales marker to original texture's size
    public final TextureHolder texture = new TextureHolder();
    public Color color = Color.white.cpy();

    private transient TextureRegion fetchedRegion;

    public TextureMarker(Object texture, float x, float y, float width, float height){
        this.texture.value = texture;
        this.pos.set(x, y);
        this.width = width;
        this.height = height;
    }

    public TextureMarker(Object texture, float x, float y){
        this.texture.value = texture;
        this.pos.set(x, y);
    }

    public TextureMarker(){
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case rotation -> rotation = (float)p1;
                case textureSize -> width = (float)p1 * tilesize;
                case color -> color.fromDouble(p1);
            }
        }

        if(!Double.isNaN(p2)){
            switch(type){
                case textureSize -> height = (float)p2 * tilesize;
            }
        }
    }

    @Override
    public void draw(float scaleFactor){
        if(fetchedRegion == null) setTexture(texture.value);
        prepareTexture(this, texture.value);

        float width = this.width, height = this.height;

        // Zero width/height scales marker to original texture's size
        if(Mathf.equal(width, 0f)) width = fetchedRegion.width * fetchedRegion.scl() * Draw.xscl;
        if(Mathf.equal(height, 0f)) height = fetchedRegion.height * fetchedRegion.scl() * Draw.yscl;

        Draw.z(drawLayer);
        Draw.color(color);
        Draw.rect(fetchedRegion, pos.x, pos.y, width * scaleFactor, height * scaleFactor, rotation);
    }

    @Override
    public void setTexture(Object texture){
        this.texture.value = texture;

        if(headless) return;
        if(fetchedRegion == null) fetchedRegion = new TextureRegion();
        lookupRegion(texture, fetchedRegion);
    }

    public static void lookupRegion(Object texture, TextureRegion out){
        if(texture instanceof String name){
            TextureRegion region = Core.atlas.find(name);
            if(region.found()){
                out.set(region);
            }else if(Core.assets.isLoaded(name, Texture.class)){
                out.set(Core.assets.get(name, Texture.class));
            }else{
                out.set(Core.atlas.find("error"));
            }
        }else if(texture instanceof UnlockableContent u){
            out.set(u.fullIcon);
        }else if(texture instanceof LogicDisplayBuild d && d.isAdded()){
            d.rootDisplay.ensureBuffer();
            d.rootDisplay.getBufferRegion(out);
        }else if(texture instanceof CanvasBuild c && c.isAdded()){
            c.updateTexture();
            if(c.texture != null) out.set(c.texture);
        }else{
            out.set(Core.atlas.find("error"));
        }
    }

    public static void prepareTexture(ObjectiveMarker marker, Object texture){
        if(texture instanceof LogicDisplayBuild d && d.isAdded()){
            if(d.rootDisplay.buffer == null || d.rootDisplay.buffer.isDisposed()){
                marker.setTexture("error");
            }else{
                d.rootDisplay.processCommands();
            }
        }else if(texture instanceof CanvasBuild c && c.isAdded()){
            if(c.texture == null || c.texture.isDisposed()){
                marker.setTexture("error");
            }else{
                c.updateTexture();
            }
        }
    }
}
