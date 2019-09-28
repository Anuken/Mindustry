package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.world.*;

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

        Draw.color(Color.white);
        Draw.alpha(entity.hit * 0.5f);
        Draw.blend(Blending.additive);
        Fill.rect(tile.drawx(), tile.drawy(), tilesize * size, tilesize * size);
        Draw.blend();
        Draw.reset();

        entity.hit = Mathf.clamp(entity.hit - Time.delta() / hitTime);
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);

        //doesn't reflect powerful bullets
        if(bullet.damage() > maxDamageDeflect || bullet.isDeflected()) return;

        float penX = Math.abs(entity.x - bullet.x), penY = Math.abs(entity.y - bullet.y);

        bullet.hitbox(rect2);

        Vector2 position = Geometry.raycastRect(bullet.x - bullet.velocity().x*Time.delta(), bullet.y - bullet.velocity().y*Time.delta(), bullet.x + bullet.velocity().x*Time.delta(), bullet.y + bullet.velocity().y*Time.delta(),
        rect.setSize(size * tilesize + rect2.width*2 + rect2.height*2).setCenter(entity.x, entity.y));

        if(position != null){
            bullet.set(position.x, position.y);
        }

        if(penX > penY){
            bullet.velocity().x *= -1;
        }else{
            bullet.velocity().y *= -1;
        }

        //bullet.updateVelocity();
        bullet.resetOwner(entity, entity.getTeam());
        bullet.scaleTime(1f);
        bullet.deflect();

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
