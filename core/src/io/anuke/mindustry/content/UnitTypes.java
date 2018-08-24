package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.entities.units.types.*;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;

public class UnitTypes implements ContentList{
    public static UnitType drone, alphaDrone, dagger, interceptor, monsoon, titan, fabricator;

    @Override
    public void load(){
        drone = new UnitType("drone", Drone.class, Drone::new){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.2f;
            maxVelocity = 0.8f;
            range = 50f;
            healSpeed = 0.05f;
            health = 60;
        }};

        alphaDrone = new UnitType("alpha-drone", AlphaDrone.class, AlphaDrone::new){
            {
                isFlying = true;
                drag = 0.005f;
                speed = 0.5f;
                maxVelocity = 1.6f;
                range = 40f;
                health = 15;
                weapon = Weapons.droneBlaster;
                trailColor = Color.valueOf("ffd37f");
            }

            @Override
            public boolean isHidden() {
                return true;
            }
        };

        dagger = new UnitType("dagger", Dagger.class, Dagger::new){{
            maxVelocity = 1.1f;
            speed = 0.2f;
            drag = 0.4f;
            range = 40f;
            weapon = Weapons.chainBlaster;
            health = 180;
        }};

        titan = new UnitType("titan", Titan.class, Titan::new){{
            maxVelocity = 0.8f;
            speed = 0.18f;
            drag = 0.4f;
            range = 10f;
            weapon = Weapons.flamethrower;
            health = 500;
        }};

        interceptor = new UnitType("interceptor", Interceptor.class, Interceptor::new){{
            speed = 0.3f;
            maxVelocity = 1.9f;
            drag = 0.01f;
            weapon = Weapons.chainBlaster;
            isFlying = true;
            health = 70;
        }};

        monsoon = new UnitType("monsoon", Monsoon.class, Monsoon::new){{
            health = 250;
            speed = 0.2f;
            maxVelocity = 1.4f;
            drag = 0.01f;
            isFlying = true;
            weapon = Weapons.bomber;
        }};

        fabricator = new UnitType("fabricator", Fabricator.class, Fabricator::new){{
            isFlying = true;
            drag = 0.01f;
            speed = 0.2f;
            maxVelocity = 0.9f;
            range = 70f;
            itemCapacity = 70;
            health = 220;
            buildPower = 0.9f;
            minePower = 1.1f;
            healSpeed = 0.09f;
            toMine = ObjectSet.with(Items.lead, Items.copper, Items.titanium);
        }};
    }

    @Override
    public Array<? extends Content> getAll(){
        return UnitType.all();
    }
}
