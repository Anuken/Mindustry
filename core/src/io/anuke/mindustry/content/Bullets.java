package io.anuke.mindustry.content;

import io.anuke.arc.entities.Effects;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.CapStyle;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shapes;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;

import static io.anuke.mindustry.Vars.world;

public class Bullets implements ContentList{
    public static BulletType

    //artillery
    artilleryDense, arilleryPlastic, artilleryPlasticFrag, artilleryHoming, artlleryIncendiary, artilleryExplosive, artilleryUnit,

    //flak
    flakPlastic, flakExplosive, flakSurge,

    //missiles
    missileExplosive, missileIncendiary, missileSurge, missileJavelin, missileSwarm,

    //standard
    standardCopper, standardDense, standardThorium, standardHoming, standardIncendiary, standardMechSmall,
            standardGlaive, standardDenseBig, standardThoriumBig, standardIncendiaryBig,

    //electric
    lancerLaser, meltdownLaser, lightning, arc, damageLightning,

    //liquid
    waterShot, cryoShot, slagShot, oilShot,

    //environment, misc.
    fireball, basicFlame, fuseShot, driverBolt, healBullet, frag,

    //bombs
    bombExplosive, bombIncendiary, bombOil;

    @Override
    public void load(){

        artilleryDense = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 50f;
            bulletWidth = bulletHeight = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
        }};

        artilleryPlasticFrag = new BasicBulletType(2.5f, 6, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            bulletShrink = 1f;
            lifetime = 15f;
            backColor = Palette.plastaniumBack;
            frontColor = Palette.plastaniumFront;
            despawnEffect = Fx.none;
        }};

        arilleryPlastic = new ArtilleryBulletType(3.3f, 0, "shell"){{
            hitEffect = Fx.plasticExplosion;
            knockback = 1f;
            lifetime = 55f;
            bulletWidth = bulletHeight = 13f;
            collidesTiles = false;
            splashDamageRadius = 35f;
            splashDamage = 35f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 9;
            backColor = Palette.plastaniumBack;
            frontColor = Palette.plastaniumFront;
        }};

        artilleryHoming = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 45f;
            bulletWidth = bulletHeight = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 33f;
            homingPower = 2f;
            homingRange = 50f;
        }};

        artlleryIncendiary = new ArtilleryBulletType(3f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 60f;
            bulletWidth = bulletHeight = 13f;
            collidesTiles = false;
            splashDamageRadius = 25f;
            splashDamage = 30f;
            incendAmount = 4;
            incendSpread = 11f;
            frontColor = Palette.lightishOrange;
            backColor = Palette.lightOrange;
            trailEffect = Fx.incendTrail;
        }};

        artilleryExplosive = new ArtilleryBulletType(2f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 70f;
            bulletWidth = bulletHeight = 14f;
            collidesTiles = false;
            splashDamageRadius = 45f;
            splashDamage = 50f;
            backColor = Palette.missileYellowBack;
            frontColor = Palette.missileYellow;
        }};

        artilleryUnit = new ArtilleryBulletType(2f, 0, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 90f;
            bulletWidth = bulletHeight = 14f;
            collides = true;
            collidesTiles = true;
            splashDamageRadius = 45f;
            splashDamage = 50f;
            backColor = Palette.bulletYellowBack;
            frontColor = Palette.bulletYellow;
        }};

        flakPlastic = new FlakBulletType(4f, 5){{
            splashDamageRadius = 40f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 4;
            hitEffect = Fx.plasticExplosion;
            frontColor = Palette.plastaniumFront;
            backColor = Palette.plastaniumBack;
        }};

        flakExplosive = new FlakBulletType(4f, 5){{
            //default bullet type, no changes
        }};

        flakSurge = new FlakBulletType(4f, 7){{
            splashDamage = 33f;
            lightining = 2;
            lightningLength = 12;
        }};

        missileExplosive = new MissileBulletType(1.8f, 10, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 30f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
        }};

        missileIncendiary = new MissileBulletType(2f, 12, "missile"){{
            frontColor = Palette.lightishOrange;
            backColor = Palette.lightOrange;
            bulletWidth = 7f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            homingPower = 7f;
            splashDamageRadius = 10f;
            splashDamage = 10f;
            lifetime = 160f;
            hitEffect = Fx.blastExplosion;
            incendSpread = 10f;
            incendAmount = 3;
        }};

        missileSurge = new MissileBulletType(3.5f, 15, "bullet"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.01f;
            splashDamageRadius = 30f;
            splashDamage = 22f;
            lifetime = 150f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            lightining = 2;
            lightningLength = 14;
        }};

        missileJavelin = new MissileBulletType(5f, 10.5f, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.003f;
            keepVelocity = false;
            splashDamageRadius = 20f;
            splashDamage = 1f;
            lifetime = 90f;
            trailColor = Color.valueOf("b6c6fd");
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            backColor = Palette.bulletYellowBack;
            frontColor = Palette.bulletYellow;
            weaveScale = 8f;
            weaveMag = 2f;
        }};

        missileSwarm = new MissileBulletType(2.7f, 12, "missile"){{
            bulletWidth = 8f;
            bulletHeight = 8f;
            bulletShrink = 0f;
            drag = -0.003f;
            homingRange = 60f;
            keepVelocity = false;
            splashDamageRadius = 25f;
            splashDamage = 10f;
            lifetime = 120f;
            trailColor = Color.GRAY;
            backColor = Palette.bulletYellowBack;
            frontColor = Palette.bulletYellow;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            weaveScale = 8f;
            weaveMag = 2f;
        }};

        standardCopper = new BasicBulletType(2.5f, 7, "bullet"){{
            bulletWidth = 7f;
            bulletHeight = 9f;
            shootEffect = Fx.shootSmall;
            smokeEffect = Fx.shootSmallSmoke;
            ammoMultiplier = 5;
        }};

        standardDense = new BasicBulletType(3.5f, 18, "bullet"){{
            bulletWidth = 9f;
            bulletHeight = 12f;
            armorPierce = 0.2f;
            reloadMultiplier = 0.6f;
            ammoMultiplier = 2;
        }};

        standardThorium = new BasicBulletType(4f, 29, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 13f;
            armorPierce = 0.5f;
            shootEffect = Fx.shootBig;
            smokeEffect = Fx.shootBigSmoke;
            ammoMultiplier = 2;
        }};

        standardHoming = new BasicBulletType(3f, 9, "bullet"){{
            bulletWidth = 7f;
            bulletHeight = 9f;
            homingPower = 5f;
            reloadMultiplier = 1.4f;
            ammoMultiplier = 5;
        }};

        standardIncendiary = new BasicBulletType(3.2f, 11, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            frontColor = Palette.lightishOrange;
            backColor = Palette.lightOrange;
            incendSpread = 3f;
            incendAmount = 1;
            incendChance = 0.3f;
            inaccuracy = 3f;
        }};

        standardGlaive = new BasicBulletType(4f, 7.5f, "bullet"){{
            bulletWidth = 10f;
            bulletHeight = 12f;
            frontColor = Color.valueOf("feb380");
            backColor = Color.valueOf("ea8878");
            incendSpread = 3f;
            incendAmount = 1;
            incendChance = 0.3f;
        }};

        standardMechSmall = new BasicBulletType(4f, 9, "bullet"){{
            bulletWidth = 11f;
            bulletHeight = 14f;
            lifetime = 40f;
            inaccuracy = 5f;
            despawnEffect = Fx.hitBulletSmall;
        }};

        standardDenseBig = new BasicBulletType(7f, 42, "bullet"){{
            bulletWidth = 15f;
            bulletHeight = 21f;
            armorPierce = 0.2f;
        }};

        standardThoriumBig = new BasicBulletType(8f, 65, "bullet"){{
            bulletWidth = 16f;
            bulletHeight = 23f;
            armorPierce = 0.5f;
        }};

        standardIncendiaryBig = new BasicBulletType(7f, 38, "bullet"){{
            bulletWidth = 16f;
            bulletHeight = 21f;
            frontColor = Palette.lightishOrange;
            backColor = Palette.lightOrange;
            incendSpread = 3f;
            incendAmount = 2;
            incendChance = 0.3f;
        }};

        damageLightning = new BulletType(0.0001f, 0f){
            {
                lifetime = Lightning.lifetime;
                hitEffect = Fx.hitLancer;
                despawnEffect = Fx.none;
                status = StatusEffects.shocked;
                statusIntensity = 1f;
            }
        };

        healBullet = new BulletType(5.2f, 13){
            float healPercent = 3f;

            {
                hitEffect = Fx.hitLaser;
                despawnEffect = Fx.hitLaser;
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
                Lines.lineAngleCenter(b.x, b.y, b.rot(), 7f);
                Draw.color(Color.WHITE);
                Lines.lineAngleCenter(b.x, b.y, b.rot(), 3f);
                Draw.reset();
            }

            @Override
            public void hitTile(Bullet b, Tile tile){
                super.hit(b);
                tile = tile.target();

                if(tile != null && tile.getTeam() == b.getTeam() && !(tile.block() instanceof BuildBlock)){
                    Effects.effect(Fx.healBlockFull, Palette.heal, tile.drawx(), tile.drawy(), tile.block().size);
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
                hitEffect = despawnEffect = Fx.none;
            }

            @Override
            public void init(Bullet b){
                b.velocity().setLength(0.6f + Mathf.random(2f));
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
                if(Mathf.chance(0.04 * Time.delta())){
                    Tile tile = world.tileWorld(b.x, b.y);
                    if(tile != null){
                        Fire.create(tile);
                    }
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Effects.effect(Fx.fireballsmoke, b.x, b.y);
                }

                if(Mathf.chance(0.1 * Time.delta())){
                    Effects.effect(Fx.ballfire, b.x, b.y);
                }
            }
        };

        basicFlame = new BulletType(2.3f, 5){
            {
                hitSize = 7f;
                lifetime = 35f;
                pierce = true;
                drag = 0.05f;
                hitEffect = Fx.hitFlameSmall;
                despawnEffect = Fx.none;
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
                hitEffect = Fx.hitLancer;
                despawnEffect = Fx.none;
                hitSize = 4;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void init(Bullet b){
                Damage.collideLine(b, b.getTeam(), hitEffect, b.x, b.y, b.rot(), length);
            }

            @Override
            public void draw(Bullet b){
                float f = Mathf.curve(b.fin(), 0f, 0.2f);
                float baseLen = length * f;

                Lines.lineAngle(b.x, b.y, b.rot(), baseLen);
                for(int s = 0; s < 3; s++){
                    Draw.color(colors[s]);
                    for(int i = 0; i < tscales.length; i++){
                        Lines.stroke(7f * b.fout() * (s == 0 ? 1.5f : s == 1 ? 1f : 0.3f) * tscales[i]);
                        Lines.lineAngle(b.x, b.y, b.rot(), baseLen * lenscales[i]);
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
                hitEffect = Fx.hitMeltdown;
                despawnEffect = Fx.none;
                hitSize = 4;
                drawSize = 420f;
                lifetime = 16f;
                pierce = true;
            }

            @Override
            public void update(Bullet b){
                if(b.timer.get(1, 5f)){
                    Damage.collideLine(b, b.getTeam(), hitEffect, b.x, b.y, b.rot(), length);
                }
                Effects.shake(1f, 1f, b.x, b.y);
            }

            @Override
            public void hit(Bullet b, float hitx, float hity){
                Effects.effect(hitEffect, colors[2], hitx, hity);
                if(Mathf.chance(0.4)){
                    Fire.create(world.tileWorld(hitx+Mathf.range(5f), hity+Mathf.range(5f)));
                }
            }

            @Override
            public void draw(Bullet b){
                float baseLen = (length) * b.fout();

                Lines.lineAngle(b.x, b.y, b.rot(), baseLen);
                for(int s = 0; s < colors.length; s++){
                    Draw.color(tmpColor.set(colors[s]).mul(1f + Mathf.absin(Time.time(), 1f, 0.1f)));
                    for(int i = 0; i < tscales.length; i++){
                        Tmp.v1.trns(b.rot() + 180f, (lenscales[i] - 1f) * 35f);
                        Lines.stroke((9f + Mathf.absin(Time.time(), 0.8f, 1.5f)) * b.fout() * strokes[s] * tscales[i]);
                        Lines.lineAngle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, b.rot(), baseLen * lenscales[i], CapStyle.none);
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
                hitEffect = Fx.hitFuse;
                lifetime = 13f;
                despawnEffect = Fx.none;
                pierce = true;
            }

            @Override
            public void init(Bullet b) {
                for (int i = 0; i < rays; i++) {
                    Damage.collideLine(b, b.getTeam(), hitEffect, b.x, b.y, b.rot(), rayLength - Math.abs(i - (rays/2))*20f);
                }
            }

            @Override
            public void draw(Bullet b) {
                super.draw(b);
                Draw.color(Color.WHITE, Palette.surge, b.fin());
                for(int i = 0; i < 7; i++){
                    Tmp.v1.trns(b.rot(), i * 8f);
                    float sl = Mathf.clamp(b.fout()-0.5f) * (80f - i *10);
                    Shapes.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, 4f, sl, b.rot() + 90);
                    Shapes.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, 4f, sl, b.rot() - 90);
                }
                Shapes.tri(b.x, b.y, 13f, (rayLength+50) * b.fout(), b.rot());
                Shapes.tri(b.x, b.y, 13f, 10f * b.fout(), b.rot() + 180f);
                Draw.reset();
            }
        };

        waterShot = new LiquidBulletType(Liquids.water){{
            knockback = 0.65f;
        }};

        cryoShot = new LiquidBulletType(Liquids.cryofluid){{

        }};

        slagShot = new LiquidBulletType(Liquids.slag){{
            damage = 4;
            speed = 1.9f;
            drag = 0.03f;
        }};

        oilShot = new LiquidBulletType(Liquids.oil){{
            speed = 2f;
            drag = 0.03f;
        }};

        lightning = new BulletType(0.001f, 12f){
            {
                lifetime = 1f;
                despawnEffect = Fx.none;
                hitEffect = Fx.hitLancer;
                keepVelocity = false;
            }

            @Override
            public void draw(Bullet b){
            }

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), Palette.lancerLaser, damage, b.x, b.y, b.rot(), 30);
            }
        };

        arc = new BulletType(0.001f, 26){{
                lifetime = 1;
                despawnEffect = Fx.none;
                hitEffect = Fx.hitLancer;
            }

            @Override
            public void draw(Bullet b){}

            @Override
            public void init(Bullet b){
                Lightning.create(b.getTeam(), Palette.lancerLaser, damage, b.x, b.y, b.rot(), 36);
            }
        };

        driverBolt = new MassDriverBolt();

        frag = new BasicBulletType(5f, 8, "bullet"){{
            bulletWidth = 8f;
            bulletHeight = 9f;
            bulletShrink = 0.5f;
            lifetime = 50f;
            drag = 0.04f;
        }};

        bombExplosive = new BombBulletType(10f, 20f, "shell"){{
            bulletWidth = 9f;
            bulletHeight = 13f;
            hitEffect = Fx.flakExplosion;
        }};

        bombIncendiary = new BombBulletType(7f, 10f, "shell"){{
            bulletWidth = 8f;
            bulletHeight = 12f;
            hitEffect = Fx.flakExplosion;
            backColor = Palette.lightOrange;
            frontColor = Palette.lightishOrange;
            incendChance = 1f;
            incendAmount = 3;
            incendSpread = 10f;
        }};

        bombOil = new BombBulletType(2f, 3f, "shell"){{
                bulletWidth = 8f;
                bulletHeight = 12f;
                hitEffect = Fx.pulverize;
                backColor = new Color(0x4f4f4fff);
                frontColor = Color.GRAY;
            }

            @Override
            public void hit(Bullet b, float x, float y){
                super.hit(b, x, y);

                for(int i = 0; i < 3; i++){
                    Tile tile = world.tileWorld(x + Mathf.range(8f), y + Mathf.range(8f));
                    Puddle.deposit(tile, Liquids.oil, 5f);
                }
            }
        };
    }
}
