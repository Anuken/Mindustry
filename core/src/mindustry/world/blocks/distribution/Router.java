package mindustry.world.blocks.distribution;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

public class Router extends Block{
    public float speed = 8f;

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

    public class RouterBuild extends Building implements ControlBlock{
        public Item lastItem;
        public Tile lastInput;
        public float time;
        public @Nullable BlockUnitc unit;

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
            if(lastItem == null && items.any()){
                lastItem = items.first();
            }

            if(lastItem != null){
                time += 1f / speed * delta();
                Building target = getTileTarget(lastItem, lastInput, false);

                if(target != null && (time >= 1f || !(target.block instanceof Router || target.block.instantTransfer))){
                    getTileTarget(lastItem, lastInput, true);
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
            return team == source.team && lastItem == null && items.total() == 0;
        }

        @Override
        public void handleItem(Building source, Item item){
            items.add(item, 1);
            lastItem = item;
            time = 0f;
            lastInput = source.tile;
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);
            if(result != 0 && item == lastItem){
                lastItem = null;
            }
            return result;
        }

        public Building getTileTarget(Item item, Tile from, boolean set){
            if(unit != null && isControlled()){
                unit.health(health);
                unit.ammo(unit.type().ammoCapacity * (items.total() > 0 ? 1f : 0f));
                unit.team(team);
                unit.set(x, y);

                if(unit.isShooting()){

                    int angle = Mathf.mod((int)((angleTo(unit.aimX(), unit.aimY()) + 45) / 90), 4);
                    Building other = nearby(rotation = angle);
                    if(other != null && other.acceptItem(this, item)){
                        return other;
                    }
                }else if((size & 1) != 0){
                    if(!Vars.headless && Vars.player.unit() == unit){
                        int xi = (int)Core.input.axis(Binding.moveX);
                        int yi = (int)Core.input.axis(Binding.moveY);
                        Building xb = nearby((xi + (xi < 0 ? -size : size)) >> 1, 0);
                        Building yb = nearby(0, (yi + (yi < 0 ? -size : size)) >> 1);
                        int xr = 1 - xi;
                        int yr = (yi + 4) & 3;
                        if(xi == 0f && yi == 0f) return null;
                        if(xi == 0f){
                            if(set) rotation = yr;
                            return yb != null && yb.acceptItem(this, item) ? yb : null;
                        }
                        if(yi == 0f){
                            if(set) rotation = xr;
                            return xb != null && xb.acceptItem(this, item) ? xb : null;
                        }
                        if((rotation & 1) == 0){
                            if(xb != null && xb.acceptItem(this, item)){
                                if(set) rotation = yr;
                                return xb;
                            }else if(yb != null && yb.acceptItem(this, item)){
                                return yb;
                            }
                        }else{
                            if(yb != null && yb.acceptItem(this, item)){
                                if(set) rotation = xr;
                                return yb;
                            }else if(xb != null && xb.acceptItem(this, item)){
                                return xb;
                            }
                        }
                    }else{
                        int off = (1 + size) >> 1;
                        return switch(rotation){
                            case 0 -> nearby(off, 0);
                            case 1 -> nearby(0, off);
                            case 2 -> nearby(-off, 0);
                            case 3 -> nearby(0, -off);
                            default -> null;
                        };
                    }
                }

                return null;
            }

            int counter = rotation;
            for(int i = 0; i < proximity.size; i++){
                Building other = proximity.get((i + counter) % proximity.size);
                if(set) rotation = ((byte)((rotation + 1) % proximity.size));
                if(other.tile == from && from.block() == Blocks.overflowGate) continue;
                if(other.acceptItem(this, item)){
                    return other;
                }
            }
            return null;
        }
    }
}
