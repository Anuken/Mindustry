package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.Array;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.
 */
public class SolidPump extends Pump{
    public Liquid result = Liquids.water;
    public Effect updateEffect = Fx.none;
    /** How near a pump must be to be "nearby" */
    public float deficiencyRadius = 0f;
    /** Raw efficiency is multiplied by this for every nearby pump */
    public float deficiencyScale = 0.97f;
    public float updateEffectChance = 0.02f;
    public float rotateSpeed = 1f;
    /** Attribute that is checked when calculating output. */
    public @Nullable Attribute attribute;

    public SolidPump(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
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
        bars.add("efficiency", entity -> new Bar(() ->
        Core.bundle.formatFloat("bar.pumpspeed",
        ((SolidPumpEntity)entity).lastPump / Time.delta() * 60, 1),
        () -> Pal.ammo,
        () -> ((SolidPumpEntity)entity).warmup));
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
    public boolean canPlaceOn(Tile tile){
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
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator"), Core.atlas.find(name + "-top")};
    }

    public class SolidPumpEntity extends PumpEntity{
        public float warmup;
        public float pumpTime;
        public float boost;
        public float lastPump;
        public float deficiency = 1f;

        @Override
        public float efficiency(){
            float raw = power != null && (block.consumes.has(ConsumeType.power) && !block.consumes.getPower().buffered) ? power.status : 1f;
            return raw * deficiency;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.color(liquids.current().color);
            Draw.alpha(liquids.total() / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();
            Draw.rect(name + "-rotator", x, y, pumpTime * rotateSpeed);
            Draw.rect(name + "-top", x, y);
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

            fraction += boost;
            fraction = Math.max(fraction, 0);

            if(cons.valid() && typeLiquid() < liquidCapacity - 0.001f){
                float maxPump = Math.min(liquidCapacity - typeLiquid(), pumpAmount * delta() * fraction * efficiency());
                liquids.add(result, maxPump);
                lastPump = maxPump;
                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
                if(timer(timerContentCheck, 10)) useContent(result);
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

        @Override
        public void onRemoved(){
            super.onRemoved();
            updateDeficiency(true);
        }

        @Override
        public void placed(){
            super.placed();
            updateDeficiency(false);
        }

        protected int clampCoord(float n, int sign, int mapSize){
            return (int) Mathf.clamp(n + sign * deficiencyRadius, 0, mapSize);
        }

        public void updateDeficiency(boolean increase){
            if(deficiencyScale != 1 && deficiencyRadius > 0.5) {
                deficiency = 1f;
                Tile t;
                Array<Tile> pumps = new Array<>();
                Map map = world.getMap();
                int xMin = clampCoord(x / tilesize, -1, map.width);
                int xMax = clampCoord(x / tilesize, 1, map.width);
                int yMin = clampCoord(y / tilesize, -1, map.height);
                int yMax = clampCoord(y / tilesize, 1, map.height);
                for(int a = xMin; a < xMax; a++){
                    for(int b = yMin; b < yMax; b++){
                        t = world.tile(a, b);
                        if(t != null && t.block() instanceof SolidPump){
                            pumps.add(t);
                        }
                    }
                }

                deficiency = Mathf.pow(deficiencyScale, (float) pumps.size - 1f);
                for(Tile pump : pumps){
                    ((SolidPumpEntity) pump.entity).deficiency = deficiency;
                }
            }
        }
    }
}
