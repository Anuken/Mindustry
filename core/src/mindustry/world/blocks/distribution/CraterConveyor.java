package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.util.*;
import mindustry.ui.*;
import arc.math.geom.*;
import mindustry.type.*;
import mindustry.world.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.world.meta.*;
import mindustry.entities.type.*;

import static mindustry.Vars.itemSize;

public class CraterConveyor extends ArmoredItemConveyor{
    private TextureRegion start, end, crater;


    public CraterConveyor(String name){
        super(name);
        compressable = true;
        entityType = PlastaniumConveyorEntity::new;
    }

    @Override
    public void load(){
        int i;
        for(i = 0; i < regions.length; i++){
            for(int j = 0; j < 4; j++){
                regions[i][j] = Core.atlas.find(name + "-" + i + "-" + 0);
            }
        }

        start  = Core.atlas.find(name + "-5-0");
        end    = Core.atlas.find(name + "-6-0");
        crater = Core.atlas.find("crater");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);
        stats.remove(BlockStat.itemsMoved);

        stats.add(BlockStat.maxUnits, 1, StatUnit.none);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        // draws the markings over either end of the track
        if(Track.start.check.get(tile) && Track.end.check.get(tile)) return;
        if(Track.start.check.get(tile))  Draw.rect(start, tile.drawx(), tile.drawy(), tile.rotation() * 90);
        if(Track.end.check.get(tile))      Draw.rect(end, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    @Override
    public void drawLayer(Tile tile){
        PlastaniumConveyorEntity entity = tile.ent();

        if(entity.crater != null) entity.crater.draw(tile);
    }


    @Override
    public void update(Tile tile){ // tick away the cooldown
        PlastaniumConveyorEntity entity = tile.ent();

        if(entity.crater == null){
            if(entity.items.total() > 0){
                entity.crater = new Crater(tile);
                Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
            }
        }else{
            if(entity.items.total() == 0){
                Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
                entity.crater = null;
            }else{
                Tile destination = tile.front();

                if(entity.crater.dst(tile) < 0.5f){
                    entity.crater.f = tile.rotation() * 90 - 90;
                    if(!(destination.block() instanceof CraterConveyor)){
                        while(entity.items.total() > 0 && entity.crater.i != null && offloadDir(tile, entity.crater.i)) entity.items.remove(entity.crater.i, 1);

                    }
                }

                if(entity.crater.dst(tile) < 0.1f){
                    if(destination.block() instanceof CraterConveyor){
                        PlastaniumConveyorEntity e = destination.ent();

                        if(e.crater == null){
                            e.crater = entity.crater;
                            entity.crater = null;

                            e.items.addAll(entity.items);
                            entity.items.clear();
                        }
                    }
                }
            }
        }

        if(entity.crater != null){
            entity.crater.x = Mathf.lerpDelta(entity.crater.x, tile.drawx(), 0.05f);
            entity.crater.y = Mathf.lerpDelta(entity.crater.y, tile.drawy(), 0.05f);
            entity.crater.r = Mathf.slerpDelta(entity.crater.r, entity.crater.f, 0.1f);
        }
    }

    public class PlastaniumConveyorEntity extends ItemConveyorEntity{
        Crater crater;
    }

    protected class Crater implements Position{
        float r;
        float f;
        float x;
        float y;
        Item i;

        Crater(Tile tile){
            x = tile.drawx();
            y = tile.drawy();
            r = tile.rotation() * 90 - 90;
            f = r;
        }

        public void draw(Tile tile){
            Draw.rect(crater, x, y, r);

            if(i == null){
                i = tile.entity.items.take();
                tile.entity.items.add(i, 1);
            }

            float size = itemSize / 1.5f;
            Draw.rect(i.icon(Cicon.medium), x, y, size, size, 0);

            Fonts.outline.draw(tile.entity.items.total() + "", x, y - 1,
            Pal.accent, 0.25f * 0.5f / Scl.scl(1f), false, Align.center);
        }

        @Override
        public float getX(){
            return x;
        }

        @Override
        public float getY(){
            return y;
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){ // summon craters into existence to be loaded
        PlastaniumConveyorEntity entity = tile.ent();

        if(!Track.start.check.get(tile) && !source.block().compressable) return false;
        if(entity.items.total() > 0 && !entity.items.has(item)) return false;
        if(entity.items.total() >= getMaximumAccepted(tile, item)) return false;

        return true;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        tile.entity.items.add(item, 1);
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && (source == null || source.getTeam() == tile.getTeam())){
            return Math.min(getMaximumAccepted(tile, item) - tile.entity.items.get(item), amount);
        }else{
            return 0;
        }
    }

    @Override
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        tile.entity.noSleep();
        tile.entity.items.add(item, amount);
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        if(tile.entity == null || tile.entity.items == null) return 0;
        amount = Math.min(amount, tile.entity.items.get(item));
        tile.entity.noSleep();
        tile.entity.items.remove(item, amount);
        return amount;
    }

    @Override
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){ // only connect to compressable blocks
        return super.blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock.compressable;
    }

    public enum Track{
        // tile is considered the end of the line
        end(tile -> {
            if(tile.front() == null) return true;
            if(tile.getTeam() != tile.front().getTeam()) return true; // comment out to trade
            return !tile.front().block().compressable;
        }),

        // tile is considered the start of the line
        start(tile -> {
            Tile[] inputs = new Tile[]{tile.back(), tile.left(), tile.right()};
            for(Tile input : inputs){
                if(input != null && input.getTeam() == tile.getTeam() && input.block().compressable && input.front() == tile) return false;
            }

            return true;
        });

        public final Boolf<Tile> check;

        Track(Boolf<Tile> check){
            this.check = check;
        }
    }
}
