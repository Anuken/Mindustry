package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.util.*;

import static mindustry.Vars.*;

public class CacheLayer{
    public static CacheLayer

    water, mud, cryofluid, tar, slag, arkycite,
    space, normal, walls;

    public static CacheLayer[] all = {};

    public int id;

    /** Register a new CacheLayer. */
    public static void add(CacheLayer... layers){
        int newSize = all.length + layers.length;
        var prev = all;
        //reallocate the array and copy everything over; performance matters very little here anyway
        all = new CacheLayer[newSize];
        System.arraycopy(prev, 0, all, 0, prev.length);
        System.arraycopy(layers, 0, all, prev.length, layers.length);

        for(int i = 0; i < all.length; i++){
            all[i].id = i;
        }
    }

    /** Adds a cache layer at a certain position. All layers >= this index are shifted upwards.*/
    public static void add(int index, CacheLayer layer){
        index = Mathf.clamp(index, 0, all.length - 1);

        var prev = all;
        all = new CacheLayer[all.length + 1];

        System.arraycopy(prev, 0, all, 0, index);
        System.arraycopy(prev, index, all, index + 1, prev.length - index);

        all[index] = layer;
    }

    /** Loads default cache layers. */
    public static void init(){
        add(
            water = new ShaderLayer(Shaders.water),
            mud = new ShaderLayer(Shaders.mud),
            tar = new ShaderLayer(Shaders.tar),
            slag = new ShaderLayer(Shaders.slag),
            arkycite = new ShaderLayer(Shaders.arkycite),
            cryofluid = new ShaderLayer(Shaders.cryofluid),
            space = new ShaderLayer(Shaders.space),
            normal = new CacheLayer(),
            walls = new CacheLayer()
        );
    }

    /** Called before the cache layer begins rendering. Begin FBOs here. */
    public void begin(){

    }

    /** Called after the cache layer ends rendering. Blit FBOs here. */
    public void end(){

    }

    public static class ShaderLayer extends CacheLayer{
        public @Nullable Shader shader;

        public ShaderLayer(Shader shader){
            //shader will be null on headless backend, but that's ok
            this.shader = shader;
        }

        @Override
        public void begin(){
            if(!Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.effectBuffer.begin();
            Core.graphics.clear(Color.clear);
            renderer.blocks.floor.beginc();
        }

        @Override
        public void end(){
            if(!Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.effectBuffer.end();

            renderer.effectBuffer.blit(shader);

            renderer.blocks.floor.beginc();
        }
    }
}
