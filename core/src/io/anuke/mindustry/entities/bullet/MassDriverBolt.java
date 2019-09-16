package io.anuke.mindustry.entities.bullet;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.world.blocks.distribution.MassDriver.DriverBulletData;

import static io.anuke.mindustry.Vars.content;

public class MassDriverBolt extends BulletType{

    public MassDriverBolt(){
        super(5.3f, 50);
        collidesTiles = false;
        lifetime = 200f;
        despawnEffect = Fx.smeltsmoke;
        hitEffect = Fx.hitBulletBig;
        drag = 0.005f;
    }

    @Override
    public void draw(io.anuke.mindustry.entities.type.Bullet b){
        float w = 11f, h = 13f;

        Draw.color(Pal.bulletYellowBack);
        Draw.rect("shell-back", b.x, b.y, w, h, b.rot() + 90);

        Draw.color(Pal.bulletYellow);
        Draw.rect("shell", b.x, b.y, w, h, b.rot() + 90);

        Draw.reset();
    }

    @Override
    public void update(io.anuke.mindustry.entities.type.Bullet b){
        //data MUST be an instance of DriverBulletData
        if(!(b.getData() instanceof DriverBulletData)){
            hit(b);
            return;
        }

        float hitDst = 7f;

        DriverBulletData data = (DriverBulletData)b.getData();

        //if the target is dead, just keep flying until the bullet explodes
        if(data.to.isDead()){
            return;
        }

        float baseDst = data.from.dst(data.to);
        float dst1 = b.dst(data.from);
        float dst2 = b.dst(data.to);

        boolean intersect = false;

        //bullet has gone past the destination point: but did it intersect it?
        if(dst1 > baseDst){
            float angleTo = b.angleTo(data.to);
            float baseAngle = data.to.angleTo(data.from);

            //if angles are nearby, then yes, it did
            if(Angles.near(angleTo, baseAngle, 2f)){
                intersect = true;
                //snap bullet position back; this is used for low-FPS situations
                b.set(data.to.x + Angles.trnsx(baseAngle, hitDst), data.to.y + Angles.trnsy(baseAngle, hitDst));
            }
        }

        //if on course and it's in range of the target
        if(Math.abs(dst1 + dst2 - baseDst) < 4f && dst2 <= hitDst){
            intersect = true;
        } //else, bullet has gone off course, does not get recieved.

        if(intersect){
            data.to.handlePayload(b, data);
        }
    }

    @Override
    public void despawned(io.anuke.mindustry.entities.type.Bullet b){
        super.despawned(b);

        if(!(b.getData() instanceof DriverBulletData)) return;

        DriverBulletData data = (DriverBulletData)b.getData();

        for(int i = 0; i < data.items.length; i++){
            int amountDropped = Mathf.random(0, data.items[i]);
            if(amountDropped > 0){
                float angle = b.rot() + Mathf.range(100f);
                Effects.effect(Fx.dropItem, Color.white, b.x, b.y, angle, content.item(i));
            }
        }
    }

    @Override
    public void hit(Bullet b, float hitx, float hity){
        super.hit(b, hitx, hity);
        despawned(b);
    }
}
