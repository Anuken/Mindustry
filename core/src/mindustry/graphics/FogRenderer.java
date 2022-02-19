package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Highly experimental fog-of-war renderer. */
public class FogRenderer{
    private FrameBuffer staticFog = new FrameBuffer(), dynamicFog = new FrameBuffer();
    private LongSeq events = new LongSeq();
    private LongSeq dynamics = new LongSeq();
    private boolean dynamicUpdate = false;
    private Rect rect = new Rect();
    private @Nullable Team lastTeam;

    public FogRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            lastTeam = null;
            events.clear();
        });
    }

    public void handleEvent(long event){
        events.add(event);
    }

    public void flushDynamic(LongSeq seq){
        dynamics.clear();
        dynamics.addAll(seq);
        dynamicUpdate = true;
    }

    public Texture getStaticTexture(){
        return staticFog.getTexture();
    }

    public Texture getDynamicTexture(){
        return dynamicFog.getTexture();
    }

    public void drawFog(){
        //there is no fog.
        if(fogControl.getDiscovered(player.team()) == null) return;

        //resize if world size changes
        boolean
            clearStatic = staticFog.resizeCheck(world.width(), world.height()),
            clearDynamic = dynamicFog.resizeCheck(world.width(), world.height());

        if(player.team() != lastTeam){
            copyFromCpu();
            lastTeam = player.team();
            clearStatic = false;
            dynamicUpdate = true;
        }

        if(clearDynamic || dynamicUpdate){
            dynamicUpdate = false;

            Draw.proj(0, 0, staticFog.getWidth(), staticFog.getHeight());
            dynamicFog.begin(Color.black);
            ScissorStack.push(rect.set(1, 1, staticFog.getWidth() - 2, staticFog.getHeight() - 2));

            //TODO render all (clipped) view circles

            ScissorStack.pop();
            Draw.proj(Core.camera);
        }

        //grab static events
        if(clearStatic || events.size > 0){
            //set projection to whole map
            Draw.proj(0, 0, staticFog.getWidth(), staticFog.getHeight());

            //if the buffer resized, it contains garbage now, clearStatic it.
            if(clearStatic){
                staticFog.begin(Color.black);
            }else{
                staticFog.begin();
            }

            ScissorStack.push(rect.set(1, 1, staticFog.getWidth() - 2, staticFog.getHeight() - 2));

            Draw.color(Color.white);

            //process new fog events
            for(int i = 0; i < events.size; i++){
                long e = events.items[i];
                Tile tile = world.tile(FogEvent.x(e), FogEvent.y(e));
                float o = 0f;
                //visual offset for uneven blocks; this is not reflected on the CPU, but it doesn't really matter
                if(tile != null && tile.block().size % 2 == 0 && tile.isCenter()){
                    o = 0.5f;
                }
                Fill.poly(FogEvent.x(e) + 0.5f + o, FogEvent.y(e) + 0.5f + o, 20, FogEvent.radius(e) + 0.3f);
            }

            events.clear();

            staticFog.end();
            ScissorStack.pop();
            Draw.proj(Core.camera);
        }

        staticFog.getTexture().setFilter(TextureFilter.linear);

        Draw.shader(Shaders.fog);
        Draw.fbo(staticFog.getTexture(), world.width(), world.height(), tilesize);
        Draw.shader();
    }

    public void copyFromCpu(){
        staticFog.resize(world.width(), world.height());
        staticFog.begin(Color.black);
        Draw.proj(0, 0, staticFog.getWidth(), staticFog.getHeight());
        Draw.color();
        int ww = world.width(), wh = world.height();

        var data = fogControl.getDiscovered(player.team());
        int len = world.width() * world.height();
        if(data != null){
            for(int i = 0; i < len; i++){
                if(data.get(i)){
                    //TODO slow, could do scanlines instead at the very least.
                    int x = i % ww, y = i / ww;

                    //manually clip with 1 pixel of padding so the borders are never fully revealed
                    if(x > 0 && y > 0 && x < ww - 1 && y < wh - 1){
                        Fill.rect(x + 0.5f, y + 0.5f, 1f, 1f);
                    }
                }
            }
        }

        staticFog.end();
        Draw.proj(Core.camera);
    }

}
