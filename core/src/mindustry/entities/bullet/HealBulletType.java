package mindustry.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.entities.type.base.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.*;

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
            float heal = healPercent / 100f * tile.entity.maxHealth();
            tile.entity.healBy(heal);

            if(b.getOwner() instanceof RepairDrone && tile.block() instanceof ShockMine){

                RepairDrone owner = (RepairDrone)b.getOwner();

                if(owner.item().amount >= owner.getItemCapacity()){
                    Call.transferItemTo(Items.silicon, owner.item().amount, owner.x, owner.y, owner.getClosestCore().tile);
                    owner.clearItem();
                }

                owner.item().item = Items.silicon;
                owner.item().amount++;

                tile.entity.damage(heal * 2);
            }
        }
    }
}
