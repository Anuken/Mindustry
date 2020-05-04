package mindustry.entities.def;

import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.blocks.payloads.*;

/** An entity that holds a payload. */
@Component
abstract class PayloadComp{
    //TODO multiple payloads?
    @Nullable Payload payload;

    boolean hasPayload(){
        return payload != null;
    }
}
