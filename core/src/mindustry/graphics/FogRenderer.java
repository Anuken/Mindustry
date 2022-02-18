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

import static mindustry.Vars.*;

/** Highly experimental fog-of-war renderer. */
public class FogRenderer{
    private static final float fogSpeed = 1f;
    private FrameBuffer buffer = new FrameBuffer();
    private LongSeq events = new LongSeq();
    private Rect rect = new Rect();
    private @Nullable Team lastTeam;

    public FogRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            lastTeam = null;
            events.clear();
        });

        //TODO draw fog when tile is placed?

    }

    public void handleEvent(long event){
        events.add(event);
    }

    public Texture getTexture(){
        return buffer.getTexture();
    }

    public void drawFog(){
        //there is no fog.
        if(fogControl.getData(player.team()) == null) return;

        //resize if world size changes
        boolean clear = buffer.resizeCheck(world.width(), world.height());

        if(player.team() != lastTeam){
            copyFromCpu();
            lastTeam = player.team();
            clear = false;
        }

        //grab events
        if(clear || events.size > 0){
            //set projection to whole map
            Draw.proj(0, 0, buffer.getWidth(), buffer.getHeight());

            //if the buffer resized, it contains garbage now, clear it.
            if(clear){
                buffer.begin(Color.black);
            }else{
                buffer.begin();
            }

            ScissorStack.push(rect.set(1, 1, buffer.getWidth() - 2, buffer.getHeight() - 2));

            Draw.color(Color.white);

            //process new fog events
            for(int i = 0; i < events.size; i++){
                long e = events.items[i];
                Fill.poly(FogEvent.x(e) + 0.5f, FogEvent.y(e) + 0.5f, 20, FogEvent.radius(e) + 0.3f);
            }

            events.clear();

            buffer.end();
            ScissorStack.pop();
            Draw.proj(Core.camera);
        }

        buffer.getTexture().setFilter(TextureFilter.linear);

        Draw.shader(Shaders.fog);
        Draw.fbo(buffer.getTexture(), world.width(), world.height(), tilesize);
        Draw.shader();
    }

    public void copyFromCpu(){
        buffer.resize(world.width(), world.height());
        buffer.begin(Color.black);
        Draw.proj(0, 0, buffer.getWidth(), buffer.getHeight());
        Draw.color();
        int ww = world.width(), wh = world.height();

        boolean[] data = fogControl.getData(player.team());
        if(data != null){
            for(int i = 0; i < data.length; i++){
                if(data[i]){
                    //TODO slow, could do scanlines instead at the very least.
                    int x = i % ww, y = i / ww;

                    //manually clip with 1 pixel of padding so the borders are never fully revealed
                    if(x > 0 && y > 0 && x < ww - 1 && y < wh - 1){
                        Fill.rect(x + 0.5f, y + 0.5f, 1f, 1f);
                    }
                }
            }
        }

        buffer.end();
        Draw.proj(Core.camera);
    }

}
