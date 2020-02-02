package mindustry.entities.def;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.net.*;

class EntityDefs{

    @EntityDef({Unitc.class, Connectionc.class})
    class PlayerDef{}

    @EntityDef({Bulletc.class, Velc.class})
    class BulletDef{}

    @Depends({Healthc.class, Velc.class, Statusc.class})
    class Unitc{

    }

    class Healthc{
        float health, maxHealth;
        boolean dead;

        float healthf(){
            return health / maxHealth;
        }
    }

    abstract class Posc implements Position{
        float x, y;

        void set(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    @Depends(Posc.class)
    class Velc{
        //transient fields act as imports from any other component clases; these are ignored by the generator
        transient float x, y;

        final Vec2 vel = new Vec2();

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(0.9f);
        }
    }

    class Statusc{
        final Statuses statuses = new Statuses();

        void update(){
            statuses.update(null);
        }
    }

    class Connectionc{
        NetConnection connection;
    }

    class Bulletc{
        BulletType bullet;

        void init(){
            bullet.init();
        }
    }

    @BaseComponent
    class Entityc{
        int id;

        void init(){}

        <T> T as(Class<T> type){
            return (T)this;
        }
    }

    static void testing(){
        Entityt abullet = new BulletGen();
        Entityt aplayer = new PlayerGen();

        if(abullet instanceof Post){
            Log.info("Pos: " + abullet.as(Post.class).getX());
        }

        Log.info(abullet.as(Post.class).dst(aplayer.as(Post.class)));
    }
}
