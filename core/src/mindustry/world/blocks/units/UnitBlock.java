package mindustry.world.blocks.units;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

public class UnitBlock extends PayloadAcceptor{

    public UnitBlock(String name){
        super(name);

        outputsPayload = true;
        rotate = true;
        update = true;
        solid = true;
    }

    @Remote(called = Loc.server)
    public static void onUnitBlockSpawn(Tile tile){
        if(!(tile.entity instanceof UnitBlockEntity)) return;
        tile.<UnitBlockEntity>ent().spawned();
    }


    public class UnitBlockEntity extends PayloadAcceptorEntity<UnitPayload>{
        public float progress, payloadPos, time, speedScl;

        public void spawned(){
            progress = 0f;

            Tmp.v1.trns(rotdeg(), size * tilesize/2f);
            Fx.smeltsmoke.at(x + Tmp.v1.x, y + Tmp.v1.y);

            if(!net.client() && payload != null){
                Unitc unit = payload.unit;
                unit.set(x, y);
                unit.rotation(rotdeg());
                unit.vel().trns(rotdeg(), payloadSpeed * 2f).add(Mathf.range(0.1f), Mathf.range(0.1f));
                unit.trns(Tmp.v1.trns(rotdeg(), size * tilesize/2f));
                unit.trns(unit.vel());
                unit.add();
                Events.fire(new UnitCreateEvent(unit));
            }

            payload = null;
            payloadPos = 0f;
        }

        public void outputPayload(){
            if(payload == null) return;

            payloadPos += edelta() * payloadSpeed;
            payVector.trns(rotdeg(), payloadPos);
            payRotation = rotdeg();

            if(payloadPos >= size * tilesize/2f){
                payloadPos = size * tilesize/2f;

                Tile front = frontLarge();
                if(front != null && front.entity != null && front.block().outputsPayload){
                    if(movePayload(payload)){
                        payload = null;
                    }
                }else if(front != null && !front.solid()){
                    //create unit if there's space
                    Call.onUnitBlockSpawn(tile);
                }
            }
        }
    }
}
