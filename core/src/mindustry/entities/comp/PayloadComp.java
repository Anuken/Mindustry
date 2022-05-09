package mindustry.entities.comp;

import arc.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;

/** An entity that holds a payload. */
@Component
abstract class PayloadComp implements Posc, Rotc, Hitboxc, Unitc{
    @Import float x, y, rotation;
    @Import Team team;
    @Import UnitType type;

    Seq<Payload> payloads = new Seq<>();

    private transient @Nullable PowerGraph payloadPower;

    @Override
    public void update(){
        if(payloadPower != null){
            payloadPower.clear();
        }

        //update power graph first, resolve everything
        for(Payload pay : payloads){
            if(pay instanceof BuildPayload pb && pb.build.power != null){
                if(payloadPower == null) payloadPower = new PowerGraph();

                pb.build.team = team;
                pb.build.power.graph = null;
                payloadPower.add(pb.build);
            }
        }

        if(payloadPower != null){
            payloadPower.update();
        }

        for(Payload pay : payloads){
            if(pay instanceof BuildPayload build){
                build.build.team = team;
            }
            pay.set(x, y, rotation);
            pay.update(true);
        }
    }

    float payloadUsed(){
        return payloads.sumf(p -> p.size() * p.size());
    }

    boolean canPickup(Unit unit){
        return type.pickupUnits && payloadUsed() + unit.hitSize * unit.hitSize <= type.payloadCapacity + 0.001f && unit.team == team() && unit.isAI();
    }

    boolean canPickup(Building build){
        return payloadUsed() + build.block.size * build.block.size * Vars.tilesize * Vars.tilesize <= type.payloadCapacity + 0.001f && build.canPickup();
    }

    boolean canPickupPayload(Payload pay){
        return payloadUsed() + pay.size()*pay.size() <= type.payloadCapacity + 0.001f && (type.pickupUnits || !(pay instanceof UnitPayload));
    }

    boolean hasPayload(){
        return payloads.size > 0;
    }

    void addPayload(Payload load){
        payloads.add(load);
    }

    void pickup(Unit unit){
        unit.remove();
        addPayload(new UnitPayload(unit));
        Fx.unitPickup.at(unit);
        if(Vars.net.client()){
            Vars.netClient.clearRemovedEntity(unit.id);
        }
        Events.fire(new PickupEvent(self(), unit));
    }

    void pickup(Building tile){
        tile.pickedUp();
        tile.tile.remove();
        tile.afterPickedUp();
        addPayload(new BuildPayload(tile));
        Fx.unitPickup.at(tile);
        Events.fire(new PickupEvent(self(), tile));
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
        if(Vars.net.client() && payload instanceof UnitPayload u){
            Vars.netClient.clearRemovedEntity(u.unit.id);
        }

        //drop off payload on an acceptor if possible
        if(on != null && on.build != null && on.build.acceptPayload(on.build, payload)){
            Fx.unitDrop.at(on.build);
            on.build.handlePayload(on.build, payload);
            return true;
        }

        if(payload instanceof BuildPayload b){
            return dropBlock(b);
        }else if(payload instanceof UnitPayload p){
            return dropUnit(p);
        }
        return false;
    }

    boolean dropUnit(UnitPayload payload){
        Unit u = payload.unit;

        //can't drop ground units
        if(!u.canPass(tileX(), tileY()) || Units.count(x, y, u.physicSize(), o -> o.isGrounded()) > 1){
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
        //decrement count to prevent double increment
        if(!u.isAdded()) u.team.data().updateCount(u.type, -1);
        u.add();
        u.unloaded();
        Events.fire(new PayloadDropEvent(self(), u));

        return true;
    }

    /** @return whether the tile has been successfully placed. */
    boolean dropBlock(BuildPayload payload){
        Building tile = payload.build;
        int tx = World.toTile(x - tile.block.offset), ty = World.toTile(y - tile.block.offset);
        Tile on = Vars.world.tile(tx, ty);
        if(on != null && Build.validPlace(tile.block, tile.team, tx, ty, tile.rotation, false)){
            int rot = (int)((rotation + 45f) / 90f) % 4;
            payload.place(on, rot);
            Events.fire(new PayloadDropEvent(self(), tile));

            if(getControllerName() != null){
                payload.build.lastAccessed = getControllerName();
            }

            Fx.unitDrop.at(tile);
            Fx.placeBlock.at(on.drawx(), on.drawy(), on.block().size);
            return true;
        }

        return false;
    }

    void contentInfo(Table table, float itemSize, float width){
        table.clear();
        table.top().left();

        float pad = 0;
        float items = payloads.size;
        if(itemSize * items + pad * items > width){
            pad = (width - (itemSize) * items) / items;
        }

        for(Payload p : payloads){
            table.image(p.icon()).size(itemSize).padRight(pad);
        }
    }
}
