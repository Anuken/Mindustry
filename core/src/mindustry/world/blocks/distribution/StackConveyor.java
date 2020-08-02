package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
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

public class StackConveyor extends Block implements Autotiler{
    protected static final int stateMove = 0, stateLoad = 1, stateUnload = 2;

    public @Load(value = "@-#", length = 3) TextureRegion[] regions;
    public @Load("@-edge") TextureRegion edgeRegion;
    public @Load("@-stack") TextureRegion stackRegion;

    public float speed = 0f;
    public boolean splitOut = true;
    public float recharge = 2f;

    public StackConveyor(String name){
        super(name);

        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 10;
        conveyorPlacement = true;

        idleSound = Sounds.conveyor;
        idleSoundVolume = 0.004f;
        unloadable = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.itemsMoved, Mathf.round(itemCapacity * speed * 60), StatUnit.itemsSecond);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        if(tile.build instanceof StackConveyorEntity){
            int state = ((StackConveyorEntity)tile.build).state;
            if(state == stateLoad){ //standard conveyor mode
                return otherblock.outputsItems() && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
            }else if(state == stateUnload){ //router mode
                return otherblock.acceptsItems &&
                    (notLookingAt(tile, rotation, otherx, othery, otherrot, otherblock) ||
                    (otherblock instanceof StackConveyor && facing(otherx, othery, otherrot, tile.x, tile.y))) &&
                    !(world.build(otherx, othery) instanceof StackConveyorEntity && ((StackConveyorEntity)world.build(otherx, othery)).state == stateUnload) &&
                    !(world.build(otherx, othery) instanceof StackConveyorEntity && ((StackConveyorEntity)world.build(otherx, othery)).state == stateMove &&
                        !facing(otherx, othery, otherrot, tile.x, tile.y));
            }
        }
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock instanceof StackConveyor;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[0];
        Draw.rect(region, req.drawx(), req.drawy(), req.rotation * 90);

        for(int i = 0; i < 4; i++){
            if((bits[3] & (1 << i)) == 0){
                Draw.rect(edgeRegion, req.drawx(), req.drawy(), (req.rotation - i) * 90);
            }
        }
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        Building tile = world.build(x, y);
        if(tile instanceof StackConveyorEntity){
            return ((StackConveyorEntity)tile).state != stateUnload;
        }
        return super.rotatedOutput(x, y);
    }

    public class StackConveyorEntity extends Building{
        public int state, blendprox;

        public int link = -1;
        public float cooldown;
        public Item lastItem;

        @Override
        public void draw(){
            Draw.rect(regions[state], x, y, rotdeg());

            for(int i = 0; i < 4; i++){
                if((blendprox & (1 << i)) == 0){
                    Draw.rect(edgeRegion, x, y, (rotation - i) * 90);
                }
            }

            Draw.z(Layer.blockOver);

            Building from = world.build(link);

            if(link == -1 || from == null) return;

            //offset
            Tmp.v1.set(from);
            Tmp.v2.set(tile);
            Tmp.v1.interpolate(Tmp.v2, 1f - cooldown, Interp.linear);

            //rotation
            float a = (from.rotation%4) * 90;
            float b = (rotation%4) * 90;
            if((from.rotation%4) == 3 && (rotation%4) == 0) a = -1 * 90;
            if((from.rotation%4) == 0 && (rotation%4) == 3) a =  4 * 90;

            //stack
            Draw.rect(stackRegion, Tmp.v1.x, Tmp.v1.y, Mathf.lerp(a, b, Interp.smooth.apply(1f - Mathf.clamp(cooldown * 2, 0f, 1f))));

            //item
            float size = itemSize * Mathf.lerp(Math.min((float)items.total() / itemCapacity, 1), 1f, 0.4f);
            Drawf.shadow(Tmp.v1.x, Tmp.v1.y, size * 1.2f);
            Draw.rect(items.first().icon(Cicon.medium), Tmp.v1.x, Tmp.v1.y, size, size, 0);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int lastState = state;

            state = stateMove;

            int[] bits = buildBlending(tile, rotation, null, true);
            if(bits[0] == 0 &&  blends(tile, rotation, 0) && !blends(tile, rotation, 2)) state = stateLoad;  // a 0 that faces into a conveyor with none behind it
            if(bits[0] == 0 && !blends(tile, rotation, 0) && blends(tile, rotation, 2)) state = stateUnload; // a 0 that faces into none with a conveyor behind it
            
            blendprox = 0;

            for(int i = 0; i < 4; i++){
                if(blends(tile, rotation, i)){
                    blendprox |= (1 << i);
                }
            }

            //update other conveyor state when this conveyor's state changes
            if(state != lastState){
                for(Building near : proximity){
                    if(near instanceof StackConveyorEntity){
                        near.onProximityUpdate();
                        for(Building other : near.proximity){
                            if(!(other instanceof StackConveyorEntity)) other.onProximityUpdate();
                        }
                    }
                }
            }
        }

        @Override
        public void updateTile(){
            // reel in crater
            if(cooldown > 0f) cooldown = Mathf.clamp(cooldown - speed * edelta(), 0f, recharge);

            if(link == -1){
                return;
            }

            // crater needs to be centered
            if(cooldown > 0f) return;

            // get current item
            if(lastItem == null){
                lastItem = items.first();
            }

            if(state == stateUnload){ //unload
                while(lastItem != null && (!splitOut ? moveForward(lastItem) : dump(lastItem))){
                    if(items.empty()) poofOut();
                }
            }else{ //transfer
                if(state != stateLoad || (items.total() >= getMaximumAccepted(lastItem))){
                    if(front() != null
                    && front().team == team
                    && front().block instanceof StackConveyor){
                        StackConveyorEntity e = (StackConveyorEntity)front();

                        // sleep if its occupied
                        if(e.link == -1){
                            e.items.addAll(items);
                            e.lastItem = lastItem;
                            e.link = tile.pos();
                            // ▲ to | from ▼
                            link = -1;
                            items.clear();

                            cooldown = recharge;
                            e.cooldown = 1;
                        }
                    }
                }
            }
        }

        @Override
        public boolean shouldIdleSound(){
            return false; // has no moving parts;
        }

        private void poofIn(){
            link = tile.pos();
            Fx.plasticburn.at(this);
        }

        private void poofOut(){
            Fx.plasticburn.at(this);
            link = -1;
        }

        @Override
        public void handleItem(Building source, Item item){
            if(items.empty()) poofIn();
            super.handleItem(source, item);
            lastItem = item;
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            if(items.empty()) poofIn();
            super.handleStack(item, amount, source);
            lastItem = item;
        }

        @Override
        public int removeStack(Item item, int amount){
            try{
                return super.removeStack(item, amount);
            }finally{
                if(items.empty()) poofOut();
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(this == source) return true;                 // player threw items
            if(cooldown > recharge - 1f) return false;      // still cooling down
            return !((state != stateLoad)                   // not a loading dock
            ||  (items.total() > 0 && !items.has(item))     // incompatible items
            ||  (items.total() >= getMaximumAccepted(item)) // filled to capacity
            ||  (front()  == source));
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
    }
}
