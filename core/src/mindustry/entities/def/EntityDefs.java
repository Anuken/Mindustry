package mindustry.entities.def;

import arc.math.geom.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.net.*;

public class EntityDefs{

    @EntityDef({Health.class, Vel.class, Status.class, Connection.class})
    class PlayerDef{}

    class Health{
        float health, maxHealth;
        boolean dead;

        float healthf(){
            return health / maxHealth;
        }
    }

    class Pos{
        float x, y;
    }

    class Vel extends Pos{
        Vec2 vel = new Vec2();

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(0.9f);
        }
    }

    class Status{
        Statuses statuses = new Statuses();

        void update(){
            statuses.update(null);
        }
    }

    class Connection{
        NetConnection connection;
    }

}
