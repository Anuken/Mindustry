package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class RotComp implements Entityc{
    float rotation;

    void interpolate(){
        Syncc sync = as(Syncc.class);

        if(sync.interpolator().values.length > 0){
            rotation = sync.interpolator().values[0];
        }
    }
}
