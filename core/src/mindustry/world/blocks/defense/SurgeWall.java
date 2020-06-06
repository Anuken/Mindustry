package mindustry.world.blocks.defense;

import arc.math.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class SurgeWall extends Wall{
    public float lightningChance = 0.05f;
    public float lightningDamage = 20f;
    public int lightningLength = 17;

    public SurgeWall(String name){
        super(name);
    }

    public class SurgeEntity extends TileEntity{
        @Override
        public void collision(Bulletc bullet){
            super.collision(bullet);
            if(Mathf.chance(lightningChance)){
                Lightning.create(team(), Pal.surge, lightningDamage, x, y, bullet.rotation() + 180f, lightningLength);
            }
        }
    }
}
