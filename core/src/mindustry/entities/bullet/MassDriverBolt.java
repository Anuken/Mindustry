package mindustry.entities.bullet;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.world.blocks.distribution.MassDriver.DriverBulletData;

import static mindustry.Vars.content;

public class MassDriverBolt extends BulletType{

    public MassDriverBolt(){
        super(1f, 50);
        collidesTiles = false;
        lifetime = 1f;
        despawnEffect = Fx.smeltsmoke;
        hitEffect = Fx.hitBulletBig;
        drag = 0f;
    }

    @Override
    public void draw(Bulletc b){
        float w = 11f, h = 13f;

        Draw.color(Pal.bulletYellowBack);
        Draw.rect("shell-back", b.x(), b.y(), w, h, b.rotation() + 90);

        Draw.color(Pal.bulletYellow);
        Draw.rect("shell", b.x(), b.y(), w, h, b.rotation() + 90);

        Draw.reset();
    }

    @Override
    public void update(Bulletc b){
        //data MUST be an instance of DriverBulletData
        if(!(b.data() instanceof DriverBulletData)){
            hit(b);
            return;
        }

        float hitDst = 7f;

        DriverBulletData data = (DriverBulletData)b.data();

        float baseDst = data.from.dst(data.to);
        float dst1 = b.dst(data.from);
        float dst2 = b.dst(data.to);
        
        //if the target is dead, unlogically oof itself
        if(data.to.dead()){
            remove();
        }

        boolean intersect = false;

        //bullet has gone past the destination point: but did it intersect it?
        if(dst1 > baseDst){
            float angleTo = b.angleTo(data.to);
            float baseAngle = data.to.angleTo(data.from);

            //if angles are nearby, then yes, it did
            if(Angles.near(angleTo, baseAngle, 2f)){
                intersect = true;
                //snap bullet position back; this is used for low-FPS situations
                b.set(data.to.x() + Angles.trnsx(baseAngle, hitDst), data.to.y() + Angles.trnsy(baseAngle, hitDst));
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
    public void despawned(Bulletc b){
        super.despawned(b);

        if(!(b.data() instanceof DriverBulletData)) return;

        DriverBulletData data = (DriverBulletData)b.data();

        for(int i = 0; i < data.items.length; i++){
            int amountDropped = Mathf.random(0, data.items[i]);
            if(amountDropped > 0){
                float angle = b.rotation() + Mathf.range(100f);
                Fx.dropItem.at(b.x(), b.y(), angle, Color.white, content.item(i));
            }
        }
    }

    @Override
    public void hit(Bulletc b, float hitx, float hity){
        super.hit(b, hitx, hity);
        despawned(b);
    }
}
