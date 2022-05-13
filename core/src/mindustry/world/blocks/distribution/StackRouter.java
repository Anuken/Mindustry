package mindustry.world.blocks.distribution;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

public class StackRouter extends DuctRouter{
    public float baseEfficiency = 0f;

    public @Load(value = "@-glow", fallback = "arrow-glow") TextureRegion glowRegion;
    public float glowAlpha = 1f;
    public Color glowColor = Pal.redLight;

    public StackRouter(String name){
        super(name);
        itemCapacity = 10;
    }

    public class StackRouterBuild extends DuctRouterBuild{
        public boolean unloading = false;

        @Override
        public void updateTile(){
            float eff = enabled ? (efficiency + baseEfficiency) : 0f;
            float cap = speed;

            if(!unloading && current != null && items.total() >= itemCapacity){
                if(progress < cap){
                    //when items are full, begin offload timer
                    progress += eff;
                }

                if(progress >= cap){
                    unloading = true;
                    progress %= cap;
                }
            }

            //unload as many as possible when in unloading state
            if(unloading && current != null){
                //unload when possible
                var target = target();
                while(target != null && items.total() > 0){
                    target.handleItem(this, current);
                    int mod = sortItem != null && current != sortItem ? 2 : 3;
                    cdump = (byte)((cdump + 1) % mod);
                    items.remove(current, 1);

                    target = target();
                }

                //if out of items, unloading is over
                if(items.total() == 0){
                    current = null;
                    unloading = false;
                }
            }

            if(current == null && items.total() > 0){
                current = items.first();
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(glowRegion.found() && power != null && power.status > 0){
                Draw.z(Layer.blockAdditive);
                Draw.color(glowColor, glowAlpha * power.status);
                Draw.blend(Blending.additive);
                Draw.rect(glowRegion, x, y, rotation * 90);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return !unloading && (current == null || item == current) && items.total() < itemCapacity &&
                (Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation);
        }
    }
}
