package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.types.AlphaDrone;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.maps.TutorialSector;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Mech;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.unitGroups;

public class Mechs implements ContentList{
    public static Mech alpha, delta, tau, omega, dart, javelin, trident, glaive;

    /**These are not new mechs, just re-assignments for convenience.*/
    public static Mech starterDesktop, starterMobile;

    @Override
    public void load(){

        alpha = new Mech("alpha-mech", false){
            int maxDrones = 3;
            float buildTime = 20f;

            {
                drillPower = 1;
                mineSpeed = 1.5f;
                mass = 1.2f;
                speed = 0.5f;
                boostSpeed = 0.85f;
                weapon = Weapons.blaster;
                trailColorTo = Color.valueOf("ffd37f");
                armor = 20f;
            }

            @Override
            public void updateAlt(Player player){

                if(player.isShooting && getDrones(player) < maxDrones && !TutorialSector.supressDrone()){
                    player.timer.get(Player.timerAbility, buildTime);

                    if(player.timer.getTime(Player.timerAbility) > buildTime/2f){
                        if(!Net.client()){
                            AlphaDrone drone = (AlphaDrone) UnitTypes.alphaDrone.create(player.getTeam());
                            drone.leader = player;
                            drone.set(player.x, player.y);
                            drone.add();

                            Effects.effect(UnitFx.unitLand, player);
                        }
                    }
                }
            }

            int getDrones(Player player){
                int sum = 0;
                for(BaseUnit unit : unitGroups[player.getTeam().ordinal()].all()){
                    if(unit instanceof AlphaDrone && ((AlphaDrone) unit).leader == player) sum ++;
                }
                return sum;
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
                armor = 30f;
                weaponOffsetX = -1;
                weaponOffsetY = -1;
                weapon = Weapons.shockgun;
                trailColorTo = Color.valueOf("d3ddff");
            }

            @Override
            public void onLand(Player player){
                if(player.timer.get(Player.timerAbility, cooldown)){
                    Effects.shake(1f, 1f, player);
                    Effects.effect(UnitFx.landShock, player);
                    for(int i = 0; i < 8; i++){
                        Timers.run(Mathf.random(8f), () -> Lightning.create(player.getTeam(), Palette.lancerLaser, 17f, player.x, player.y, Mathf.random(360f), 14));
                    }
                }
            }
        };

        tau = new Mech("tau-mech", false){
            float healRange = 60f;
            float healAmount = 10f;
            float healReload = 160f;
            Rectangle rect = new Rectangle();
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
                weapon = Weapons.healBlaster;
                armor = 15f;
                trailColorTo = Palette.heal;
            }

            @Override
            public void updateAlt(Player player){

                if(player.timer.get(Player.timerAbility, healReload)){
                    wasHealed = false;

                    rect.setSize(healRange*2f).setCenter(player.x, player.y);
                    Units.getNearby(player.getTeam(), rect, unit -> {
                        if(unit.distanceTo(player) <= healRange){
                            if(unit.health < unit.maxHealth()){
                                Effects.effect(UnitFx.heal, unit);
                                wasHealed = true;
                            }
                            unit.healBy(healAmount);
                        }
                    });

                    if(wasHealed){
                        Effects.effect(UnitFx.healWave, player);
                    }
                }
            }
        };

        omega = new Mech("omega-mech", false){
            protected TextureRegion armorRegion;

            {
                drillPower = 2;
                mineSpeed = 1.5f;
                itemCapacity = 50;
                speed = 0.36f;
                boostSpeed = 0.6f;
                mass = 4f;
                shake = 4f;
                weaponOffsetX = 1;
                weaponOffsetY = 0;
                weapon = Weapons.swarmer;
                trailColorTo = Color.valueOf("feb380");
                armor = 45f;
            }

            @Override
            public float getRotationAlpha(Player player){
                return 0.6f - player.shootHeat * 0.3f;
            }

            @Override
            public float spreadX(Player player){
                return player.shootHeat*2f;
            }

            @Override
            public void load(){
                super.load();
                armorRegion = Draw.region(name + "-armor");
            }

            @Override
            public void updateAlt(Player player){
                float scl = 1f - player.shootHeat/2f;
                player.getVelocity().scl(scl);
            }

            @Override
            public float getExtraArmor(Player player){
                return player.shootHeat * 30f;
            }

            @Override
            public void draw(Player player){
                if(player.shootHeat <= 0.01f) return;

                float alpha = Core.batch.getColor().a;
                Shaders.build.progress = player.shootHeat;
                Shaders.build.region = armorRegion;
                Shaders.build.time = Timers.time() / 10f;
                Shaders.build.color.set(Palette.accent).a = player.shootHeat;
                Graphics.shader(Shaders.build);
                Draw.alpha(1f);
                Draw.rect(armorRegion, player.snappedX(), player.snappedY(), player.rotation);
                Graphics.shader(Shaders.mix);
                Draw.color(1f, 1f, 1f, alpha);
            }
        };

        dart = new Mech("dart-ship", true){{
            drillPower = 1;
            mineSpeed = 0.9f;
            speed = 0.4f;
            drag = 0.1f;
            armor = 10f;
            weapon = Weapons.blasterSmall;
            weaponOffsetX = -1;
            weaponOffsetY = -1;
            trailColor = Palette.lightTrail;
            cellTrnsY = 1f;
        }};

        javelin = new Mech("javelin-ship", true){
            float minV = 3.6f;
            float maxV = 6f;
            TextureRegion shield;
            {
                drillPower = -1;
                speed = 0.11f;
                drag = 0.01f;
                mass = 2f;
                armor = 5f;
                weapon = Weapons.missiles;
                trailColor = Color.valueOf("d3ddff");
                cellTrnsY = 1f;
            }

            @Override
            public void load(){
                super.load();
                shield = Draw.region(name + "-shield");
            }

            @Override
            public float getRotationAlpha(Player player){
                return 0.5f;
            }

            @Override
            public void updateAlt(Player player){
                float scl = scld(player);
                if(Mathf.chance(Timers.delta() * (0.15*scl))){
                    Effects.effect(BulletFx.hitLancer, Palette.lancerLaser, player.x, player.y);
                    Lightning.create(player.getTeam(), Palette.lancerLaser, 10f,
                    player.x + player.getVelocity().x, player.y + player.getVelocity().y, player.rotation, 14);
                }
            }

            @Override
            public void draw(Player player){
                float scl = scld(player);
                if(scl < 0.01f) return;
                float alpha = Core.batch.getColor().a;
                Graphics.shader();
                Graphics.setAdditiveBlending();
                Draw.color(Palette.lancerLaser);
                Draw.alpha(scl/2f);
                Draw.rect(shield, player.snappedX() + Mathf.range(scl/2f), player.snappedY() + Mathf.range(scl/2f), player.rotation - 90);
                Graphics.setNormalBlending();
                Graphics.shader(Shaders.mix);
                Draw.color();
                Draw.alpha(alpha);
            }

            float scld(Player player){
                return Mathf.clamp((player.getVelocity().len() - minV) / (maxV - minV));
            }
        };

        trident = new Mech("trident-ship", true){
            {
                drillPower = 2;
                speed = 0.14f;
                drag = 0.034f;
                mass = 2.5f;
                turnCursor = false;
                armor = 20f;
                itemCapacity = 30;
                trailColor = Color.valueOf("84f491");
                weapon = Weapons.bomberTrident;
                cellTrnsY = 1f;
            }

            @Override
            public boolean canShoot(Player player){
                return player.getVelocity().len() > 1.2f;
            }
        };

        glaive = new Mech("glaive-ship", true){
            {
                weapon = Weapons.glaiveBlaster;
                drillPower = 4;
                mineSpeed = 1.3f;
                speed = 0.32f;
                drag = 0.06f;
                mass = 3f;
                armor = 30f;
                itemCapacity = 60;
                trailColor = Color.valueOf("feb380");
                cellTrnsY = 1f;
            }
        };

        starterDesktop = alpha;
        starterMobile = dart;
    }

    @Override
    public ContentType type(){
        return ContentType.mech;
    }
}
