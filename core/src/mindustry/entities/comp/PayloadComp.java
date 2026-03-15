package mindustry.entities.comp;

import arc.*;
import arc.audio.*;
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
                if(payloadPower == null) payloadPower = new PowerGraph(false);

                //pb.build.team = team;
                pb.build.power.graph = null;
                payloadPower.add(pb.build);
            }
        }

        if(payloadPower != null){
            payloadPower.update();
        }

        for(Payload pay : payloads){
            //apparently BasedUser doesn't want this and several plugins use it
            //if(pay instanceof BuildPayload build){
            //    build.build.team = team;
            //}
            pay.set(x, y, rotation);
            pay.update(self(), null);
        }
    }

    @Override
    public void remove(){
        for(Payload pay : payloads){
            pay.remove();
        }
        payloads.clear();
    }

    public void destroy(){
        if(Vars.state.rules.unitPayloadsExplode) payloads.each(Payload::destroyed);
    }

    float payloadUsed(){
        return payloads.sumf(p -> p.size() * p.size());
    }

    boolean canPickup(Unit unit){
        return type.pickupUnits && payloadUsed() + unit.hitSize * unit.hitSize <= type.payloadCapacity + 0.001f && unit.team == team() && unit.isAI() && unit.type.allowedInPayloads;
    }

    boolean canPickup(Building build){
        return payloadUsed() + build.block.size * build.block.size * Vars.tilesize * Vars.tilesize <= type.payloadCapacity + 0.001f && build.canPickup() && build.team == team;
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
        if(unit.isAdded()) unit.team.data().updateCount(unit.type, 1);

        unit.remove();
        addPayload(new UnitPayload(unit));
        Fx.unitPickup.at(unit);
        if(Vars.net.client()){
            Vars.netClient.clearRemovedEntity(unit.id);
        }
        Sounds.payloadPickup.at(self(), Mathf.random(0.9f, 1.1f));
        Events.fire(new PickupEvent(self(), unit));
    }

    void pickup(Building tile){
        tile.pickedUp();
        tile.tile.remove();
        tile.afterPickedUp();
        addPayload(new BuildPayload(tile));
        Fx.unitPickup.at(tile);
        Sounds.payloadPickup.at(self());
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
        if(on != null && on.build != null && on.build.team == team && on.build.acceptPayload(on.build, payload)){
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

        //add random offset to prevent unit stacking
        Tmp.v1.rnd(Mathf.random(2f));

        //can't drop ground units
        //allow stacking for small units for now - otherwise, unit transfer would get annoying
        if(!u.canPass(World.toTile(x + Tmp.v1.x), World.toTile(y + Tmp.v1.y)) || Units.count(x, y, u.physicSize(), o -> o.isGrounded() && o.hitSize > 14f) > 1){
            return false;
        }

        Fx.unitDrop.at(this);

        //clients do not drop payloads
        if(Vars.net.client()) return true;

        u.set(x + Tmp.v1.x, y + Tmp.v1.y);
        u.rotation(rotation);
        //reset the ID to a new value to make sure it's synced
        u.id = EntityGroup.nextId();
        //decrement count to prevent double increment
        if(!u.isAdded()) u.team.data().updateCount(u.type, -1);
        u.add();
        u.unloaded();
        Sound dropSound =
            payload.size() <= 12f ? Sounds.payloadDrop1 :
            payload.size() <= 20f ? Sounds.payloadDrop2 :
            Sounds.payloadDrop3;
        dropSound.at(self(), Mathf.random(0.9f, 1.1f));
        Events.fire(new PayloadDropEvent(self(), u));

        return true;
    }

    /** @return whether the tile has been successfully placed. */
    boolean dropBlock(BuildPayload payload){
        Building tile = payload.build;
        int tx = World.toTile(x - tile.block.offset), ty = World.toTile(y - tile.block.offset);
        Tile on = Vars.world.tile(tx, ty);
        if(on != null && Build.validPlace(tile.block, tile.team, tx, ty, tile.rotation, false)){
            payload.place(on, tile.rotation);
            Events.fire(new PayloadDropEvent(self(), tile));

            if(getControllerName() != null){
                payload.build.lastAccessed = getControllerName();
            }

            Fx.unitDrop.at(tile);
            on.block().placeEffect.at(on.drawx(), on.drawy(), on.block().size);
            on.block().placeSound.at(tile);
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
