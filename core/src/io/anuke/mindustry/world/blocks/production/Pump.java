package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.graphics.Draw;

public class Pump extends LiquidBlock{
    protected final Array<Tile> drawTiles = new Array<>();
    protected final Array<Tile> updateTiles = new Array<>();

    protected final int timerContentCheck = timers++;

    /**Pump amount per tile this block is on.*/
    protected float pumpAmount = 1f;
    /**Maximum liquid tier this pump can use.*/
    protected int tier = 0;

    public Pump(String name){
        super(name);
        layer = Layer.overlay;
        liquidFlowFactor = 3f;
        group = BlockGroup.liquids;
        floating = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Draw.region("pump-liquid");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.liquidOutputSpeed, 60f * pumpAmount, StatUnit.liquidSecond);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name(), tile.drawx(), tile.drawy());

        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name)};
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        if(isMultiblock()){
            Liquid last = null;
            for(Tile other : tile.getLinkedTilesAs(this, drawTiles)){
                if(other.floor().liquidDrop == null)
                    continue;
                if(other.floor().liquidDrop != last && last != null)
                    return false;
                last = other.floor().liquidDrop;
            }
            return last != null;
        }else{
            return isValid(tile);
        }
    }

    @Override
    public void update(Tile tile){
        float tiles = 0f;
        Liquid liquidDrop = null;

        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(updateTiles)){
                if(isValid(other)){
                    liquidDrop = other.floor().liquidDrop;
                    tiles++;
                }
            }
        }else{
            tiles = 1f;
            liquidDrop = tile.floor().liquidDrop;
        }

        if(tile.entity.cons.valid() && liquidDrop != null){
            float maxPump = Math.min(liquidCapacity - tile.entity.liquids.total(), tiles * pumpAmount * tile.entity.delta());
            if(hasPower){
                maxPump *= tile.entity.power.satisfaction; // Produce slower if not at full power
            }
            tile.entity.liquids.add(liquidDrop, maxPump);
        }

        if(tile.entity.liquids.currentAmount() > 0f && tile.entity.timer.get(timerContentCheck, 10)){
            useContent(tile, tile.entity.liquids.current());
        }

        tryDumpLiquid(tile, tile.entity.liquids.current());
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return consumes.has(ConsumeLiquid.class) && consumes.liquid() == liquid && super.acceptLiquid(tile, source, liquid, amount);
    }

    protected boolean isValid(Tile tile){
        return tile != null && tile.floor().liquidDrop != null && tier >= tile.floor().liquidDrop.tier;
    }

}
