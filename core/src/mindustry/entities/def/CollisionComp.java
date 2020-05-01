package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.async.CollisionProcess.*;
import mindustry.gen.*;

/** Can be collided with. Collision elibility depends on team.
 * TODO merge with hitboxcomp?*/
@Component
abstract class CollisionComp implements Hitboxc{
    transient CollisionRef colref;
}
