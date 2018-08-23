package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Mechs implements ContentList{
    public static Mech alpha, delta, tau, omega, dart, javelin, trident, halberd;

    /**These are not new mechs, just re-assignments for convenience.*/
    public static Mech starterDesktop, starterMobile;

    @Override
    public void load(){

        alpha = new Mech("alpha-mech", false){{
            drillPower = 1;
            mineSpeed = 1.5f;
            speed = 0.5f;
            boostSpeed = 0.85f;
            weapon = Weapons.blaster;
            maxSpeed = 4f;
        }};

        delta = new Mech("delta-mech", false){
            {
                drillPower = -1;
                speed = 0.75f;
                boostSpeed = 0.95f;
                itemCapacity = 15;
                armor = 30f;
                weaponOffsetX = -1;
                itemCapacity = 15;
                weaponOffsetY = -1;
                weapon = Weapons.shockgun;
                trailColorTo = Color.valueOf("d3ddff");
                maxSpeed = 5f;
                altChargeAlpha = 0.03f;
            }

            @Override
            public void updateAlt(Player player){
                if(player.altHeat >= 0.91f){
                    Effects.shake(3f, 3f, player);
                    for(int i = 0; i < 8; i++){
                        Timers.run(Mathf.random(5f), () -> Lightning.create(player.getTeam(), BulletFx.hitLancer, player.getTeam().color, 10f, player.x, player.y, Mathf.random(360f), 20));
                    }
                    player.altHeat = 0f;
                }
            }

            @Override
            public void draw(Player player){
                super.draw(player);
                player.hitTime = Math.max(player.hitTime, player.altHeat * Unit.hitDuration);
            }
        };

        tau = new Mech("tau-mech", false){
            protected float healRange = 60f;
            protected float healAmount = 10f;

            protected Rectangle rect = new Rectangle();

            {
                drillPower = 4;
                mineSpeed = 3f;
                itemCapacity = 70;
                weaponOffsetY = -1;
                weaponOffsetX = 1;
                speed = 0.44f;
                drag = 0.35f;
                boostSpeed = 0.8f;
                weapon = Weapons.healBlaster;
                maxSpeed = 5f;
                armor = 30f;
                altChargeAlpha = 0.05f;
                trailColorTo = Palette.heal;
            }

            @Override
            public void draw(Player player){
                super.draw(player);
                player.hitTime = Math.max(player.hitTime, player.altHeat * Unit.hitDuration);
            }

            @Override
            public void updateAlt(Player player){
                if(player.altHeat >= 0.91f){
                    Effects.effect(UnitFx.healWave, player);
                    rect.setSize(healRange*2f).setCenter(player.x, player.y);
                    Units.getNearby(player.getTeam(), rect, unit -> {
                        if(unit.distanceTo(player) <= healRange){
                            if(unit.health < unit.maxHealth()){
                                Effects.effect(UnitFx.heal, unit);
                            }
                            unit.healBy(healAmount);
                        }
                    });

                    int blockRange = (int)(healRange/tilesize);
                    int px = world.toTile(player.x), py = world.toTile(player.y);

                    for(int x = -blockRange; x <= blockRange; x++){
                        for(int y = -blockRange; y <= blockRange; y++){
                            if(Mathf.dst(x, y) > blockRange) continue;
                            Tile tile = world.tile(px + x, py + y);
                            if(tile != null){
                                Fire.extinguish(tile, 1000f);
                            }
                        }
                    }
                    player.altHeat = 0f;
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
                shake = 4f;
                weaponOffsetX = 1;
                weaponOffsetY = 0;
                weapon = Weapons.swarmer;
                trailColorTo = Color.valueOf("feb380");
                maxSpeed = 3.5f;
                armor = 50f;
            }

            @Override
            public float getRotationAlpha(Player player){
                return 0.6f - player.altHeat * 0.3f;
            }

            @Override
            public float spreadX(Player player){
                return player.altHeat*2f;
            }

            @Override
            public void load(){
                super.load();
                armorRegion = Draw.region(name + "-armor");
            }

            @Override
            public void updateAlt(Player player){
                float scl = 1f - player.altHeat/2f;
                player.getVelocity().scl(scl);
            }

            @Override
            public float getExtraArmor(Player player){
                return player.altHeat * 40f;
            }

            @Override
            public void draw(Player player){
                if(player.altHeat <= 0.01f) return;

                float alpha = Core.batch.getColor().a;
                Shaders.build.progress = player.altHeat;
                Shaders.build.region = armorRegion;
                Shaders.build.time = Timers.time() / 10f;
                Shaders.build.color.set(Palette.accent).a = player.altHeat;
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
            maxSpeed = 3f;
            drag = 0.1f;
            weaponOffsetX = -1;
            weaponOffsetY = -1;
            trailColor = Palette.lightTrail;
        }};

        javelin = new Mech("javelin-ship", true){{
            drillPower = -1;
            speed = 0.4f;
            maxSpeed = 3.6f;
            drag = 0.09f;
            weapon = Weapons.missiles;
            trailColor = Color.valueOf("d3ddff");
        }};

        trident = new Mech("trident-ship", true){{
            drillPower = 1;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        halberd = new Mech("halberd-ship", true){{
            drillPower = 2;
            speed = 0.4f;
            maxSpeed = 3f;
            drag = 0.1f;
        }};

        starterDesktop = alpha;
        starterMobile = dart;
    }

    @Override
    public Array<? extends Content> getAll(){
        return Upgrade.all();
    }
}
