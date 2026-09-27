package mindustry.game.markers;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.logic.*;

import static arc.graphics.g2d.SpriteBatch.*;
import static mindustry.Vars.*;

public class QuadMarker extends ObjectiveMarker{
    public final TextureHolder texture = new TextureHolder();
    public @Vertices float[] vertices = new float[vertexSize * 4];
    private boolean mapRegion = true;

    private transient @Nullable TextureRegion fetchedRegion;

    public QuadMarker(){
        for(int i = 0; i < 4; i++){
            vertices[i * vertexSize + 5] = Color.whiteFloatBits;
            vertices[i * vertexSize + 6] = Color.clearFloatBits;
        }
    }

    @Override
    public void draw(float scaleFactor){
        if(fetchedRegion == null) setTexture(texture.value);
        TextureMarker.prepareTexture(this, texture.value);

        Draw.z(drawLayer);
        Draw.vert(fetchedRegion.texture, vertices, 0, vertices.length);
    }

    @Override
    public void control(LMarkerControl type, double p1, double p2, double p3){
        super.control(type, p1, p2, p3);

        if(!Double.isNaN(p1)){
            switch(type){
                case color -> {
                    float col = Tmp.c1.fromDouble(p1).toFloatBits();
                    for(int i = 0; i < 4; i++) vertices[i * vertexSize + 5] = col;
                }
                case pos -> vertices[0] = (float)p1 * tilesize;
                case posi -> setPos((int)p1, p2, p3);
                case uvi -> setUv((int)p1, p2, p3);
            }
        }

        if(!Double.isNaN(p2)){
            switch(type){
                case pos -> vertices[1] = (float)p1 * tilesize;
            }
        }

        if(!Double.isNaN(p1) && !Double.isNaN(p2)){
            switch(type){
                case colori -> setColor((int)p1, p2);
            }
        }
    }

    @Override
    public void setTexture(Object texture){
        this.texture.value = texture;
        if(headless) return;

        boolean firstUpdate = fetchedRegion == null;

        if(firstUpdate) fetchedRegion = new TextureRegion();
        Tmp.tr1.set(fetchedRegion);

        TextureMarker.lookupRegion(texture, fetchedRegion);

        if(firstUpdate){
            if(mapRegion){
                mapRegion = false;
                for(int i = 0; i < 4; i++){
                    setUv(i, vertices[i * vertexSize + 2], vertices[i * vertexSize + 3]);
                }
            }
        }else{
            for(int i = 0; i < 4; i++){
                setUv(i, unmap(vertices[i * vertexSize + 2], Tmp.tr1.u, Tmp.tr1.u2), 1 - unmap(vertices[i * vertexSize + 3], Tmp.tr1.v, Tmp.tr1.v2));
            }
        }
    }

    private static float unmap(float x, float from, float to){
        if(Mathf.equal(from, to)) return x;
        return (x - from) / (to - from);
    }

    private void setPos(int i, double x, double y){
        if(i >= 0 && i < 4){
            if(!Double.isNaN(x)) vertices[i * vertexSize] = (float)x * tilesize;
            if(!Double.isNaN(y)) vertices[i * vertexSize + 1] = (float)y * tilesize;
        }
    }

    private void setColor(int i, double c){
        if(i >= 0 && i < 4){
            vertices[i * vertexSize + 5] = Tmp.c1.fromDouble(c).toFloatBits();
        }
    }

    private void setUv(int i, double u, double v){
        if(headless) return;

        if(i >= 0 && i < 4){
            if(fetchedRegion == null) setTexture(texture);

            if(!Double.isNaN(u)){
                boolean clampU = fetchedRegion.texture.getUWrap() != TextureWrap.mirroredRepeat && fetchedRegion.texture.getUWrap() != TextureWrap.repeat;
                vertices[i * vertexSize + 2] = Mathf.map(clampU ? Mathf.clamp((float)u) : (float)u, fetchedRegion.u, fetchedRegion.u2);
            }
            if(!Double.isNaN(v)){
                boolean clampV = fetchedRegion.texture.getVWrap() != TextureWrap.mirroredRepeat && fetchedRegion.texture.getVWrap() != TextureWrap.repeat;
                vertices[i * vertexSize + 3] = Mathf.map(clampV ? 1 - Mathf.clamp((float)v) : 1 - (float)v, fetchedRegion.v, fetchedRegion.v2);
            }
            vertices[i * vertexSize + 4] = fetchedRegion.getDepth();
        }
    }
}
