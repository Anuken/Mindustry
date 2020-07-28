package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.
 */
public class SolidPump extends Pump{
    public Liquid result = Liquids.water;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.02f;
    public float rotateSpeed = 1f;
    /** Attribute that is checked when calculating output. */
    public @Nullable Attribute attribute;

    public @Load("@-rotator") TextureRegion rotatorRegion;

    public SolidPump(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(attribute != null){
            drawPlaceText(Core.bundle.formatFloat("bar.efficiency", Math.max(sumAttribute(attribute, x, y) + 1f, 0f) * 100 * percentSolid(x, y), 1), x, y, valid);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("efficiency", (SolidPumpEntity entity) -> new Bar(() -> Core.bundle.formatFloat("bar.pumpspeed",
        entity.lastPump / Time.delta * 60, 1),
        () -> Pal.ammo,
        () -> entity.warmup));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.output);
        stats.add(BlockStat.output, result, 60f * pumpAmount, true);
        if(attribute != null){
            stats.add(BlockStat.affinities, attribute);
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team){
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(canPump(other)){
                    return true;
                }
            }
            return false;
        }else{
            return canPump(tile);
        }
    }

    @Override
    protected boolean canPump(Tile tile){
        return tile != null && !tile.floor().isLiquid;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, rotatorRegion, topRegion};
    }

    public class SolidPumpEntity extends PumpEntity{
        public float warmup;
        public float pumpTime;
        public float boost;
        public float lastPump;

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.color(liquids.current().color);
            Draw.alpha(liquids.total() / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();
            Draw.rect(rotatorRegion, x, y, pumpTime * rotateSpeed);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public boolean shouldConsume(){
            return liquids.get(result) < liquidCapacity - 0.01f;
        }

        @Override
        public void updateTile(){
            float fraction = 0f;

            if(isMultiblock()){
                for(Tile other : tile.getLinkedTiles(tempTiles)){
                    if(canPump(other)){
                        fraction += 1f / (size * size);
                    }
                }
            }else{
                if(canPump(tile)) fraction = 1f;
            }

            fraction += boost + (attribute == null ? 0 : attribute.env());
            fraction = Math.max(fraction, 0);

            if(cons.valid() && typeLiquid() < liquidCapacity - 0.001f){
                float maxPump = Math.min(liquidCapacity - typeLiquid(), pumpAmount * delta() * fraction * efficiency());
                liquids.add(result, maxPump);
                lastPump = maxPump;
                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
                if(Mathf.chance(delta() * updateEffectChance))
                    updateEffect.at(getX() + Mathf.range(size * 2f), getY() + Mathf.range(size * 2f));
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.02f);
                lastPump = 0f;
            }

            pumpTime += warmup * delta();

            dumpLiquid(result);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityAdded();

            if(attribute != null){
                boost = sumAttribute(attribute, tile.x, tile.y);
            }
        }

        public float typeLiquid(){
            return liquids.total();
        }
    }
}
