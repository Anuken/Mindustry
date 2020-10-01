package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;

import static mindustry.Vars.tilesize;

public class ChargeTurret extends PowerTurret{
    public float chargeTime = 30f;
    public int chargeEffects = 5;
    public float chargeMaxDelay = 10f;
    public Effect chargeEffect = Fx.none;
    public Effect chargeBeginEffect = Fx.none;

    public ChargeTurret(String name){
        super(name);
    }

    public class ChargeTurretBuild extends PowerTurretBuild{
        public boolean shooting;

        @Override
        public void shoot(BulletType ammo){
            useAmmo();

            tr.trns(rotation, size * tilesize / 2f);
            chargeBeginEffect.at(x + tr.x, y + tr.y, rotation);

            for(int i = 0; i < chargeEffects; i++){
                Time.run(Mathf.random(chargeMaxDelay), () -> {
                    if(!isValid()) return;
                    tr.trns(rotation, size * tilesize / 2f);
                    chargeEffect.at(x + tr.x, y + tr.y, rotation);
                });
            }

            shooting = true;

            Time.run(chargeTime, () -> {
                if(!isValid()) return;
                tr.trns(rotation, size * tilesize / 2f);
                recoil = recoilAmount;
                heat = 1f;
                bullet(ammo, rotation + Mathf.range(inaccuracy));
                effects();
                shooting = false;
            });
        }

        @Override
        public boolean shouldTurn(){
            return !shooting;
        }
    }
}
