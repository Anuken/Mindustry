package mindustry.entities.comp;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

/** An entity that holds a payload. */
@Component
abstract class PayloadComp implements Posc, Rotc{
    Array<Payload> payloads = new Array<>();

    boolean hasPayload(){
        return payloads.size > 0;
    }

    void addPayload(Payload load){
        payloads.add(load);
    }

    void pickup(Unitc unit){
        unit.remove();
        payloads.add(new UnitPayload(unit));
        Fx.unitPickup.at(unit);
    }

    void pickup(Tilec tile){
        tile.tile().remove();
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
        if(payload instanceof BlockPayload){
            return dropBlock((BlockPayload)payload);
        }else if(payload instanceof UnitPayload){
            return dropUnit((UnitPayload)payload);
        }
        return false;
    }

    boolean dropUnit(UnitPayload payload){
        //TODO create an effect here and/or make them be at a lower elevation
        Unitc u = payload.unit;

        //can't drop ground units
        if((tileOn() == null || tileOn().solid()) && u.elevation() < 0.1f){
            return false;
        }

        u.set(this);
        u.trns(Tmp.v1.rnd(Mathf.random(2f)));
        u.add();
        Fx.unitDrop.at(u);

        return true;
    }

    /** @return whether the tile has been successfully placed. */
    boolean dropBlock(BlockPayload payload){
        Tilec tile = payload.entity;
        int tx = tileX(), ty = tileY();
        Tile on = tileOn();
        if(Build.validPlace(tile.team(), tx, ty, tile.block(), tile.rotation())){
            int rot = (int)((rotation() + 45f) / 90f) % 4;
            payload.place(tileOn(), rot);

            Fx.unitDrop.at(tile);
            Fx.placeBlock.at(on.drawx(), on.drawy(), on.block().size);
            return true;
        }

        return false;
    }
}
