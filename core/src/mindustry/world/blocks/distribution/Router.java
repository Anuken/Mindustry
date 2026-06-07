package mindustry.world.blocks.distribution;

import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

public class Router extends Block{
    public float speed = 8f;
    /** Whether this block can be chained, regardless of max consecutive limits. */
    public boolean allowChaining = true;

    public Router(String name){
        super(name);
        solid = false;
        underBullets = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
        unloadable = false;
        noUpdateDisabled = true;
    }

    @Override
    public void init(){
        super.init();
        if(instantTransfer){
            itemCapacity = 0;
        }
    }

    public class RouterBuild extends Building implements ControlBlock{
        public Item lastItem;
        public Tile lastInput;
        public float time;
        public @Nullable BlockUnitc unit;
        public int consecutive;

        @Override
        public Unit unit(){
            if(unit == null){
                unit = (BlockUnitc)UnitTypes.block.create(team);
                unit.tile(this);
            }
            return (Unit)unit;
        }

        @Override
        public boolean canControl(){
            return size == 1;
        }

        @Override
        public boolean shouldAutoTarget(){
            return false;
        }

        @Override
        public void updateTile(){
            if(instantTransfer) return;

            if(lastItem == null && items.any()){
                lastItem = items.first();
            }

            if(lastItem != null){
                time += 1f / speed * delta();
                Building target = getTileTarget(lastItem, lastInput, null, false);

                if(target != null && (time >= 1f || !(target.block instanceof Router || target.block.instantTransfer))){
                    getTileTarget(lastItem, lastInput, target, true);
                    target.handleItem(this, lastItem);
                    items.remove(lastItem, 1);
                    lastItem = null;
                }
            }
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(!instantTransfer){
                return team == source.team && lastItem == null && items.total() == 0;
            }

            int chained = acceptTransfer(source);
            Building to = getTileTarget(item, source.tile, source, false);
            handleTransfer(chained);

            return to != null && to.team == team;
        }

        @Override
        public void handleItem(Building source, Item item){
            if(!instantTransfer){
                items.add(item, 1);
                lastItem = item;
                time = 0f;
                lastInput = source.tile;
                return;
            }

            int chained = acceptTransfer(source);
            Building target = getTileTarget(item, source.tile, source, true);
            handleTransfer(chained);

            if(target != null) target.handleItem(this, item);

            lastInput = source.tile;
        }

        @Override
        public int removeStack(Item item, int amount){
            if(!instantTransfer){
                int result = super.removeStack(item, amount);
                if(result != 0 && item == lastItem){
                    lastItem = null;
                }
                return result;
            }
            return 0;
        }

        public int acceptTransfer(Building source){
            int prev = consecutive;
            consecutive = prev == 0 && source != null && source.block.instantTransfer ? 2 : prev + 1;
            return prev;
        }

        public void handleTransfer(int chained){
            consecutive = chained;
        }

        public boolean canBackflow(Building other, Building src){
            return other != src || src == null || !src.block.instantTransfer;
        }

        public boolean canChain(Building other){
            return !other.block.instantTransfer || (consecutive < maxConsecutive && (allowChaining || !(other.block instanceof Router)));
        }

        public @Nullable Building getTileTarget(Item item, Tile from, Building src, boolean set){
            if(unit != null && isControlled()){
                unit.health(health);
                unit.ammo((items.total() > 0 ? 1f : 0f));
                unit.team(team);
                unit.set(x, y);

                int angle = Mathf.mod((int)((angleTo(unit.aimX(), unit.aimY()) + 45) / 90), 4);

                if(unit.isShooting()){
                    Building other = proximityEdge(rotation = angle, build -> canBackflow(build, src) && canChain(build) && build.team == team && build.acceptItem(this, item));
                    if(other != null){
                        return other;
                    }
                }

                return null;
            }

            int counter = rotation;
            for(int i = 0; i < proximity.size; i++){
                Building other = proximity.get((i + counter) % proximity.size);

                if(set) rotation = ((byte)((rotation + 1) % proximity.size)); 
                if((from != null && other.tile == from && from.block() == Blocks.overflowGate)
                    || !canBackflow(other, src) 
                    || (!allowChaining && other.block instanceof Router)) continue;
                if(!canChain(other)) continue;
                if(other.team == team && other.acceptItem(this, item)){
                    return other;
                }
            }
            return null;
        }
    }
}
