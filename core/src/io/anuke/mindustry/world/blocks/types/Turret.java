package io.anuke.mindustry.world.blocks.types;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.Renderer;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Turret extends Block{
	protected float range = 50f;
	protected float reload = 10f;
	protected String shootsound = "shoot";
	protected BulletType bullet;
	protected Item ammo;
	protected int maxammo = 400;

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
		
		TurretEntity entity = tile.entity();
		
		float fract = (float)entity.ammo/maxammo;
		if(fract > 0)
			fract = Mathf.clamp(fract, 0.24f, 1f);
		
		Renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 13, fract);
	}
	
	@Override
	public boolean accept(Item item, Tile dest, Tile source){
		return item == ammo && dest.<TurretEntity>entity().ammo < maxammo;
	}

	@Override
	public String description(){
		return "[turretinfo]Ammo: "+(ammo==null ? "N/A" : ammo.name())+"\nRange: " + (int)range + "\nDamage: " + bullet.damage;
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
				entity.rotation = MathUtils.lerpAngleDeg(entity.rotation, 
						Angles.predictAngle(tile.worldx(), tile.worldy(), enemy.x, enemy.y, enemy.xvelocity, enemy.yvelocity, bullet.speed), 
						0.2f*Timers.delta());
				float reload = Vars.multiplier*this.reload;
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
		Bullet out = new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, entity.rotation).add();
		out.damage = bullet.damage*Vars.multiplier;
	}
	
	protected void bullet(Tile tile, float angle){
		 Bullet out = new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, angle).add();
		 out.damage = bullet.damage*Vars.multiplier;
	}
	
	public static class TurretEntity extends TileEntity{
		public TileEntity target;
		public int ammo;
		public float rotation;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeInt(ammo);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			this.ammo = stream.readInt();
		}
	}
}
