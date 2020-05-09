package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.*;

public class HealBulletType extends BulletType{
    protected float healPercent = 3f;
    protected float bulletHeight = 7f, bulletWidth = 2f;
    protected Color backColor = Pal.heal, frontColor = Color.white;

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
    public boolean collides(Bulletc b, Tilec tile){
        return tile.team() != b.team() || tile.healthf() < 1f;
    }

    @Override
    public void draw(Bulletc b){
        Draw.color(backColor);
        Lines.stroke(bulletWidth);
        Lines.lineAngleCenter(b.x(), b.y(), b.rotation(), bulletHeight);
        Draw.color(frontColor);
        Lines.lineAngleCenter(b.x(), b.y(), b.rotation(), bulletHeight / 2f);
        Draw.reset();
    }

    @Override
    public void hitTile(Bulletc b, Tilec tile){
        super.hit(b);

        if(tile.team() == b.team() && !(tile.block() instanceof BuildBlock)){
            Fx.healBlockFull.at(tile.x(), tile.y(), tile.block().size, Pal.heal);
            tile.heal(healPercent / 100f * tile.maxHealth());
        }
    }
}
