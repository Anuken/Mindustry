package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/** Incinerator that accepts only items and optionally requires a liquid, e.g. slag. */
public class ItemIncinerator extends Block{
    public Effect effect = Fx.incinerateSlag;
    public float effectChance = 0.2f;

    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-top") TextureRegion topRegion;

    public ItemIncinerator(String name){
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    public class ItemIncineratorBuild extends Building{

        @Override
        public void updateTile(){
        }

        @Override
        public BlockStatus status(){
            return efficiency > 0 ? BlockStatus.active : BlockStatus.noInput;
        }

        @Override
        public void draw(){
            super.draw();

            if(liquidRegion.found()){
                Drawf.liquid(liquidRegion, x, y, liquids.currentAmount() / liquidCapacity, liquids.current().color);
            }
            if(topRegion.found()){
                Draw.rect(topRegion, x, y);
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            if(Mathf.chance(effectChance)){
                effect.at(x, y);
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return efficiency > 0;
        }
    }
}
