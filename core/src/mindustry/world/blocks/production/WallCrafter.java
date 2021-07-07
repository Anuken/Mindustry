package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class WallCrafter extends Block{
    public @Load("@-top") TextureRegion topRegion;

    /** Time to produce one item at 100% efficiency. */
    public float drillTime = 200f;
    /** Effect randomly played while drilling. */
    public Effect updateEffect = Fx.mineSmall;
    /** Attribute to check for wall output. */
    public Attribute attribute = Attribute.oil; //TODO silicates

    public Item output = Items.sand;

    public WallCrafter(String name){
        super(name);

        hasItems = true;
        rotate = true;
        update = true;
        solid = true;
        drawArrow = false;

        envEnabled |= Env.space;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(topRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        float eff = 0f;

        for(int i = 0; i < size; i++){
            getLaserPos(x, y, rotation, Tmp.p1);
            int rx = Tmp.p1.x, ry = Tmp.p1.y;

            Tile other = world.tile(rx, ry);
            if(other != null && other.solid()){
                eff += other.block().attributes.get(attribute);
            }
        }

        drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / drillTime * eff, 2), x, y, valid);

    }

    void getLaserPos(int tx, int ty, int rotation, Point2 out){
        int cornerX = tx - (size-1)/2, cornerY = ty - (size-1)/2, s = size;
        switch(rotation){
            case 0 -> out.set(cornerX + s, cornerY + 1);
            case 1 -> out.set(cornerX + 1, cornerY + s);
            case 2 -> out.set(cornerX - 1, cornerY + 1);
            case 3 -> out.set(cornerX + 1, cornerY - 1);
        }
    }

    public class WallCrafterBuild extends Building{
        public float time;
        public float warmup;

        @Override
        public void drawSelect(){

            //TODO efficiency
        }

        @Override
        public void updateTile(){
            super.updateTile();

            boolean cons = shouldConsume();

            warmup = Mathf.lerpDelta(warmup, Mathf.num(consValid()), 0.1f);
            float eff = 0f;

            //update facing tiles
            for(int p = 0; p < size; p++){
                getLaserPos(tile.x, tile.y, rotation, Tmp.p1);

                int rx = Tmp.p1.x, ry = Tmp.p1.y;
                Tile dest = world.tile(rx, ry);
                if(dest != null && dest.solid()){
                    eff += dest.block().attributes.get(attribute);

                    //TODO make not chance based?
                    if(cons && dest.block().attributes.get(attribute) > 0f && Mathf.chanceDelta(0.05 * warmup)){
                        updateEffect.at(dest.worldx() + Mathf.range(3f), dest.worldy() + Mathf.range(3f));
                    }
                }


            }

            time += edelta();

            if(time >= drillTime){

                time %= drillTime;
            }

            if(timer(timerDump, dumpTime)){
                dump();
            }
        }

        @Override
        public boolean shouldConsume(){
            return items.total() < itemCapacity;
        }

        @Override
        public void draw(){
            //TODO draw spinner drill thingies
            Draw.rect(block.region, x, y);
            Draw.rect(topRegion, x, y, rotdeg());
        }
    }
}
