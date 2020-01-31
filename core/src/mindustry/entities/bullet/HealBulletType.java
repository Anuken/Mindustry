package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

public class HealBulletType extends BulletType{
    protected float healPercent = 3f;

    public HealBulletType(float speed, float damage){
        super(speed, damage);

        shootEffect = Fx.shootHeal;
        smokeEffect = Fx.hitLaser;
        hitEffect = Fx.hitLaser;
        despawnEffect = Fx.hitLaser;
        collidesTeam = true;
    }

    public HealBulletType(){
        this(1f, 1f);
    }

    @Override
    public boolean collides(Bullet b, Tile tile){
        return tile.getTeam() != b.getTeam() || tile.entity.healthf() < 1f;
    }

    @Override
    public void draw(Bullet b){
        Draw.color(Pal.heal);
        Lines.stroke(2f);
        Lines.lineAngleCenter(b.x, b.y, b.rot(), 7f);
        Draw.color(Color.white);
        Lines.lineAngleCenter(b.x, b.y, b.rot(), 3f);
        Draw.reset();
    }

    @Override
    public void hitTile(Bullet b, Tile tile){
        super.hit(b);
        tile = tile.link();

        if(tile.entity != null && tile.getTeam() == b.getTeam() && !(tile.block() instanceof BuildBlock)){
            Effects.effect(Fx.healBlockFull, Pal.heal, tile.drawx(), tile.drawy(), tile.block().size);
            tile.entity.healBy(healPercent / 100f * tile.entity.maxHealth());
        }
    }
}
