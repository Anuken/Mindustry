package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Physics;

import static io.anuke.mindustry.Vars.tilesize;

public class DeflectorWall extends Wall{
    static final float hitTime = 10f;

    protected float maxDamageDeflect = 5f;
    protected Rectangle rect = new Rectangle();

    public DeflectorWall(String name){
        super(name);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        DeflectorEntity entity = tile.entity();

        if(entity.hit < 0.0001f) return;

        Graphics.setAdditiveBlending();

        Draw.color(Color.WHITE);
        Draw.alpha(entity.hit * 0.5f);
        Draw.rect("blank", tile.drawx(), tile.drawy(), tilesize * size, tilesize * size);
        Draw.reset();

        entity.hit = Mathf.clamp(entity.hit - Timers.delta() / hitTime);

        Graphics.setNormalBlending();
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);

        //doesn't reflect powerful bullets
        if(bullet.getDamage() > maxDamageDeflect) return;

        float penX = Math.abs(entity.x - bullet.x), penY = Math.abs(entity.y - bullet.y);

        Vector2 position = Physics.raycastRect(bullet.lastPosition().x, bullet.lastPosition().y, bullet.x, bullet.y,
                rect.setCenter(entity.x, entity.y).setSize(size * tilesize + bullet.hitbox.width + bullet.hitbox.height));

        if(position != null){
            bullet.set(position.x, position.y);
        }

        if(penX > penY){
            bullet.getVelocity().x *= -1;
        }else{
            bullet.getVelocity().y *= -1;
        }

        bullet.updateVelocity(0f);
        bullet.resetOwner(entity, Team.none);
        bullet.scaleTime(1f);
        bullet.supressCollision();

        ((DeflectorEntity) entity).hit = 1f;
    }

    @Override
    public TileEntity getEntity(){
        return new DeflectorEntity();
    }

    static class DeflectorEntity extends TileEntity{
        public float hit;
    }
}
