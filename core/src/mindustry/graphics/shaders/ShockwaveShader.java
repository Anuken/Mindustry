package mindustry.graphics.shaders;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;

import static mindustry.Vars.*;

public class ShockwaveShader extends LoadShader{
    static final int max = 64;
    static final int size = 5;

    //x y radius life[1-0] lifetime
    protected FloatSeq data = new FloatSeq();
    protected FloatSeq uniforms = new FloatSeq();
    protected boolean hadAny = false;
    protected FrameBuffer buffer = new FrameBuffer();

    public float lifetime = 20f;

    public ShockwaveShader(){
        super("shockwave", "screenspace");

        Events.run(Trigger.update, () -> {
            if(state.isPaused()) return;
            if(state.isMenu()){
                data.size = 0;
                return;
            }

            var items = data.items;
            for(int i = 0; i < data.size; i += size){
                //decrease lifetime
                items[i + 3] -= Time.delta / items[i + 4];

                if(items[i + 3] <= 0f){
                    //swap with head.
                    if(data.size > size){
                        System.arraycopy(items, data.size - size, items, i, size);
                    }

                    data.size -= size;
                    i -= size;
                }
            }
        });

        Events.run(Trigger.preDraw, () -> {
            hadAny = data.size > 0;

            if(hadAny){
                buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                buffer.begin(Color.clear);
            }
        });

        Events.run(Trigger.postDraw, () -> {
            if(hadAny){
                buffer.end();
                Draw.blend(Blending.disabled);
                buffer.blit(this);
                Draw.blend();
            }
        });
    }

    @Override
    public void apply(){
        int count = data.size / size;

        setUniformi("u_shockwave_count", count);
        if(count > 0){
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2f, Core.camera.position.y - Core.camera.height / 2f);

            uniforms.clear();

            var items = data.items;
            for(int i = 0; i < count; i++){
                int offset = i * size;

                uniforms.add(
                items[offset], items[offset + 1], //xy
                items[offset + 2] * (1f - items[offset + 3]), //radius * time
                items[offset + 3] //time
                //lifetime ignored
                );
            }

            setUniform4fv("u_shockwaves", uniforms.items, 0, uniforms.size);
        }
    }

    public void add(float x, float y, float radius){
        add(x, y, radius, 20f);
    }

    public void add(float x, float y, float radius, float lifetime){
        //replace first entry
        if(data.size / size >= max){
            var items = data.items;
            items[0] = x;
            items[1] = y;
            items[2] = radius;
            items[3] = 1f;
            items[4] = lifetime;
        }else{
            data.addAll(x, y, radius, 1f, lifetime);
        }
    }
}
