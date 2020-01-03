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

import static mindustry.Vars.*;

public class CraterConveyor extends BaseConveyor{
    private TextureRegion start, end, crater;


    public CraterConveyor(String name){
        super(name);
        compressable = true;
        entityType = CraterConveyorEntity::new;
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

        stats.add(BlockStat.maxUnits, 1, StatUnit.none);
        stats.add(BlockStat.boostEffect, "$blocks.itemcapacity");
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
        CraterConveyorEntity entity = tile.ent();

        if(entity.crater != null) entity.crater.draw(tile);
    }


    @Override
    public void update(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        if(entity.lastFrameUpdated == Core.graphics.getFrameId()) return;
        entity.lastFrameUpdated = Core.graphics.getFrameId();

        if(entity.crater == null){
            if(entity.items.total() > 0 && Core.graphics.getFrameId() > entity.lastFrameSpawned){
                entity.crater = new Crater(tile);
                Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
            }
        }else{
            if(entity.items.total() == 0){
                Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
                entity.crater = null;
            }else{

                if(shouldLaunch(tile)){
                    Tile destination = tile.front();
                    destination.block().update(destination);

                    if(entity.crater.dst(tile) < 1.25f){
                        entity.crater.face = tile.rotation() * 90 - 90;
                        if(!(destination.block() instanceof CraterConveyor)){
                            while(entity.items.total() > 0 && entity.crater.item != null && offloadDir(tile, entity.crater.item)) entity.items.remove(entity.crater.item, 1);

                        }
                    }

                    if(entity.crater.dst(tile) < 0.1f){
                        if(destination.block() instanceof CraterConveyor){
                            CraterConveyorEntity e = destination.ent();

                            if(e.crater == null){
                                e.crater = entity.crater;
                                entity.crater = null;

                                entity.lastFrameSpawned = Core.graphics.getFrameId() + 10;

                                e.items.addAll(entity.items);
                                entity.items.clear();
                            }
                        }
                    }
                }
            }
        }

        if(entity.crater != null){
            entity.crater.x = Mathf.lerpDelta(entity.crater.x, tile.drawx(), speed);
            entity.crater.y = Mathf.lerpDelta(entity.crater.y, tile.drawy(), speed);
            entity.crater.rotation = Mathf.slerpDelta(entity.crater.rotation, entity.crater.face, speed * 2);
        }
    }

    public class CraterConveyorEntity extends BaseConveyorEntity{
        Crater crater;
        float lastFrameUpdated = -1;
        float lastFrameSpawned = -1;
    }

    protected class Crater implements Position{
        float rotation;
        float face;
        float x;
        float y;
        Item item;

        Crater(Tile tile){
            x = tile.drawx();
            y = tile.drawy();
            rotation = tile.rotation() * 90 - 90;
            face = rotation;
        }

        public void draw(Tile tile){
            Draw.rect(crater, x, y, rotation);

            if(item == null){
                item = tile.entity.items.take();
                tile.entity.items.add(item, 1);
            }

            float size = itemSize / 1.5f;
            Draw.rect(item.icon(Cicon.medium), x, y, size, size, 0);

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
    public boolean acceptItem(Item item, Tile tile, Tile source){
        CraterConveyorEntity entity = tile.ent();

        if(!Track.start.check.get(tile) && !source.block().compressable) return false;
        if(entity.items.total() > 0 && !entity.items.has(item)) return false;
        if(entity.items.total() >= getMaximumAccepted(tile, item)) return false;

        return true;
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return Mathf.round(super.getMaximumAccepted(tile, item) * tile.entity.timeScale);
    }

    public boolean shouldLaunch(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        // its not a start tile so it should be moving
        if(!Track.start.check.get(tile)) return true;

        // its considered full
        if(entity.items.total() >= getMaximumAccepted(tile, entity.crater.item)) return true;

        // if it has no way of getting additional items
        Tile[] inputs = new Tile[]{tile.back(), tile.left(), tile.right()};
        boolean headless = true;
        for(Tile input : inputs){
            if(input != null && input.getTeam() == tile.getTeam() && input.block().outputsItems()) headless = false;
        }
        if(headless) return true;

        // inactive timer tracker?
        return false;
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
