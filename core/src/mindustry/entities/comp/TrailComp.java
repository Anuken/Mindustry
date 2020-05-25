package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;

@Component
abstract class TrailComp{
    transient Trail trail = new Trail();
}
