package mindustry.entities.comp;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

/** An entity that holds a payload. */
@Component
abstract class PayloadComp implements Posc, Rotc, Hitboxc, Unitc{
    @Import float x, y, rotation;
    @Import UnitType type;

    Seq<Payload> payloads = new Seq<>();

    float payloadUsed(){
        return payloads.sumf(p -> p.size() * p.size());
    }

    boolean canPickup(Unit unit){
        return payloadUsed() + unit.hitSize * unit.hitSize <= type.payloadCapacity;
    }

    boolean canPickup(Building build){
        return payloadUsed() + build.block.size * build.block.size * Vars.tilesize * Vars.tilesize <= type.payloadCapacity;
    }

    boolean canPickupPayload(Payload pay){
        return payloadUsed() + pay.size()*pay.size() <= type.payloadCapacity;
    }

    boolean hasPayload(){
        return payloads.size > 0;
    }

    void addPayload(Payload load){
        payloads.add(load);
    }

    void pickup(Unit unit){
        unit.remove();
        payloads.add(new UnitPayload(unit));
        Fx.unitPickup.at(unit);
        if(Vars.net.client()){
            Vars.netClient.clearRemovedEntity(unit.id);
        }
    }

    void pickup(Building tile){
        tile.tile.remove();
        payloads.add(new BlockPayload(tile));
        Fx.unitPickup.at(tile);
    }

    boolean dropLastPayload(){
        if(payloads.isEmpty()) return false;

        Payload load = payloads.peek();

        if(tryDropPayload(load)){
            payloads.pop();
            return true;
        }
        return false;
    }

    boolean tryDropPayload(Payload payload){
        Tile on = tileOn();

        //clear removed state of unit so it can be synced
        if(Vars.net.client() && payload instanceof UnitPayload){
            Vars.netClient.clearRemovedEntity(((UnitPayload)payload).unit.id);
        }

        //drop off payload on an acceptor if possible
        if(on != null && on.build != null && on.build.acceptPayload(on.build, payload)){
            Fx.unitDrop.at(on.build);
            on.build.handlePayload(on.build, payload);
            return true;
        }

        if(payload instanceof BlockPayload){
            return dropBlock((BlockPayload)payload);
        }else if(payload instanceof UnitPayload){
            return dropUnit((UnitPayload)payload);
        }
        return false;
    }

    boolean dropUnit(UnitPayload payload){
        Unit u = payload.unit;

        //can't drop ground units
        if(((tileOn() == null || tileOn().solid()) && u.elevation < 0.1f) || (!floorOn().isLiquid && u instanceof WaterMovec)){
            return false;
        }

        Fx.unitDrop.at(this);

        //clients do not drop payloads
        if(Vars.net.client()) return true;

        u.set(this);
        u.trns(Tmp.v1.rnd(Mathf.random(2f)));
        u.rotation(rotation);
        //reset the ID to a new value to make sure it's synced
        u.id = EntityGroup.nextId();
        u.add();

        return true;
    }

    /** @return whether the tile has been successfully placed. */
    boolean dropBlock(BlockPayload payload){
        Building tile = payload.entity;
        int tx = Vars.world.toTile(x - tile.block.offset), ty = Vars.world.toTile(y - tile.block.offset);
        Tile on = Vars.world.tile(tx, ty);
        if(on != null && Build.validPlace(tile.block, tile.team, tx, ty, tile.rotation)){
            int rot = (int)((rotation + 45f) / 90f) % 4;
            payload.place(on, rot);

            Fx.unitDrop.at(tile);
            Fx.placeBlock.at(on.drawx(), on.drawy(), on.block().size);
            return true;
        }

        return false;
    }
}
