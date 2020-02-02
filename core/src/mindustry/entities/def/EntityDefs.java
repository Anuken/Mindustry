package mindustry.entities.def;

import arc.math.geom.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.net.*;

public class EntityDefs{

    @EntityDef({Unit.class, Connection.class})
    class PlayerDef{}

    @Depends({Health.class, Vel.class, Status.class})
    class Unit{

    }

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

    @Depends(Pos.class)
    class Vel{
        //transient fields act as imports from any other clases; these are ignored by the generator
        transient float x, y;

        final Vec2 vel = new Vec2();

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(0.9f);
        }
    }

    class Status{
        final Statuses statuses = new Statuses();

        void update(){
            statuses.update(null);
        }
    }

    class Connection{
        NetConnection connection;
    }

    static <T extends Connectionc & Unitc> void doSomethingWithAConnection(T value){
        value.setX(0);
        value.setY(0);
        value.getVel().set(100, 100f);
        value.setDead(true);
        value.getConnection().kick("you are dead");
    }

    static void test(){
        doSomethingWithAConnection(new PlayerGen());
    }
}
