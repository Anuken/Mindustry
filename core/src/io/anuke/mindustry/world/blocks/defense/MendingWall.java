package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.defense.DeflectorWall.DeflectorEntity;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class MendingWall extends Wall{
    protected float regenSpeed = 0.25f;

    public MendingWall(String name){
        super(name);
        update = true;
    }

    @Override
    public void handleBulletHit(TileEntity entity, Bullet bullet){
        super.handleBulletHit(entity, bullet);
        ((DeflectorEntity) entity).hit = 1f;
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

        entity.hit = Mathf.clamp(entity.hit - Timers.delta() / DeflectorWall.hitTime);

        Graphics.setNormalBlending();
    }

    @Override
    public void update(Tile tile){
        tile.entity.health = Mathf.clamp(tile.entity.health + regenSpeed * Timers.delta(), 0f, health);
    }

    @Override
    public TileEntity newEntity(){
        return new DeflectorEntity();
    }
}
