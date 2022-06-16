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
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class WallCrafter extends Block{
    static int idx = 0;

    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-rotator-bottom") TextureRegion rotatorBottomRegion;
    public @Load("@-rotator") TextureRegion rotatorRegion;

    /** Time to produce one item at 100% efficiency. */
    public float drillTime = 150f;
    /** Effect randomly played while drilling. */
    public Effect updateEffect = Fx.mineWallSmall;
    public float updateEffectChance = 0.02f;
    public float rotateSpeed = 2f;
    /** Attribute to check for wall output. */
    public Attribute attribute = Attribute.sand;

    public Item output = Items.sand;

    public WallCrafter(String name){
        super(name);

        hasItems = true;
        rotate = true;
        update = true;
        solid = true;
        regionRotated1 = 1;

        envEnabled |= Env.space;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("drillspeed", (WallCrafterBuild e) ->
            new Bar(() -> Core.bundle.format("bar.drillspeed", Strings.fixed(e.lastEfficiency * 60 / drillTime, 2)), () -> Pal.ammo, () -> e.warmup));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, output);
        stats.add(Stat.tiles, StatValues.blocks(attribute, floating, 1f, true, false));
        stats.add(Stat.drillSpeed, 60f / drillTime * size, StatUnit.itemsSecond);
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
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        float eff = getEfficiency(x, y, rotation, null, null);

        drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / drillTime * eff, 2), x, y, valid);
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return getEfficiency(tile.x, tile.y, rotation, null, null) > 0;
    }

    float getEfficiency(int tx, int ty, int rotation, @Nullable Cons<Tile> ctile, @Nullable Intc2 cpos){
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

            if(cpos != null){
                cpos.get(rx, ry);
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
        public float time, warmup, totalTime, lastEfficiency;

        @Override
        public void updateTile(){
            super.updateTile();

            boolean cons = shouldConsume();

            warmup = Mathf.approachDelta(warmup, Mathf.num(efficiency > 0), 1f / 40f);
            float dx = Geometry.d4x(rotation) * 0.5f, dy = Geometry.d4y(rotation) * 0.5f;

            float eff = getEfficiency(tile.x, tile.y, rotation, dest -> {
                //TODO make not chance based?
                if(wasVisible && cons && Mathf.chanceDelta(updateEffectChance * warmup)){
                    updateEffect.at(
                        dest.worldx() + Mathf.range(3f) - dx * tilesize,
                        dest.worldy() + Mathf.range(3f) - dy * tilesize,
                        dest.block().mapColor
                    );
                }
            }, null);

            lastEfficiency = eff * timeScale * efficiency;

            if(cons && (time += edelta() * eff) >= drillTime){
                items.add(output, 1);
                time %= drillTime;
            }

            totalTime += edelta() * warmup;

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
            float ds = 0.6f, dx = Geometry.d4x(rotation) * ds, dy = Geometry.d4y(rotation) * ds;

            int bs = (rotation == 0 || rotation == 3) ? 1 : -1;
            idx = 0;
            getEfficiency(tile.x, tile.y, rotation, null, (cx, cy) -> {
                int sign = idx++ >= size/2 && size % 2 == 0 ? -1 : 1;
                float vx = (cx - dx) * tilesize, vy = (cy - dy) * tilesize;
                Draw.z(Layer.blockOver);
                Draw.rect(rotatorBottomRegion, vx, vy, totalTime * rotateSpeed * sign * bs);
                Draw.rect(rotatorRegion, vx, vy);
            });
        }
    }
}
