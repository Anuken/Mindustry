package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Bullets;

public class Monk extends Mech{
    public static TextureRegion[] idle;

    public Monk(String name, boolean flying){
        super(name, flying);

        drillPower = 1;
        mineSpeed = 1.5f;
        mass = 1.2f;
        speed = 0.5f;
        itemCapacity = 40;
        boostSpeed = 0.95f;
        buildPower = 1.2f;
        health = 250f;

        weapon = new Weapon("blaster"){{
            length = 1.5f;
            reload = 14f;
            alternate = true;
            bullet = Bullets.waterShot;
        }};
    }

    @Override
    public void load(){
        weapon.load();

        idle = new TextureRegion[6];
        for(int i = 0; i < 6; i++){
            idle[i] = Core.atlas.find("monk-idle-0-" + i);
        }

        region = Core.atlas.find("monk-idle-0-0");
    }
}
