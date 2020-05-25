package mindustry.entities.comp;

import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.blocks.payloads.*;

/** An entity that holds a payload. */
@Component
abstract class PayloadComp{
    Array<Payload> payloads = new Array<>();

    boolean hasPayload(){
        return payloads.size > 0;
    }

    void addPayload(Payload load){
        payloads.add(load);
    }
}
