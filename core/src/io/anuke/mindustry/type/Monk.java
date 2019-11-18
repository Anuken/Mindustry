package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.gen.Sounds;

public class Monk extends Mech{
    public static TextureRegion[][] idle;
    public static TextureRegion[] fly;
    public static TextureRegion[][] attack;
    public static TextureRegion[][] walk;

    public Monk(String name, boolean flying){
        super(name, flying);

        drillPower = 1;
        mineSpeed = 0.1f;
        mass = 1f;
        speed = 0.25f;
        itemCapacity = 0;
        boostSpeed = 0.95f;
        buildPower = 0.1f;
        health = 1010100f;

        weapon = new Weapon("staff"){{
            length = 1.5f;
            reload = 150f;
            alternate = true;
            bullet = Bullets.convert;
            shootSound = Sounds.wololoo;
        }};
    }

    @Override
    public void load(){
        weapon.load();

        legRegion = Core.atlas.find("white");
        baseRegion = Core.atlas.find("white");

        idle = new TextureRegion[8][6];
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 6; j++){
                idle[i][j] = Core.atlas.find("monk-idle-"+ i +"-"+ j);
            }
        }

        fly = new TextureRegion[8];
        for(int i = 0; i < 8; i++){
            fly[i] = Core.atlas.find("monk-fly-"+ i);
        }

        attack = new TextureRegion[8][10];
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 10; j++){
                attack[i][j] = Core.atlas.find("monk-attack-"+ i +"-"+ j);
            }
        }

        walk = new TextureRegion[8][10];
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 10; j++){
                walk[i][j] = Core.atlas.find("monk-walk-"+ i +"-"+ j);
            }
        }

        region = Core.atlas.find("monk-idle-0-0");
    }
}
