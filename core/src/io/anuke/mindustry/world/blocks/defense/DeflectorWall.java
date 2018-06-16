package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class DeflectorWall extends Wall {
    static final float hitTime = 10f;

    public DeflectorWall(String name) {
        super(name);
        update = false;
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

        entity.hit = Mathf.clamp(entity.hit - Timers.delta()/hitTime);

        Graphics.setNormalBlending();
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);

        float penX = Math.abs(entity.x - bullet.x), penY = Math.abs(entity.y - bullet.y);

        if(penX < tilesize/2f * size) {
            bullet.getVelocity().x *= -1;
        }

        if(penY < tilesize/2f * size){
            bullet.getVelocity().y *= -1;
        }

        bullet.updateVelocity(BulletType.getByID(bullet.getTypeID()).drag);
        bullet.supressCollision();

        ((DeflectorEntity)entity).hit = 1f;
    }

    @Override
    public TileEntity getEntity() {
        return new DeflectorEntity();
    }

    static class DeflectorEntity extends TileEntity{
        public float hit;
    }
}
