package io.anuke.mindustry.entities.type.base;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class RepairDrone extends BaseDrone{
    private Unit unit = (Unit)this;
    private Fire smoke;
    private Float nozzle = 0.05f;

    public final UnitState repair = new UnitState(){

        public void entered(){
            target = null;
            liquid.amount = 0;
        }

        public void update(){

            if(retarget()){
                if(fires().isEmpty()){
                    target = Units.findDamagedTile(team, x, y);
                }else{
                    setState(refill);
                    return;
                }
            }

            if(target instanceof TileEntity && ((TileEntity)target).block instanceof BuildBlock){
                target = null;
            }

            if(target != null){
                if(target.dst(RepairDrone.this) > type.range){
                    circle(type.range * 0.9f);
                }else{
                    getWeapon().update(RepairDrone.this, target.getX(), target.getY());
                }
            }else{
                //circle spawner if there's nothing to repair
                if(getSpawner() != null){
                    target = getSpawner();
                    circle(type.range * 1.5f, type.speed/2f);
                    target = null;
                }
            }
        }
    };

    public final UnitState refill = new UnitState(){
        @Override
        public void update(){

            if(fires().isEmpty()){
                setState(repair);
                return;
            }

            if(getSpawner() != null){
                target = getSpawner();
                if(target.dst(RepairDrone.this) > type.range){
                    circle(type.range * 0.9f);
                }else{
                    if(Mathf.chance(Time.delta() * 0.25)){
                        liquid.liquid = getSpawner().entity.liquids.current();

                        if(getSpawner().entity.liquids.total() >= nozzle){
                            Call.transferItemEffect(Items.titanium, target.getX(), target.getY(), unit);
                            getSpawner().entity.liquids.remove(liquid.liquid, nozzle);
                            liquid.amount = Mathf.clamp(liquid.amount += nozzle, 0, getLiquidCapacity());
                        }
                    }
                    if(liquid.amount == getLiquidCapacity()) setState(firefight);
                }
            }
        }
    };

    public final UnitState firefight = new UnitState(){

        @Override
        public void entered(){
            target = null;
        }

        @Override
        public void update(){

            if(fires().isEmpty()){
                setState(repair);
                return;
            }

            if(retarget()){
                smoke = Geometry.findClosest(x, y, fires());
                target = world.tileWorld(smoke.x, smoke.y);
            }

            if(target != null){
                if(target.dst(RepairDrone.this) > type.range){
                    circle(type.range * 0.9f);
                }else{
                    getSecondary().update(RepairDrone.this, target.getX(), target.getY());
                    liquid.amount = Mathf.clamp(liquid.amount -= nozzle / 5, 0, getLiquidCapacity());
                    if(liquid.amount == 0) setState(refill);
                }
            }
        }
    };

    private Array<Fire> fires(){
        return fireGroup.all();
    }

    @Override
    public Weapon getWeapon(){
        return state.current() == repair ? super.getWeapon() : getSecondary();
    }

    @Override
    public boolean shouldRotate(){
        return target != null;
    }

    @Override
    public UnitState getStartState(){
        return repair;
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.write(data);
        data.writeInt(state.is(repair) && target instanceof TileEntity ? ((TileEntity)target).tile.pos() : Pos.invalid);
    }

    @Override
    public void read(DataInput data) throws IOException{
        super.read(data);
        Tile repairing = world.tile(data.readInt());

        if(repairing != null){
            target = repairing.entity;
        }
    }
}
