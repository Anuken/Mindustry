package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class CraterConveyor extends Block implements Autotiler{
    private TextureRegion[] regions = new TextureRegion[8];

    public float speed = 0f;

    public CraterConveyor(String name){
        super(name);

        rotate = true;
        update = true;
        layer = Layer.overlay;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 4;
        conveyorPlacement = true;
        entityType = CraterConveyorEntity::new;

        idleSound = Sounds.conveyor;
        idleSoundVolume = 0.004f;
        unloadable = false;
    }

    @Override
    public void load(){
        for(int i = 0; i < regions.length; i++){
            regions[i] = Core.atlas.find(name + "-" + i + "-" + 0);
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-0-0")};
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.itemsMoved, speed * 60, StatUnit.perSecond);
        stats.add(BlockStat.boostEffect, "$blocks.itemcapacity");
    }

    @Override
    public void draw(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        draw(tile, entity.blendbit1);
        if(entity.blendbit2 == 0) return;
        draw(tile, entity.blendbit2);
    }

    public void draw(Tile tile, int bit){
        CraterConveyorEntity entity = tile.ent();

        Draw.rect(regions[Mathf.clamp(bit, 0, regions.length - 1)], tile.drawx(), tile.drawy(), tilesize * entity.blendsclx, tilesize * entity.blendscly, tile.rotation() * 90);
    }

    @Override
    public void drawLayer(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        //     no from == no crater
        if(entity.from == Pos.invalid) return;

        Tile from = world.tile(entity.from);
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
        Draw.rect(regions[7], Tmp.v1.x, Tmp.v1.y, rotation);

        // failsafe
        if(entity.items.first() == null) return;

        // draw resource
        float size = itemSize / 2f;
        size += entity.items.total() * 0.1f / (itemCapacity / 8f);
        Draw.rect(entity.items.first().icon(Cicon.medium), Tmp.v1.x, Tmp.v1.y, size, size, 0);
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[bits[0]];
        Draw.rect(region, req.drawx(), req.drawy(), region.getWidth() * bits[1] * Draw.scl * req.animScale, region.getHeight() * bits[2] * Draw.scl * req.animScale, req.rotation * 90);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock instanceof CraterConveyor; // blend with nothing but crater conveyors
    }

    @Override
    public void onProximityUpdate(Tile tile){
        super.onProximityUpdate(tile);

        CraterConveyorEntity entity = tile.ent();
        int[] bits = buildBlending(tile, tile.rotation(), null, true);

        entity.blendbit2 = 0;
        if(bits[0] == 0 && blends(tile, tile.rotation(), 0) && !blends(tile, tile.rotation(), 2)) entity.blendbit2 = 5; // a 0 that faces into a crater conveyor with none behind it
        if(bits[0] == 0 && !blends(tile, tile.rotation(), 0) && blends(tile, tile.rotation(), 2)) entity.blendbit2 = 6; // a 0 that faces into none with a crater conveyor behind it

        entity.blendbit1 = bits[0];
        entity.blendsclx = bits[1];
        entity.blendscly = bits[2];
    }

    @Override
    public void update(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        // only update once per frame
        if(entity.lastFrameUpdated == Core.graphics.getFrameId()) return;
        entity.lastFrameUpdated = Core.graphics.getFrameId();

        entity.reload = Mathf.clamp(entity.reload - speed, 0f, 1f);

        // ensure a crater exists below this block
        if(entity.from == Pos.invalid){
            // poof in crater
            if(entity.items.total() <= 0 || entity.reload > 0){
                entity.sleep();
                return;
            }
            Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
            entity.from = tile.pos();
        }else{
            // poof out crater
            if(entity.items.total() == 0){
                entity.from = Pos.invalid;
                entity.sleep();
                return;
            }
        }

        if(shouldLaunch(tile)){

            // when near the center of the target tile...
            if(entity.reload < 0.25f){
                if(entity.blendbit2 != 5 && (entity.from != tile.pos() || entity.blendbit2 == 6)){ // ...and if its not a crater conveyor, start unloading (everything)
                    while(true) if(!tryDump(tile)) break;
                    if(entity.items.total() == 0){
                        Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
                        bump(tile);
                    }
                }
            }

            Tile destination = tile.front();

            // failsafe
            if(destination == null) return;

            // prevent trading
            if(destination.getTeam() != tile.getTeam()) return;

            // update the target first to potentially make room
            destination.block().update(destination);

            // when basically exactly on the center:
            if(entity.reload == 0){
                if(destination.block() instanceof CraterConveyor){
                    CraterConveyorEntity e = destination.ent();

                    // check if next crater conveyor is not occupied
                    if(e.items.total() == 0){
                        // transfer ownership of crater
                        entity.from = Pos.invalid;
                        e.from = tile.pos();

                        // prevent this tile from spawning a new crater to avoid collisions
                        entity.reload = 1;
                        e.reload = 1;

                        // transfer inventory of conveyor
                        e.items.addAll(entity.items);
                        entity.items.clear();

                        e.noSleep();
                        bump(tile);
                    }else{
                        entity.sleep();
                    }
                }
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);

        tile.entity.noSleep();
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        int i = super.removeStack(tile, item, amount);
        if(tile.entity.items.total() == 0) Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
        if(tile.entity.items.total() == 0) bump(tile);
        return i;
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        return Mathf.round(super.getMaximumAccepted(tile, item) * tile.entity.timeScale); // increased item capacity while boosted
    }

    @Override
    public boolean shouldIdleSound(Tile tile){
        return false; // has no moving parts
    }

    class CraterConveyorEntity extends TileEntity{
        float lastFrameUpdated = -1;

        int blendbit1, blendbit2;
        int blendsclx, blendscly;

        int from = Pos.invalid;
        float reload;

        byte dump;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);

            stream.writeInt(from);
            stream.writeByte(dump);
            stream.writeFloat(reload);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);

            from = stream.readInt();
            dump = stream.readByte();
            reload = stream.readFloat();
        }
    }

    // crater conveyor tiles that input into this one
    private void upstream(Tile tile, Cons<Tile> cons){
        CraterConveyorEntity entity = tile.ent();

        if(    entity.blendbit1 == 0 // 1 input from the back, 0 from the sides
            || entity.blendbit1 == 2 // 1 input from the back, 1 from the sides
            || entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
        ) cons.get(tile.back());

        if(    entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
            || entity.blendbit1 == 4 // 0 input from the back, 2 from the sides
            ||(entity.blendbit1 == 1 && entity.blendscly == -1) // side is open
            ||(entity.blendbit1 == 2 && entity.blendscly == +1) // side is open
        ) cons.get(tile.right());

        if(    entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
            || entity.blendbit1 == 4 // 0 input from the back, 2 from the sides
            ||(entity.blendbit1 == 1 && entity.blendscly == +1) // side is open
            ||(entity.blendbit1 == 2 && entity.blendscly == -1) // side is open
        ) cons.get(tile.left());
    }

    // ▲ | ▼ fixme: refactor

    // awaken inputting conveyors
    private void bump(Tile tile){
        upstream(tile, t -> {
            if(t != null && t.entity != null && t.entity.isSleeping() && t.entity.items.total() > 0){
                t.entity.noSleep();
                bump(t);
            }
        });
    }

    private boolean shouldLaunch(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        // prevent(s) launch only when the crater is on a loading dock that still has room for items
        return entity.blendbit2 != 5 || (entity.items.total() >= getMaximumAccepted(tile, entity.items.first()));
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        CraterConveyorEntity entity = tile.ent();

        if (tile == source) return true;                                  // player threw items
        return!((entity.blendbit2 != 5)                                   // not a loading dock
            ||  (entity.items.total() > 0 && !entity.items.has(item))     // incompatible items
            ||  (entity.items.total() >= getMaximumAccepted(tile, item))  // filled to capacity
            ||  (tile.front() == source));                                // fed from the front
    }

    @Override
    protected int retrieveDump(Tile tile){
        return tile.<CraterConveyorEntity>ent().dump;
    }

    @Override
    protected void incrementDump(Tile tile, int prox){
        CraterConveyorEntity entity = tile.ent();

        entity.dump = (byte)((entity.dump + 1) % prox);
    }
}
