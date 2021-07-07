package mindustry.world.blocks.production;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
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
    public Attribute attribute = Attribute.silicate;

    public Item output = Items.sand;

    public WallCrafter(String name){
        super(name);

        hasItems = true;
        rotate = true;
        update = true;
        solid = true;

        envEnabled |= Env.space;
    }

    @Override
    public void setBars(){
        super.setBars();
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, output);
        stats.add(Stat.tiles, StatValues.blocks(attribute, floating, 1f, true, false));
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
        float eff = getEfficiency(x, y, rotation, null);

        drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / drillTime * eff, 2), x, y, valid);
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return getEfficiency(tile.x, tile.y, rotation, null) > 0;
    }

    float getEfficiency(int tx, int ty, int rotation, @Nullable Cons<Tile> ctile){
        float eff = 0f;
        int cornerX = tx - (size-1)/2, cornerY = ty - (size-1)/2, s = size;

        for(int i = 0; i < size; i++){
            int rx = 0, ry = 0;

            switch(rotation){
                case 0 -> {
                    rx = cornerX + s;
                    ry = cornerY + i;
                }
                case 1 -> {
                    rx = cornerX + i;
                    ry = cornerY + s;
                }
                case 2 -> {
                    rx = cornerX - 1;
                    ry = cornerY + i;
                }
                case 3 -> {
                    rx = cornerX + i;
                    ry = cornerY - 1;
                }
            }

            Tile other = world.tile(rx, ry);
            if(other != null && other.solid()){
                float at = other.block().attributes.get(attribute);
                eff += at;
                if(at > 0 && ctile != null){
                    ctile.get(other);
                }
            }
        }
        return eff;
    }

    public class WallCrafterBuild extends Building{
        public float time;
        public float warmup;

        @Override
        public void updateTile(){
            super.updateTile();

            boolean cons = shouldConsume();

            warmup = Mathf.lerpDelta(warmup, Mathf.num(consValid()), 0.1f);
            float dx = Geometry.d4x(rotation) * 0.5f, dy = Geometry.d4y(rotation) * 0.5f;

            float eff = getEfficiency(tile.x, tile.y, rotation, dest -> {
                //TODO make not chance based?
                if(cons && Mathf.chanceDelta(0.05 * warmup)){
                    updateEffect.at(
                        dest.worldx() + Mathf.range(3f) - dx,
                        dest.worldy() + Mathf.range(3f) - dy,
                        output.color
                    );
                }
            });

            if(cons && (time += edelta() * eff) >= drillTime){
                items.add(output, 1);
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
