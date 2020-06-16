package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Pump extends LiquidBlock{
    public final int timerContentCheck = timers++;

    /** Pump amount, total. */
    protected float pumpAmount = 1f;

    public Pump(String name){
        super(name);
        group = BlockGroup.liquids;
        floating = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.output, 60f * pumpAmount, StatUnit.liquidSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile tile = world.tile(x, y);
        if(tile == null) return;

        float tiles = 0f;
        Liquid liquidDrop = null;

        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            if(canPump(other)){
                liquidDrop = other.floor().liquidDrop;
                tiles++;
            }
        }

        if(liquidDrop != null){
            float width = drawPlaceText(Core.bundle.formatFloat("bar.pumpspeed", tiles * pumpAmount / size / size * 60f, 0), x, y, valid);
            float dx = x * tilesize + offset() - width/2f - 4f, dy = y * tilesize + offset() + size * tilesize / 2f + 5;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(liquidDrop.icon(Cicon.small), dx, dy - 1);
            Draw.reset();
            Draw.rect(liquidDrop.icon(Cicon.small), dx, dy);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region};
    }

    @Override
    public boolean canPlaceOn(Tile tile){
        if(isMultiblock()){
            Liquid last = null;
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(other.floor().liquidDrop == null)
                    continue;
                if(other.floor().liquidDrop != last && last != null)
                    return false;
                last = other.floor().liquidDrop;
            }
            return last != null;
        }else{
            return canPump(tile);
        }
    }

    protected boolean canPump(Tile tile){
        return tile != null && tile.floor().liquidDrop != null;
    }

    public class PumpEntity extends LiquidBlockEntity{

        @Override
        public void draw(){
            Draw.rect(name, x, y);

            Draw.color(liquids.current().color);
            Draw.alpha(liquids.total() / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();
        }

        @Override
        public void updateTile(){
            float tiles = 0f;
            Liquid liquidDrop = null;

            if(isMultiblock()){
                for(Tile other : tile.getLinkedTiles(tempTiles)){
                    if(canPump(other)){
                        liquidDrop = other.floor().liquidDrop;
                        tiles++;
                    }
                }
            }else{
                tiles = 1f;
                liquidDrop = tile.floor().liquidDrop;
            }

            if(cons.valid() && liquidDrop != null){
                float maxPump = Math.min(liquidCapacity - liquids.total(), tiles * pumpAmount * delta() / size / size) * efficiency();
                liquids.add(liquidDrop, maxPump);
            }

            if(liquids.currentAmount() > 0f && timer(timerContentCheck, 10)){
                useContent(liquids.current());
            }

            dumpLiquid(liquids.current());
        }
    }

}
