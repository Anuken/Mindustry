package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;

public class SurgeWall extends Wall{
    protected float lightningChance = 0.05f;
    protected float lightningDamage = 15f;
    protected int lightningLength = 17;

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
