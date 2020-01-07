package mindustry.world.blocks.distribution;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.ui.*;
import mindustry.type.*;
import mindustry.world.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class CraterConveyor extends BaseConveyor{
    private TextureRegion start, end, crater;

    public CraterConveyor(String name){
        super(name);
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

        stats.add(BlockStat.boostEffect, "$blocks.itemcapacity");
        stats.add(BlockStat.itemsMoved, speed * 60, StatUnit.perSecond);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        // don't draw if its just one lone tile
        if(isStart(tile) && isEnd(tile)) return;
        if(isStart(tile))  Draw.rect(start, tile.drawx(), tile.drawy(), tile.rotation() * 90);
        if(isEnd(tile))      Draw.rect(end, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    @Override
    public void drawLayer(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        if(entity.link == Pos.invalid) return;
        
        Tile from = world.tile(entity.link);
        Tmp.v1.set(from.getX(), from.getY());
        Tmp.v2.set(tile.drawx(), tile.drawy());
        Tmp.v1.interpolate(Tmp.v2, 1f - entity.reload, Interpolation.linear);

        // rotating smoothly
        float a = from.rotation() * 90;
        float b = tile.rotation() * 90;
        if(from.rotation() == 3 && tile.rotation() == 0) a = -1 * 90;
        if(from.rotation() == 0 && tile.rotation() == 3) a = 4 * 90;
        float rotation = Mathf.lerp(a, b, Interpolation.linear.apply(1f - Mathf.clamp(entity.reload * 2, 0f, 1f)));

        // draw crater
        Draw.rect(crater, Tmp.v1.x, Tmp.v1.y, rotation - 90);

        // failsafe
        if(entity.dominant() == null) return;

        // draw resource
        float size = itemSize / 2f;
        size += entity.items.total() * 0.1f / (itemCapacity / 8f);
        Draw.rect(entity.dominant().icon(Cicon.medium), Tmp.v1.x, Tmp.v1.y, size, size, 0);
    }


    @Override
    public void update(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        // only update once per frame
        if(entity.lastFrameUpdated == Core.graphics.getFrameId()) return;
        entity.lastFrameUpdated = Core.graphics.getFrameId();

        entity.reload = Mathf.clamp(entity.reload - speed, 0f, 1f);

        // ensure a crater exists below this block
        if(entity.link == Pos.invalid){
            // poof in crater
            if(entity.items.total() <= 0 || entity.reload > 0) return;
            Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
            entity.link = tile.pos();
        }else{
            // poof out crater
            if(entity.items.total() == 0){
                entity.link = Pos.invalid;
                return;
            }
        }

        if(shouldLaunch(tile)){
            Tile destination = tile.front();

            // failsafe
            if(destination == null) return;

            // prevent trading
            if(destination.getTeam() != tile.getTeam()) return;

            // update the target first to potentially make room
            destination.block().update(destination);

            // when near the center of the target tile...
            if(entity.reload < 0.25f){
                if(!(destination.block() instanceof CraterConveyor) && (entity.link != tile.pos() || !isStart(tile))){ // ...and if its not a crater conveyor, start unloading (everything)
                    while(entity.items.total() > 0 && entity.dominant() != null && offloadDir(tile, entity.dominant())) entity.items.remove(entity.dominant(), 1);
                    if(entity.items.total() == 0) Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
                }
            }

            // when basically exactly on the center:
            if(entity.reload == 0){
                if(destination.block() instanceof CraterConveyor){
                    CraterConveyorEntity e = destination.ent();
                    
                    // check if next crater conveyor is not occupied
                    if(e.items.total() == 0){
                        // transfer ownership of crater
                        entity.link = Pos.invalid;
                        e.link = tile.pos();

                        // prevent this tile from spawning a new crater to avoid collisions
                        entity.reload = 1;
                        e.reload = 1;

                        // transfer inventory of conveyor
                        e.items.addAll(entity.items);
                        entity.items.clear();
                    }
                }
            }
        }
    }

    class CraterConveyorEntity extends BaseConveyorEntity{
        float lastFrameUpdated = -1;

        int link = Pos.invalid;
        float reload;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);

            stream.writeInt(link);
            stream.writeFloat(reload);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);

            link = stream.readInt();
            reload = stream.readFloat();
        }

        public Item dominant(){ // fixme: do this better
            if(tile.entity.items.total() == 0) return null;
            Item item = tile.entity.items.take();
            tile.entity.items.add(item, 1);
            return item;
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        CraterConveyorEntity entity = tile.ent();

        if(!isStart(tile) && !(source.block() instanceof CraterConveyor)) return false;
        if(entity.items.total() > 0 && !entity.items.has(item)) return false;
        if(entity.items.total() >= getMaximumAccepted(tile, item)) return false;
        if(tile.front() == source) return false;

        return true;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        int i = super.removeStack(tile, item, amount);
        if(tile.entity.items.total() == 0) Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
        return i;
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return Mathf.round(super.getMaximumAccepted(tile, item) * tile.entity.timeScale);
    }

    public boolean shouldLaunch(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        // its not a start tile so it should be moving
        if(!isStart(tile)) return true;

        // its considered full
        if(entity.items.total() >= getMaximumAccepted(tile, entity.dominant())) return true;

        return false;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock instanceof CraterConveyor;
    }

    // has no crater conveyors facing into it
    private boolean isStart(Tile tile){
        Tile[] inputs = new Tile[]{tile.back(), tile.left(), tile.right()};
        for(Tile input : inputs){
            if(input != null && input.getTeam() == tile.getTeam() && input.block() instanceof CraterConveyor && input.front() == tile) return false;
        }

        return true;
    }

    // has no crater conveyor in front of it
    private boolean isEnd(Tile tile){
        if(tile.front() == null) return true;
        if(tile.getTeam() != tile.front().getTeam()) return true;
        if(!(tile.front().block() instanceof CraterConveyor)) return true;

        return false;
    }

}
