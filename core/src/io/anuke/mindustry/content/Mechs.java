package io.anuke.mindustry.content;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;

public class Mechs implements ContentList{
    public static Mech alpha, delta, tau, omega, dart, javelin, trident, glaive;

    public static Mech starter;

    @Override
    public void load(){

        alpha = new Mech("alpha-mech", false){
            {
                drillPower = 1;
                mineSpeed = 1.5f;
                mass = 1.2f;
                speed = 0.5f;
                itemCapacity = 40;
                boostSpeed = 0.95f;
                buildPower = 1.2f;
                engineColor = Color.valueOf("ffd37f");
                health = 250f;

                weapon = new Weapon("blaster"){{
                    length = 1.5f;
                    reload = 14f;
                    roundrobin = true;
                    ejectEffect = Fx.shellEjectSmall;
                    bullet = Bullets.standardMechSmall;
                }};
            }

            @Override
            public void updateAlt(Player player){
                player.healBy(Time.delta() * 0.09f);
            }

        };

        delta = new Mech("delta-mech", false){
            float cooldown = 120;

            {
                drillPower = -1;
                speed = 0.75f;
                boostSpeed = 0.95f;
                itemCapacity = 15;
                mass = 0.9f;
                health = 150f;
                buildPower = 0.9f;
                weaponOffsetX = -1;
                weaponOffsetY = -1;
                engineColor = Color.valueOf("d3ddff");

                weapon = new Weapon("shockgun"){{
                    shake = 2f;
                    length = 1f;
                    reload = 55f;
                    shotDelay = 3f;
                    roundrobin = true;
                    shots = 2;
                    inaccuracy = 0f;
                    ejectEffect = Fx.none;
                    bullet = Bullets.lightning;
                    shootSound = Sounds.spark;
                }};
            }

            @Override
            public void onLand(Player player){
                if(player.timer.get(Player.timerAbility, cooldown)){
                    Effects.shake(1f, 1f, player);
                    Effects.effect(Fx.landShock, player);
                    for(int i = 0; i < 8; i++){
                        Time.run(Mathf.random(8f), () -> Lightning.create(player.getTeam(), Pal.lancerLaser, 17f * Vars.state.rules.playerDamageMultiplier, player.x, player.y, Mathf.random(360f), 14));
                    }
                }
            }
        };

        tau = new Mech("tau-mech", false){
            float healRange = 60f;
            float healAmount = 10f;
            float healReload = 160f;
            boolean wasHealed;

            {
                drillPower = 4;
                mineSpeed = 3f;
                itemCapacity = 70;
                weaponOffsetY = -1;
                weaponOffsetX = 1;
                mass = 1.75f;
                speed = 0.44f;
                drag = 0.35f;
                boostSpeed = 0.8f;
                canHeal = true;
                health = 200f;
                buildPower = 1.6f;
                engineColor = Pal.heal;

                weapon = new Weapon("heal-blaster"){{
                    length = 1.5f;
                    reload = 24f;
                    roundrobin = false;
                    ejectEffect = Fx.none;
                    recoil = 2f;
                    bullet = Bullets.healBullet;
                    shootSound = Sounds.pew;
                }};
            }

            @Override
            public void updateAlt(Player player){

                if(player.timer.get(Player.timerAbility, healReload)){
                    wasHealed = false;

                    Units.nearby(player.getTeam(), player.x, player.y, healRange, unit -> {
                        if(unit.health < unit.maxHealth()){
                            Effects.effect(Fx.heal, unit);
                            wasHealed = true;
                        }
                        unit.healBy(healAmount);
                    });

                    if(wasHealed){
                        Effects.effect(Fx.healWave, player);
                    }
                }
            }
        };

        omega = new Mech("omega-mech", false){
            protected TextureRegion armorRegion;

            {
                drillPower = 2;
                mineSpeed = 1.5f;
                itemCapacity = 80;
                speed = 0.36f;
                boostSpeed = 0.6f;
                mass = 4f;
                shake = 4f;
                weaponOffsetX = 1;
                weaponOffsetY = 0;
                engineColor = Color.valueOf("feb380");
                health = 350f;
                buildPower = 1.5f;
                weapon = new Weapon("swarmer"){{
                    length = 1.5f;
                    recoil = 4f;
                    reload = 38f;
                    shots = 4;
                    spacing = 8f;
                    inaccuracy = 8f;
                    roundrobin = true;
                    ejectEffect = Fx.none;
                    shake = 3f;
                    bullet = Bullets.missileSwarm;
                    shootSound = Sounds.shootBig;
                }};
            }

            @Override
            public float getRotationAlpha(Player player){
                return 0.6f - player.shootHeat * 0.3f;
            }

            @Override
            public float spreadX(Player player){
                return player.shootHeat * 2f;
            }

            @Override
            public void load(){
                super.load();
                armorRegion = Core.atlas.find(name + "-armor");
            }

            @Override
            public void updateAlt(Player player){
                float scl = 1f - player.shootHeat / 2f;
                player.velocity().scl(scl);
            }

            @Override
            public float getExtraArmor(Player player){
                return player.shootHeat * 30f;
            }

            @Override
            public void draw(Player player){
                if(player.shootHeat <= 0.01f) return;

                Shaders.build.progress = player.shootHeat;
                Shaders.build.region = armorRegion;
                Shaders.build.time = Time.time() / 10f;
                Shaders.build.color.set(Pal.accent).a = player.shootHeat;
                Draw.shader(Shaders.build);
                Draw.rect(armorRegion, player.x, player.y, player.rotation);
                Draw.shader();
            }
        };

        dart = new Mech("dart-ship", true){
            {
                drillPower = 1;
                mineSpeed = 3f;
                speed = 0.5f;
                drag = 0.09f;
                health = 200f;
                weaponOffsetX = -1;
                weaponOffsetY = -1;
                engineColor = Pal.lightTrail;
                cellTrnsY = 1f;
                buildPower = 1.1f;
                weapon = new Weapon("blaster"){{
                    length = 1.5f;
                    reload = 15f;
                    roundrobin = true;
                    ejectEffect = Fx.shellEjectSmall;
                    bullet = Bullets.standardCopper;
                }};
            }

            @Override
            public boolean alwaysUnlocked(){
                return true;
            }
        };

        javelin = new Mech("javelin-ship", true){
            float minV = 3.6f;
            float maxV = 6f;
            TextureRegion shield;

            {
                drillPower = -1;
                speed = 0.11f;
                drag = 0.01f;
                mass = 2f;
                health = 170f;
                engineColor = Color.valueOf("d3ddff");
                cellTrnsY = 1f;
                weapon = new Weapon("missiles"){{
                    length = 1.5f;
                    reload = 70f;
                    shots = 4;
                    inaccuracy = 2f;
                    roundrobin = true;
                    ejectEffect = Fx.none;
                    velocityRnd = 0.2f;
                    spacing = 1f;
                    bullet = Bullets.missileJavelin;
                    shootSound = Sounds.missile;
                }};
            }

            @Override
            public void load(){
                super.load();
                shield = Core.atlas.find(name + "-shield");
            }

            @Override
            public float getRotationAlpha(Player player){
                return 0.5f;
            }

            @Override
            public void updateAlt(Player player){
                float scl = scld(player);
                if(Mathf.chance(Time.delta() * (0.15 * scl))){
                    Effects.effect(Fx.hitLancer, Pal.lancerLaser, player.x, player.y);
                    Lightning.create(player.getTeam(), Pal.lancerLaser, 10f * Vars.state.rules.playerDamageMultiplier,
                    player.x + player.velocity().x, player.y + player.velocity().y, player.rotation, 14);
                }
            }

            @Override
            public void draw(Player player){
                float scl = scld(player);
                if(scl < 0.01f) return;
                Draw.color(Pal.lancerLaser);
                Draw.alpha(scl / 2f);
                Draw.blend(Blending.additive);
                Draw.rect(shield, player.x + Mathf.range(scl / 2f), player.y + Mathf.range(scl / 2f), player.rotation - 90);
                Draw.blend();
            }

            float scld(Player player){
                return Mathf.clamp((player.velocity().len() - minV) / (maxV - minV));
            }
        };

        trident = new Mech("trident-ship", true){
            {
                drillPower = 2;
                speed = 0.15f;
                drag = 0.034f;
                mass = 2.5f;
                turnCursor = false;
                health = 250f;
                itemCapacity = 30;
                engineColor = Color.valueOf("84f491");
                cellTrnsY = 1f;
                buildPower = 2.5f;
                weapon = new Weapon("bomber"){{
                    length = 0f;
                    width = 2f;
                    reload = 25f;
                    shots = 2;
                    shotDelay = 1f;
                    shots = 8;
                    roundrobin = true;
                    ejectEffect = Fx.none;
                    velocityRnd = 1f;
                    inaccuracy = 20f;
                    ignoreRotation = true;
                    bullet = new BombBulletType(16f, 25f, "shell"){{
                        bulletWidth = 10f;
                        bulletHeight = 14f;
                        hitEffect = Fx.flakExplosion;
                        shootEffect = Fx.none;
                        smokeEffect = Fx.none;
                        shootSound = Sounds.artillery;
                    }};
                }};
            }

            @Override
            public boolean canShoot(Player player){
                return player.velocity().len() > 1.2f;
            }
        };

        glaive = new Mech("glaive-ship", true){
            {
                drillPower = 4;
                mineSpeed = 1.3f;
                speed = 0.32f;
                drag = 0.06f;
                mass = 3f;
                health = 240f;
                itemCapacity = 60;
                engineColor = Color.valueOf("feb380");
                cellTrnsY = 1f;
                buildPower = 1.2f;

                weapon = new Weapon("bomber"){{
                    length = 1.5f;
                    reload = 13f;
                    roundrobin = true;
                    ejectEffect = Fx.shellEjectSmall;
                    bullet = Bullets.standardGlaive;
                    shootSound = Sounds.shootSnap;
                }};
            }
        };

        starter = dart;
    }
}
