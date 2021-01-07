package mindustry.type;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.io.*;

/** A point defense. Although it extends Weapon, it really has an AI of it's own. */
public class PointDefense extends Weapon{
    public float range = 80f;
    public float retargetTime = 5f;

    public Color color = Color.white;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.pointHit;

    public float bulletDamage = 10f;
    public float shootLength = 3f;

    public PointDefense(String name){
        super(name);

        bullet = new BasicBulletType();
        rotate = true;
        ejectEffect = Fx.sparkShoot;
    }

    public PointDefense(){
        this("");
    }
    
    @Override
    public PointDefense copy(){
        PointDefense out = new PointDefense();
        JsonIO.json().copyFields(this, out);
        return out;
    }
}