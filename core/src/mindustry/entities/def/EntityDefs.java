package mindustry.entities.def;

import arc.math.geom.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
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

    static <T extends Connectionc & Velc & Healthc & Posc> void doSomethingWithAConnection(T value){
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
