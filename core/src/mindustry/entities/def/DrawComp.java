package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class DrawComp implements Posc{

    abstract float clipSize();

    void draw(){

    }
}
