package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
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
        itemCapacity = 8;
        conveyorPlacement = true;
        entityType = CraterConveyorEntity::new;

        idleSound = Sounds.conveyor;
        idleSoundVolume = 0.004f;
        unloadable = false;
        dumpIncrement = 4;
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

    private void draw(Tile tile, int bit){
        CraterConveyorEntity entity = tile.ent();

        Draw.rect(regions[Mathf.clamp(bit, 0, regions.length - 1)], tile.drawx(), tile.drawy(), tilesize * entity.blendsclx, tilesize * entity.blendscly, tile.rotation() * 90);
    }

    @Override
    public void drawLayer(Tile tile){
        CraterConveyorEntity entity = tile.ent();

        if(entity.from == Pos.invalid) return;

        // position
        Tile from = world.tile(entity.from);
        Tmp.v1.set(from);
        Tmp.v2.set(tile);
        Tmp.v1.interpolate(Tmp.v2, 1f - entity.cooldown, Interpolation.linear);

        // fixme, cleanup
        float a = (from.rotation()%4) * 90;
        float b = (tile.rotation()%4) * 90;
        if((from.rotation()%4) == 3 && (tile.rotation()%4) == 0) a = -1 * 90;
        if((from.rotation()%4) == 0 && (tile.rotation()%4) == 3) a = 4 * 90;

        // crater
        Draw.rect(regions[7], Tmp.v1.x, Tmp.v1.y, Mathf.lerp(a, b, Interpolation.smooth.apply(1f - Mathf.clamp(entity.cooldown * 2, 0f, 1f))));

        // item
        float size = (itemSize / 2f) + entity.items.total() * 0.1f / (itemCapacity / 8f);
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

        // reel in crater
        if(entity.cooldown > 0f) entity.cooldown = Mathf.clamp(entity.cooldown - speed, 0f, 1f);

        // sleep when idle
        if(entity.from == Pos.invalid){
            if(entity.cooldown == 0f) tile.entity.sleep();
            return;
        }

        // crater needs to be centered
        if(entity.cooldown > 0f) return;

        if(entity.blendbit2 == 6){
            while(tryDump(tile)){
                if(entity.items.total() == 0) poofOut(tile);
            }
        }

        /* unload */ else /* transfer */

        if(entity.blendbit2 != 5 || (entity.items.total() >= getMaximumAccepted(tile, entity.items.first()))){
            if(tile.front() != null
            && tile.front().getTeam() == tile.getTeam()
            && tile.front().block() instanceof CraterConveyor){
                CraterConveyorEntity e = tile.front().ent();

                // sleep if its occupied
                if(e.from != Pos.invalid){
                    entity.sleep();
                }else{
                    e.items.addAll(entity.items);
                    e.from = tile.pos();
                    // ▲ new | old ▼
                    entity.from = Pos.invalid;
                    entity.items.clear();

                    e.cooldown = entity.cooldown = 1;
                    e.noSleep();
                    bump(tile);
                }
            }
        }
    }

    private void poofIn(Tile tile){
        tile.<CraterConveyorEntity>ent().from = tile.pos();
        Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
        tile.entity.noSleep();
    }

    private void poofOut(Tile tile){
        Effects.effect(Fx.plasticburn, tile.drawx(), tile.drawy());
        tile.<CraterConveyorEntity>ent().from = Pos.invalid;
        bump(tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(tile.entity.items.total() == 0) poofIn(tile);
        super.handleItem(item, tile, source);
    }

    @Override
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        if(tile.entity.items.total() == 0) poofIn(tile);
        super.handleStack(item, amount, tile, source);
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        try{return super.removeStack(tile, item, amount);
        }finally{
            if(tile.entity.items.total() == 0) poofOut(tile);
        }
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
        int blendbit1, blendbit2;
        int blendsclx, blendscly;

        int from = Pos.invalid;
        float cooldown;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);

            stream.writeInt(from);
            stream.writeFloat(cooldown);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);

            from = stream.readInt();
            cooldown = stream.readFloat();
        }
    }

    // crater conveyor tiles that input into this one
    private void upstream(Tile tile, Cons<Tile> cons){
        CraterConveyorEntity entity = tile.ent();

        if(    entity.blendbit1 == 0 // 1 input from the back, 0 from the sides
            || entity.blendbit1 == 2 // 1 input from the back, 1 from the sides
            || entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
        ) cons.get(tile.back());     // fixme, fires for 0 while nothing behind

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

    // awaken inputting conveyors
    private void bump(Tile tile){
        upstream(tile, t -> {
            if(t == null || t.entity == null || !t.entity.isSleeping() || t.entity.items.total() <= 0) return;
            t.entity.noSleep();
            bump(t);
        });
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        CraterConveyorEntity entity = tile.ent();

        if (tile == source) return true;                                  // player threw items
        if (entity.cooldown > 0f) return false;                           // still cooling down
        return!((entity.blendbit2 != 5)                                   // not a loading dock
            ||  (entity.items.total() > 0 && !entity.items.has(item))     // incompatible items
            ||  (entity.items.total() >= getMaximumAccepted(tile, item))  // filled to capacity
            ||  (tile.front() == source));                                // fed from the front
    }
}
