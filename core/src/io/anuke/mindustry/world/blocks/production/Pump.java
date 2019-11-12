package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Pump extends LiquidBlock{
    protected final Array<Tile> drawTiles = new Array<>();
    protected final Array<Tile> updateTiles = new Array<>();

    protected final int timerContentCheck = timers++;

    /** Pump amount, total. */
    protected float pumpAmount = 1f;

    public Pump(String name){
        super(name);
        layer = Layer.overlay;
        group = BlockGroup.liquids;
        floating = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find("pump-liquid");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.output, 60f * pumpAmount, StatUnit.liquidSecond);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name, tile.drawx(), tile.drawy());

        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile tile = world.tile(x, y);
        if(tile == null) return;

        float tiles = 0f;
        Liquid liquidDrop = null;

        for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
            if(isValid(other)){
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
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name)};
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
            float maxPump = Math.min(liquidCapacity - tile.entity.liquids.total(), tiles * pumpAmount * tile.entity.delta() / size / size);
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

    protected boolean isValid(Tile tile){
        return tile != null && tile.floor().liquidDrop != null;
    }

}
