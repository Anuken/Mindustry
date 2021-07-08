package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class StackConveyor extends Block implements Autotiler{
    protected static final int stateMove = 0, stateLoad = 1, stateUnload = 2;

    public @Load(value = "@-#", length = 3) TextureRegion[] regions;
    public @Load("@-edge") TextureRegion edgeRegion;
    public @Load("@-stack") TextureRegion stackRegion;

    public float speed = 0f;
    public boolean splitOut = true;
    /** (minimum) amount of loading docks needed to fill a line. */
    public float recharge = 2f;
    public Effect loadEffect = Fx.plasticburn;
    public Effect unloadEffect = Fx.plasticburn;

    public StackConveyor(String name){
        super(name);

        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 10;
        conveyorPlacement = true;

        ambientSound = Sounds.conveyor;
        ambientSoundVolume = 0.004f;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, Mathf.round(itemCapacity * speed * 60), StatUnit.itemsSecond);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        if(tile.build instanceof StackConveyorBuild b){
            int state = b.state;
            if(state == stateLoad){ //standard conveyor mode
                return otherblock.outputsItems() && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
            }else if(state == stateUnload){ //router mode
                return otherblock.acceptsItems &&
                    (!otherblock.noSideBlend || lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock)) &&
                    (notLookingAt(tile, rotation, otherx, othery, otherrot, otherblock) ||
                    (otherblock instanceof StackConveyor && facing(otherx, othery, otherrot, tile.x, tile.y))) &&
                    !(world.build(otherx, othery) instanceof StackConveyorBuild s && s.state == stateUnload) &&
                    !(world.build(otherx, othery) instanceof StackConveyorBuild s2 && s2.state == stateMove &&
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
        if(tile instanceof StackConveyorBuild s){
            return s.state != stateUnload;
        }
        return super.rotatedOutput(x, y);
    }

    public class StackConveyorBuild extends Building{
        public int state, blendprox;

        public int link = -1;
        public float cooldown;
        public Item lastItem;
        public float velocityScl;

        boolean proxUpdating = false;

        @Override
        public void draw(){
            Draw.z(Layer.block - 0.2f);

            Draw.rect(regions[state], x, y, rotdeg());

            for(int i = 0; i < 4; i++){
                if((blendprox & (1 << i)) == 0){
                    Draw.rect(edgeRegion, x, y, (rotation - i) * 90);
                }
            }

            Draw.z(Layer.block - 0.1f);

            Tile from = world.tile(link);

            if(link == -1 || from == null || lastItem == null) return;

            int fromRot = from.build == null ? rotation : from.build.rotation;

            //offset
            Tmp.v1.set(from.worldx(), from.worldy());
            Tmp.v2.set(x, y);
            Tmp.v1.interpolate(Tmp.v2, 1f - cooldown, Interp.linear);

            //rotation
            float a = (fromRot%4) * 90;
            float b = (rotation%4) * 90;
            if((fromRot%4) == 3 && (rotation%4) == 0) a = -1 * 90;
            if((fromRot%4) == 0 && (rotation%4) == 3) a =  4 * 90;

            //stack
            Draw.rect(stackRegion, Tmp.v1.x, Tmp.v1.y, Mathf.lerp(a, b, Interp.smooth.apply(1f - Mathf.clamp(cooldown * 2, 0f, 1f))));

            //item
            float size = itemSize * Mathf.lerp(Math.min((float)items.total() / itemCapacity, 1), 1f, 0.4f);
            Drawf.shadow(Tmp.v1.x, Tmp.v1.y, size * 1.2f);
            Draw.rect(lastItem.fullIcon, Tmp.v1.x, Tmp.v1.y, size, size, 0);
        }

        @Override
        public void drawCracks(){
            Draw.z(Layer.block - 0.15f);
            super.drawCracks();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int lastState = state;

            state = stateMove;

            int[] bits = buildBlending(tile, rotation, null, true);
            if(bits[0] == 0 &&  blends(tile, rotation, 0) && !blends(tile, rotation, 2)) state = stateLoad;  // a 0 that faces into a conveyor with none behind it
            if(bits[0] == 0 && !blends(tile, rotation, 0) && blends(tile, rotation, 2)) state = stateUnload; // a 0 that faces into none with a conveyor behind it

            if(!headless){
                blendprox = 0;

                for(int i = 0; i < 4; i++){
                    if(blends(tile, rotation, i)){
                        blendprox |= (1 << i);
                    }
                }
            }

            //cannot load when facing
            if(state == stateLoad){
                for(Building near : proximity){
                    if(near instanceof StackConveyorBuild && near.front() == this){
                        state = stateMove;
                        break;
                    }
                }
            }

            //update other conveyor state when this conveyor's state changes
            if(state != lastState){
                proxUpdating = true;
                for(Building near : proximity){
                    if(!(near instanceof StackConveyorBuild b && b.proxUpdating && b.state != stateUnload)){
                        near.onProximityUpdate();
                    }
                }
                proxUpdating = false;
            }
        }

        @Override
        public boolean canUnload(){
            return state != stateLoad;
        }

        @Override
        public float efficiency(){
            return 1f;
        }

        @Override
        public void updateTile(){
            //let the stack keep its highest acceleration
            velocityScl = Math.max(timeScale, velocityScl);

            //reel in crater
            if(cooldown > 0f) cooldown = Mathf.clamp(cooldown - speed * (efficiency() * Time.delta * velocityScl), 0f, recharge);

            //indicates empty state
            if(link == -1) return;

            //crater needs to be centered
            if(cooldown > 0f) return;

            //get current item
            if(lastItem == null || !items.has(lastItem)){
                lastItem = items.first();
            }

            //do not continue if disabled, will still allow one to be reeled in to prevent visual stacking
            if(!enabled) return;

            if(state == stateUnload){ //unload
                while(lastItem != null && (!splitOut ? moveForward(lastItem) : dump(lastItem))){
                    if(items.empty()) poofOut();
                }
            }else{ //transfer
                if(state != stateLoad || (items.total() >= getMaximumAccepted(lastItem))){
                    if(front() instanceof StackConveyorBuild e && e.team == team){
                        //sleep if its occupied
                        if(e.link == -1){
                            e.items.add(items);
                            e.lastItem = lastItem;
                            e.link = tile.pos();
                            e.velocityScl = velocityScl;
                            //▲ to | from ▼
                            link = -1;
                            items.clear();

                            cooldown = recharge;
                            e.cooldown = 1;
                            velocityScl = 1;
                        }else{
                            //bumped into another
                            velocityScl = 1;
                        }
                    }
                }
            }
        }

        @Override
        public void overwrote(Seq<Building> builds){
            if(builds.first() instanceof ConveyorBuild build){
                Item item = build.items.first();
                if(item != null){
                    handleStack(item, build.items.get(item), null);
                }
            }
        }

        @Override
        public boolean shouldAmbientSound(){
            return false; //has no moving parts;
        }

        protected void poofIn(){
            link = tile.pos();
            loadEffect.at(this);
        }

        protected void poofOut(){
            unloadEffect.at(this);
            link = -1;
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            if(items.any() && !items.has(item)) return 0;
            return super.acceptStack(item, amount, source);
        }

        @Override
        public void handleItem(Building source, Item item){
            if(items.empty() && tile != null) poofIn();
            super.handleItem(source, item);
            lastItem = item;
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            if(amount <= 0) return;
            if(items.empty() && tile != null) poofIn();
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
        public void itemTaken(Item item){
            if(items.empty()) poofOut();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(this == source) return true; //player threw items
            if(cooldown > recharge - 1f) return false; //still cooling down
            return !((state != stateLoad) //not a loading dock
            ||  (items.any() && !items.has(item)) //incompatible items
            ||  (items.total() >= getMaximumAccepted(item)) //filled to capacity
            ||  (front()  == source));
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(link);
            write.f(cooldown);
            write.f(velocityScl);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            link = read.i();
            cooldown = read.f();
            if(revision >= 1) velocityScl = read.f();
        }
    }
}
