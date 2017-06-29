package io.anuke.mindustry.world.blocks;
import static io.anuke.mindustry.Vars.tilesize;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Timers;

public class Turret extends Block{
	protected float range = 50f;
	protected float reload = 10f;
	protected String shootsound = "shoot";
	protected BulletType bullet;
	protected Item ammo;

	public Turret(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect("block", tile.worldx(), tile.worldy());
	}
	
	@Override
	public void drawOver(Tile tile){
		TurretEntity entity = tile.entity();
		Draw.rect(name(), tile.worldx(), tile.worldy(), entity.rotation - 90);
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		Draw.color("green");
		Draw.dashcircle(tile.worldx(), tile.worldy(), range);
		Draw.reset();
	}
	
	@Override
	public void drawOverlay(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.ammo <= 0 && ammo != null){
			Draw.tcolor(Color.SCARLET);
			Draw.tscl(1 / 8f);
			Draw.text("No ammo!", tile.worldx(), tile.worldy() + tilesize);

		}else if(ammo != null){
			Draw.tscl(1 / 8f);
			Draw.tcolor(Color.GREEN);
			Draw.text("Ammo: " + entity.ammo, tile.worldx(), tile.worldy() - tilesize);
		}
		
		Draw.tscl(Vars.fontscale);
	}
	
	@Override
	public boolean accept(Item item, Tile dest, Tile source){
		return item == ammo;
	}

	@Override
	public String description(){
		return "Shoots things.";
	}
	
	@Override
	public void update(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.hasItem(ammo)){
			entity.ammo += 20;
			entity.removeItem(ammo, 1);
		}
		
		if(entity.ammo > 0){
			Enemy enemy = (Enemy)Entities.getClosest(tile.worldx(), tile.worldy(), range, e->{
				return e instanceof Enemy;
			});
			
			if(enemy != null){
				entity.rotation = MathUtils.lerpAngleDeg(entity.rotation, Angles.predictAngle(tile.worldx(), tile.worldy(), enemy.x, enemy.y, enemy.xvelocity, enemy.yvelocity, bullet.speed - 0.1f), 0.2f);
				if(Timers.get(tile, reload)){
					Effects.sound(shootsound, entity);
					shoot(tile);
					entity.ammo --;
				}
			}
		}
	}
	
	@Override
	public TileEntity getEntity(){
		return new TurretEntity();
	}
	
	protected void shoot(Tile tile){
		TurretEntity entity = tile.entity();
		
		vector.set(0, 4).setAngle(entity.rotation);
		new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, entity.rotation).add();
	}
	
	protected void bullet(Tile tile, float angle){
		new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, angle).add();
	}
	
	static class TurretEntity extends TileEntity{
		public TileEntity target;
		public int ammo;
		public float rotation;
	}
}
