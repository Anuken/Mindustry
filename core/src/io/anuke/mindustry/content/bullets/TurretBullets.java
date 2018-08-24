package io.anuke.mindustry.content.bullets;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.bullet.LiquidBulletType;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.distribution.MassDriver.DriverBulletData;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class TurretBullets extends BulletList implements ContentList{
    public static BulletType fireball, basicFlame, lancerLaser, fuseShot, waterShot, cryoShot, lavaShot, oilShot, lightning, driverBolt, healBullet;

    @Override
    public void load(){

        healBullet = new BulletType(5.2f, 19){
            float healAmount = 21f;

            {
                hiteffect = BulletFx.hitLaser;
                despawneffect = BulletFx.hitLaser;
                collidesTeam = true;
            }

            @Override
            public void draw(Bullet b){
                Draw.color(Palette.heal);
                Lines.stroke(2f);
                Lines.lineAngleCenter(b.x, b.y, b.angle(), 7f);
                Draw.color(Color.WHITE);
                Lines.lineAngleCenter(b.x, b.y, b.angle(), 3f);
                Draw.reset();
            }

            @Override
            public void hitTile(Bullet b, Tile tile){
                super.hit(b);

                if(tile.getTeam() == b.getTeam()){
                    Effects.effect(BlockFx.healBlock, tile.drawx(), tile.drawy(), tile.block().size);
                    tile.entity.health += healAmount;
                    tile.entity.health = Mathf.clamp(tile.entity.health, 0, tile.block().health);
                }
            }
        };

        fireball = new BulletType(1f, 4){
            {
                pierce = true;
                hitTiles = false;
                collides = false;
                collidesTiles = false;
                drag = 0.03f;
            }

            @Override
            public void init(Bullet b){
                b.getVelocity().setLength(0.6f + Mathf.random(2f));
            }

            @Override
            public void draw(Bullet b){
                //TODO add color to the bullet depending on the color of the flame it came from
                Draw.color(Palette.lightFlame, Palette.darkFlame, Color.GRAY, b.fin());
                Fill.circle(b.x, b.y, 3f * b.fout());
                Draw.reset();
            }

            @Override
            public void update(Bullet b){
                if(Mathf.chance(0.04 * Timers.delta())){
                    Tile tile = world.tileWorld(b.x, b.y);
                    if(tile != null){
                        Fire.create(tile);
                    }
                }

                if(Mathf.chance(0.1 * Timers.delta())){
                    Effects.effect(EnvironmentFx.fireballsmoke, b.x, b.y);
                }

                if(Mathf.chance(0.1 * Timers.delta())){
                    Effects.effect(EnvironmentFx.ballfire, b.x, b.y);
                }
            }
        };

        basicFlame = new BulletType(2.3f, 5){
            {
                hitsize = 7f;
                lifetime = 35f;
                pierce = true;
                drag = 0.05f;
                hiteffect = BulletFx.hitFlameSmall;
                despawneffect = Fx.none;
                status = StatusEffects.burning;
            }

            @Override
            public void draw(Bullet b){
            }
        };

        lancerLaser = new BulletType(0.001f, 110){
            Color[] colors = {Palette.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Palette.lancerLaser, Color.WHITE};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] lenscales = {1f, 1.1f, 1.13f, 1.14f};
            float length = 90f;

            {
                hiteffect = BulletFx.hitLancer;
                despawneffect = Fx.none;
                hitsize = 4;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void init(Bullet b){
                Damage.collideLine(b, b.getTeam(), hiteffect, b.x, b.y, b.angle(), length);
            }

            @Override
            public void draw(Bullet b){
                float f = Mathf.curve(b.fin(), 0f, 0.2f);
                float baseLen = length * f;

                Lines.lineAngle(b.x, b.y, b.angle(), baseLen);
                for(int s = 0; s < 3; s++){
                    Draw.color(colors[s]);
                    for(int i = 0; i < tscales.length; i++){
                        Lines.stroke(7f * b.fout() * (s == 0 ? 1.5f : s == 1 ? 1f : 0.3f) * tscales[i]);
                        Lines.lineAngle(b.x, b.y, b.angle(), baseLen * lenscales[i]);
                    }
                }
                Draw.reset();
            }
        };

        fuseShot = new BulletType(0.01f, 100){
            //TODO
        };

        waterShot = new LiquidBulletType(Liquids.water){
            {
                status = StatusEffects.wet;
                statusIntensity = 0.5f;
                knockback = 0.65f;
            }
        };
        cryoShot = new LiquidBulletType(Liquids.cryofluid){
            {
                status = StatusEffects.freezing;
                statusIntensity = 0.5f;
            }
        };
        lavaShot = new LiquidBulletType(Liquids.lava){
            {
                damage = 4;
                speed = 1.9f;
                drag = 0.03f;
                status = StatusEffects.melting;
                statusIntensity = 0.5f;
            }
        };
        oilShot = new LiquidBulletType(Liquids.oil){
            {
                speed = 2f;
                drag = 0.03f;
                status = StatusEffects.tarred;
                statusIntensity = 0.5f;
            }
        };
        lightning = new BulletType(0.001f, 10){
            {
                lifetime = 1;
                despawneffect = Fx.none;
                hiteffect = BulletFx.hitLancer;
            }

            @Override
            public void draw(Bullet b){
            }

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), hiteffect, Palette.lancerLaser, damage, b.x, b.y, b.angle(), 30);
            }
        };

        driverBolt = new BulletType(5.3f, 50){
            {
                collidesTiles = false;
                lifetime = 200f;
                despawneffect = BlockFx.smeltsmoke;
                hiteffect = BulletFx.hitBulletBig;
                drag = 0.01f;
            }

            @Override
            public void draw(Bullet b){
                float w = 11f, h = 13f;

                Draw.color(Palette.bulletYellowBack);
                Draw.rect("shell-back", b.x, b.y, w, h, b.angle() + 90);

                Draw.color(Palette.bulletYellow);
                Draw.rect("shell", b.x, b.y, w, h, b.angle() + 90);

                Draw.reset();
            }

            @Override
            public void update(Bullet b){
                //data MUST be an instance of DriverBulletData
                if(!(b.getData() instanceof DriverBulletData)){
                    hit(b);
                    return;
                }

                float hitDst = 7f;

                DriverBulletData data = (DriverBulletData) b.getData();

                //if the target is dead, just keep flying until the bullet explodes
                if(data.to.isDead()){
                    return;
                }

                float baseDst = data.from.distanceTo(data.to);
                float dst1 = b.distanceTo(data.from);
                float dst2 = b.distanceTo(data.to);

                boolean intersect = false;

                //bullet has gone past the destination point: but did it intersect it?
                if(dst1 > baseDst){
                    float angleTo = b.angleTo(data.to);
                    float baseAngle = data.to.angleTo(data.from);

                    //if angles are nearby, then yes, it did
                    if(Mathf.angNear(angleTo, baseAngle, 2f)){
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
            public void despawned(Bullet b){
                super.despawned(b);

                if(!(b.getData() instanceof DriverBulletData)) return;

                DriverBulletData data = (DriverBulletData) b.getData();
                data.to.isRecieving = false;

                for(int i = 0; i < data.items.length; i++){
                    int amountDropped = Mathf.random(0, data.items[i]);
                    if(amountDropped > 0){
                        float angle = b.angle() + Mathf.range(100f);
                        float vs = Mathf.random(0f, 4f);
                        ItemDrop.create(Item.getByID(i), amountDropped, b.x, b.y, Angles.trnsx(angle, vs), Angles.trnsy(angle, vs));
                    }
                }
            }

            @Override
            public void hit(Bullet b, float hitx, float hity){
                super.hit(b, hitx, hity);
                despawned(b);
            }
        };
    }
}
