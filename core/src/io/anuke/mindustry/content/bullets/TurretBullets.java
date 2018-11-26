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
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.distribution.MassDriver.DriverBulletData;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.*;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

public class TurretBullets extends BulletList implements ContentList{
    public static BulletType fireball, basicFlame, lancerLaser, burstLaser, meltdownLaser,
        fuseShot, waterShot, cryoShot, lavaShot, oilShot, lightning, driverBolt, healBullet, arc, damageLightning;

    @Override
    public void load(){

        damageLightning = new BulletType(0.0001f, 0f){
            {
                lifetime = Lightning.lifetime;
                hiteffect = BulletFx.hitLancer;
                despawneffect = Fx.none;
            }
        };

        healBullet = new BulletType(5.2f, 13){
            float healPercent = 3f;

            {
                hiteffect = BulletFx.hitLaser;
                despawneffect = BulletFx.hitLaser;
                collidesTeam = true;
            }

            @Override
            public boolean collides(Bullet b, Tile tile){
                return tile.getTeam() != b.getTeam() || tile.entity.healthf() < 1f;
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
                tile = tile.target();

                if(tile.getTeam() == b.getTeam() && !(tile.block() instanceof BuildBlock)){
                    Effects.effect(BlockFx.healBlockFull, Palette.heal, tile.drawx(), tile.drawy(), tile.block().size);
                    tile.entity.healBy(healPercent / 100f * tile.entity.maxHealth());
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
                hiteffect = despawneffect = Fx.none;
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

        lancerLaser = new BulletType(0.001f, 140){
            Color[] colors = {Palette.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Palette.lancerLaser, Color.WHITE};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] lenscales = {1f, 1.1f, 1.13f, 1.14f};
            float length = 100f;

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

        meltdownLaser = new BulletType(0.001f, 26){
            Color tmpColor = new Color();
            Color[] colors = {Color.valueOf("ec745855"), Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.WHITE};
            float[] tscales = {1f, 0.7f, 0.5f, 0.2f};
            float[] strokes = {2f, 1.5f, 1f, 0.3f};
            float[] lenscales = {1f, 1.12f, 1.15f, 1.17f};
            float length = 200f;

            {
                hiteffect = BulletFx.hitMeltdown;
                despawneffect = Fx.none;
                hitsize = 4;
                drawSize = 420f;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void update(Bullet b){
                if(b.timer.get(1, 5f)){
                    Damage.collideLine(b, b.getTeam(), hiteffect, b.x, b.y, b.angle(), length);
                }
                Effects.shake(1f, 1f, b.x, b.y);
            }

            @Override
            public void hit(Bullet b, float hitx, float hity){
                Effects.effect(hiteffect, colors[2], hitx, hity);
                if(Mathf.chance(0.4)){
                    Fire.create(world.tileWorld(hitx+Mathf.range(5f), hity+Mathf.range(5f)));
                }
            }

            @Override
            public void draw(Bullet b){
                float baseLen = (length) * b.fout();

                Lines.lineAngle(b.x, b.y, b.angle(), baseLen);
                for(int s = 0; s < colors.length; s++){
                    Draw.color(tmpColor.set(colors[s]).mul(1f + Mathf.absin(Timers.time(), 1f, 0.1f)));
                    for(int i = 0; i < tscales.length; i++){
                        vector.trns(b.angle() + 180f, (lenscales[i] - 1f) * 35f);
                        Lines.stroke((9f + Mathf.absin(Timers.time(), 0.8f, 1.5f)) * b.fout() * strokes[s] * tscales[i]);
                        Lines.lineAngle(b.x + vector.x, b.y + vector.y, b.angle(), baseLen * lenscales[i], CapStyle.none);
                    }
                }
                Draw.reset();
            }
        };

        fuseShot = new BulletType(0.01f, 70){
            int rays = 3;
            float raySpace = 2f;
            float rayLength = 80f;
            {
                hiteffect = BulletFx.hitFuse;
                lifetime = 13f;
                despawneffect = Fx.none;
                pierce = true;
            }

            @Override
            public void init(Bullet b) {
                for (int i = 0; i < rays; i++) {
                    float offset = (i-rays/2)*raySpace;
                    vector.trns(b.angle(), 0.01f, offset);
                    Damage.collideLine(b, b.getTeam(), hiteffect, b.x, b.y, b.angle(), rayLength - Math.abs(i - (rays/2))*20f);
                }
            }

            @Override
            public void draw(Bullet b) {
                super.draw(b);
                Draw.color(Color.WHITE, Palette.surge, b.fin());
                for(int i = 0; i < 7; i++){
                    vector.trns(b.angle(), i * 8f);
                    float sl = Mathf.clamp(b.fout()-0.5f) * (80f - i *10);
                    Shapes.tri(b.x + vector.x, b.y + vector.y, 4f, sl, b.angle() + 90);
                    Shapes.tri(b.x + vector.x, b.y + vector.y, 4f, sl, b.angle() - 90);
                }
                Shapes.tri(b.x, b.y, 13f, (rayLength+50) * b.fout(), b.angle());
                Shapes.tri(b.x, b.y, 13f, 10f * b.fout(), b.angle() + 180f);
                Draw.reset();
            }

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

        lightning = new BulletType(0.001f, 12f){
            {
                lifetime = 1f;
                despawneffect = Fx.none;
                hiteffect = BulletFx.hitLancer;
                keepVelocity = false;
            }

            @Override
            public void draw(Bullet b){
            }

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), Palette.lancerLaser, damage, b.x, b.y, b.angle(), 30);
            }
        };

        arc = new BulletType(0.001f, 26){
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
                Lightning.create(b.getTeam(), Palette.lancerLaser, damage, b.x, b.y, b.angle(), 36);
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
                        Effects.effect(EnvironmentFx.dropItem, Color.WHITE, b.x, b.y, angle, content.item(i));
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
