package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;

public class SurgeWall extends Wall{
    public float lightningChance = 0.05f;
    public float lightningDamage = 15f;
    public int lightningLength = 17;

    public SurgeWall(String name){
        super(name);
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);
        if(Mathf.chance(lightningChance)){
            Lightning.create(entity.getTeam(), Pal.surge, lightningDamage, bullet.x, bullet.y, bullet.rot() + 180f, lightningLength);
        }
    }
}
