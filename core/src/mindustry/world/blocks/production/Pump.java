package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Pump extends LiquidBlock{
    /** Pump amount per tile. */
    protected float pumpAmount = 0.2f;

    public Pump(String name){
        super(name);
        group = BlockGroup.liquids;
        floating = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.output, 60f * pumpAmount * size * size, StatUnit.liquidSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile tile = world.tile(x, y);
        if(tile == null) return;

        float amount = 0f;
        Liquid liquidDrop = null;

        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            if(canPump(other)){
                liquidDrop = other.floor().liquidDrop;
                amount += other.floor().liquidMultiplier;
            }
        }

        if(liquidDrop != null){
            float width = drawPlaceText(Core.bundle.formatFloat("bar.pumpspeed", amount * pumpAmount * 60f, 0), x, y, valid);
            float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5;
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
    public boolean canPlaceOn(Tile tile, Team team){
        if(isMultiblock()){
            Liquid last = null;
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(other.floor().liquidDrop == null) continue;
                if(other.floor().liquidDrop != last && last != null) return false;
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

    public class PumpBuild extends LiquidBuild{
        public float amount = 0f;
        public Liquid liquidDrop = null;

        @Override
        public void draw(){
            Draw.rect(name, x, y);

            Draw.color(liquids.current().color);
            Draw.alpha(liquids.total() / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            amount = 0f;
            liquidDrop = null;

            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(canPump(other)){
                    liquidDrop = other.floor().liquidDrop;
                    amount += other.floor().liquidMultiplier;
                }
            }
        }

        @Override
        public boolean shouldConsume(){
            return liquidDrop != null && liquids.get(liquidDrop) < liquidCapacity - 0.01f && enabled;
        }

        @Override
        public void updateTile(){
            if(consValid() && liquidDrop != null){
                float maxPump = Math.min(liquidCapacity - liquids.total(), amount * pumpAmount * edelta());
                liquids.add(liquidDrop, maxPump);
            }

            dumpLiquid(liquids.current());
        }
    }

}
