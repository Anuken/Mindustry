package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;

public class DeflectorWall extends Wall{
    public static final float hitTime = 10f;

    protected float maxDamageDeflect = 10f;
    protected Rectangle rect = new Rectangle();
    protected Rectangle rect2 = new Rectangle();

    public DeflectorWall(String name){
        super(name);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        DeflectorEntity entity = tile.entity();

        if(entity.hit < 0.0001f) return;

        Draw.color(Color.WHITE);
        Draw.alpha(entity.hit * 0.5f);
        Draw.blend(Blending.additive);
        Draw.rect("blank", tile.drawx(), tile.drawy(), tilesize * size, tilesize * size);
        Draw.blend();
        Draw.reset();

        entity.hit = Mathf.clamp(entity.hit - Time.delta() / hitTime);
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);

        //doesn't reflect powerful bullets
        if(bullet.damage() > maxDamageDeflect) return;

        float penX = Math.abs(entity.x - bullet.x), penY = Math.abs(entity.y - bullet.y);

        bullet.hitbox(rect2);

        Vector2 position = Geometry.raycastRect(bullet.x, bullet.y, bullet.x + bullet.velocity().x, bullet.y + bullet.velocity().y,
        rect.setCenter(entity.x, entity.y).setSize(size * tilesize + rect2.width + rect2.height));

        if(position != null){
            bullet.set(position.x, position.y);
        }

        if(penX > penY){
            bullet.velocity().x *= -1;
        }else{
            bullet.velocity().y *= -1;
        }

        bullet.updateVelocity();
        bullet.resetOwner(entity, Team.none);
        bullet.scaleTime(1f);
        bullet.supress();

        ((DeflectorEntity)entity).hit = 1f;
    }

    @Override
    public TileEntity newEntity(){
        return new DeflectorEntity();
    }

    public static class DeflectorEntity extends TileEntity{
        public float hit;
    }
}
