package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

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
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock instanceof CraterConveyor; // blend with nothing but crater conveyors
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[bits[0]];
        Draw.rect(region, req.drawx(), req.drawy(), region.getWidth() * bits[1] * Draw.scl * req.animScale, region.getHeight() * bits[2] * Draw.scl * req.animScale, req.rotation * 90);
    }

    class CraterConveyorEntity extends TileEntity{

        int blendbit1, blendbit2;
        int blendsclx, blendscly;

        int link = -1;
        float cooldown;

        @Override
        public void draw(){
            draw(blendbit1);
            if(blendbit2 == 0) return;
            draw(blendbit2);
        }

        private void draw(int bit){
            Draw.rect(regions[Mathf.clamp(bit, 0, regions.length - 1)], x, y, tilesize * blendsclx, tilesize * blendscly, rotation() * 90);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, tile.rotation(), null, true);

            blendbit2 = 0;
            if(bits[0] == 0 && blends(tile, tile.rotation(), 0) && !blends(tile, tile.rotation(), 2)) blendbit2 = 5; // a 0 that faces into a crater conveyor with none behind it
            if(bits[0] == 0 && !blends(tile, tile.rotation(), 0) && blends(tile, tile.rotation(), 2)) blendbit2 = 6; // a 0 that faces into none with a crater conveyor behind it

            blendbit1 = bits[0];
            blendsclx = bits[1];
            blendscly = bits[2];
        }

        @Override
        public void drawLayer(){
            if(link == -1) return;

            // offset
            Tile from = world.tile(link);
            Tmp.v1.set(from);
            Tmp.v2.set(tile);
            Tmp.v1.interpolate(Tmp.v2, 1f - cooldown, Interpolation.linear);

            // fixme
            float a = (from.rotation()%4) * 90;
            float b = (tile.rotation()%4) * 90;
            if((from.rotation()%4) == 3 && (tile.rotation()%4) == 0) a = -1 * 90;
            if((from.rotation()%4) == 0 && (tile.rotation()%4) == 3) a = 4 * 90;

            // crater
            Draw.rect(regions[7], Tmp.v1.x, Tmp.v1.y, Mathf.lerp(a, b, Interpolation.smooth.apply(1f - Mathf.clamp(cooldown * 2, 0f, 1f))));

            // item
            float size = itemSize * Mathf.lerp(Math.min((float)items.total() / itemCapacity, 1), 1f, 0.4f);
            Drawf.shadow(Tmp.v1.x, Tmp.v1.y, size * 1.2f);
            Draw.rect(items.first().icon(Cicon.medium), Tmp.v1.x, Tmp.v1.y, size, size, 0);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return Mathf.round(super.getMaximumAccepted(item) * timeScale); // increased item capacity while boosted
        }

        @Override
        public boolean shouldIdleSound(){
            return false; // has no moving parts;
        }

        private void poofIn(){
            link = tile.pos();
            Fx.plasticburn.at(this);
            tile.entity.noSleep();
        }

        private void poofOut(){
            Fx.plasticburn.at(this);
            link = -1;
            bump(this);
        }

        @Override
        public void handleItem(Tilec source, Item item){
            if(items.total() == 0) poofIn();
            super.handleItem(source, item);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            if(items.total() == 0) poofIn();
            super.handleStack(item, amount, source);
        }

        @Override
        public int removeStack(Item item, int amount){
            try{
                return super.removeStack(item, amount);
            }finally{
                if(items.total() == 0) poofOut();
            }
        }

        // crater conveyor tiles that input into this one
        private void upstream(Tilec tile, Cons<Tilec> cons){
            CraterConveyorEntity entity = (CraterConveyorEntity)tile;

            if(    entity.blendbit1 == 0 // 1 input from the back, 0 from the sides
                || entity.blendbit1 == 2 // 1 input from the back, 1 from the sides
                || entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
            ) cons.get(back());          // fixme, fires for 0 while nothing behind

            if(    entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
                || entity.blendbit1 == 4 // 0 input from the back, 2 from the sides
                ||(entity.blendbit1 == 1 && entity.blendscly == -1) // side is open
                ||(entity.blendbit1 == 2 && entity.blendscly == +1) // side is open
            ) cons.get(right());

            if(    entity.blendbit1 == 3 // 1 input from the back, 2 from the sides
                || entity.blendbit1 == 4 // 0 input from the back, 2 from the sides
                ||(entity.blendbit1 == 1 && entity.blendscly == +1) // side is open
                ||(entity.blendbit1 == 2 && entity.blendscly == -1) // side is open
            ) cons.get(left());
        }

        // awaken inputting conveyors
        private void bump(Tilec tile){
            upstream(tile, t -> {
                if(t == null || !t.isSleeping() || t.items().total() <= 0) return;
                t.noSleep();
                bump(t);
            });
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            if (this == source) return true;                // player threw items
            if (cooldown > 0f) return false;                // still cooling down
            return!((blendbit2 != 5)                        // not a loading dock
            ||  (items.total() > 0 && !items.has(item))     // incompatible items
            ||  (items.total() >= getMaximumAccepted(item)) // filled to capacity
            ||  (tile.front() == source));
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(link);
            write.f(cooldown);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            link = read.i();
            cooldown = read.f();
        }

        @Override
        public void updateTile(){
            // reel in crater
            if(cooldown > 0f) cooldown = Mathf.clamp(cooldown - speed, 0f, 1f);

            // sleep when idle
            if(link == -1){
                if(cooldown == 0f) sleep();
                return;
            }

            // crater needs to be centered
            if(cooldown > 0f) return;

            if(blendbit2 == 6){
                while(dump()) if(items.total() == 0) poofOut();
            }

            /* unload */ else /* transfer */

            if(blendbit2 != 5 || (items.total() >= getMaximumAccepted(items.first()))){
                if(front() != null
                && front().team() == team()
                && front().block() instanceof CraterConveyor){
                    CraterConveyorEntity e = (CraterConveyorEntity)tile.front();

                    // sleep if its occupied
                    if(e.link != -1){
                        sleep();
                    }else{
                        e.items.addAll(items);
                        e.link = tile.pos();
                        // ▲ new | old ▼
                        link = -1;
                        items.clear();

                        e.cooldown = cooldown = 1;
                        e.noSleep();
                        bump(this);
                    }
                }
            }
        }
    }
}
