package mindustry.entities.def;

import arc.math.geom.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.net.*;

class EntityDefs{

    @EntityDef({UnitComp.class, ConnectionComp.class})
    class PlayerDef{}

    @EntityDef({BulletComp.class, VelComp.class})
    class BulletDef{}

    @Depends({HealthComp.class, VelComp.class, StatusComp.class})
    class UnitComp{

    }

    class HealthComp{
        float health, maxHealth;
        boolean dead;

        float healthf(){
            return health / maxHealth;
        }
    }

    abstract class PosComp implements Position{
        float x, y;

        void set(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    @Depends(PosComp.class)
    class VelComp{
        //transient fields act as imports from any other component clases; these are ignored by the generator
        transient float x, y;

        final Vec2 vel = new Vec2();

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(0.9f);
        }
    }

    @Depends(PosComp.class)
    class HitboxComp{
        transient float x, y;

        float hitSize;

        boolean collides(Hitboxc other){
            return Intersector.overlapsRect(x - hitSize/2f, y - hitSize/2f, hitSize, hitSize,
                other.getX() - other.getHitSize()/2f, other.getY() - other.getHitSize()/2f, other.getHitSize(), other.getHitSize());
        }
    }

    class StatusComp{
        final Statuses statuses = new Statuses();

        void update(){
            statuses.update(null);
        }
    }

    class ConnectionComp{
        NetConnection connection;
    }

    class BulletComp{
        BulletType bullet;

        void init(){
            bullet.init();
        }
    }

    @BaseComponent
    class EntityComp{
        int id;

        void init(){}

        <T> T as(Class<T> type){
            return (T)this;
        }
    }

    static void testing(){
        Entityc abullet = new BulletGen();
        Entityc aplayer = new PlayerGen();
    }
}
