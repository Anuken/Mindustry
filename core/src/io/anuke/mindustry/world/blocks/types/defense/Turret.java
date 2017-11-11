package io.anuke.mindustry.world.blocks.types.defense;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

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
	public static final int targetInterval = 15;
	private static boolean drawDebug = false;
	
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected String shootsound = "shoot";
	protected BulletType bullet = BulletType.iron;
	protected Item ammo;
	protected int ammoMultiplier = 20;
	protected int maxammo = 400;
	protected float rotatespeed = 0.2f;
	protected float shootCone = 5f;

	public Turret(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Turret;
	}
	
	@Override
	public void draw(Tile tile){
		Vector2 offset = getPlaceOffset();
		
		if(isMultiblock()){
			Draw.rect("block-" + width + "x" + height, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}else{
			Draw.rect("block", tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
	}
	
	@Override
	public void drawOver(Tile tile){
		TurretEntity entity = tile.entity();
		Vector2 offset = getPlaceOffset();
		
		Draw.rect(name(), tile.worldx() + offset.x, tile.worldy() + offset.y, entity.rotation - 90);
		
		if(Vars.debug && drawDebug){
			drawTargeting(tile);
		}
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		Vector2 offset = getPlaceOffset();
		
		Draw.color("green");
		Draw.dashcircle(tile.worldx() + offset.x, tile.worldy() + offset.y, range);
		Draw.reset();
		
		TurretEntity entity = tile.entity();
		
		float fract = (float)entity.ammo/maxammo;
		if(fract > 0)
			fract = Mathf.clamp(fract, 0.24f, 1f);
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx() + offset.x, 2 + tile.worldy() + height/2f*Vars.tilesize + offset.y, fract);
	}
	
	@Override
	public void drawPlace(int x, int y, boolean valid){
		Draw.color(Color.PURPLE);
		Draw.thick(1f);
		Draw.dashcircle(x*Vars.tilesize, y*Vars.tilesize, range);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
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
			entity.ammo += ammoMultiplier;
			entity.removeItem(ammo, 1);
		}
		
		if(entity.target != null && entity.target.isDead())
			entity.target = null;
		
		if(hasAmmo(tile)){
			
			if(Timers.get(entity, "target", targetInterval)){
				entity.target = (Enemy)Entities.getClosest(tile.worldx(), tile.worldy(), range, e->{
					return e instanceof Enemy && !((Enemy)e).isDead();
				});
			}
			
			if(entity.target != null){
				
				float targetRot = Angles.predictAngle(tile.worldx(), tile.worldy(), 
						entity.target.x, entity.target.y, entity.target.xvelocity, entity.target.yvelocity, bullet.speed);
				 
				entity.rotation = Mathf.slerp(entity.rotation, targetRot, 
						rotatespeed*Timers.delta());
				
				float reload = Vars.multiplier*this.reload;
				if(Angles.angleDist(entity.rotation, targetRot) < shootCone && Timers.get(tile, "reload", reload)){
					Effects.sound(shootsound, entity);
					shoot(tile);
					consumeAmmo(tile);
					entity.ammo --;
				}
			}
		}
	}
	
	public boolean hasAmmo(Tile tile){
		TurretEntity entity = tile.entity();
		return entity.ammo > 0;
	}
	
	public void consumeAmmo(Tile tile){
		TurretEntity entity = tile.entity();
		entity.ammo --;
	}
	
	@Override
	public TileEntity getEntity(){
		return new TurretEntity();
	}
	
	void drawTargeting(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.target == null) return;
		
		float dst = entity.distanceTo(entity.target);
		float hittime = dst / bullet.speed;
		
		float angle = Angles.predictAngle(tile.worldx(), tile.worldy(), 
				entity.target.x, entity.target.y, entity.target.xvelocity, entity.target.yvelocity, bullet.speed);
		
		float predictX = entity.target.x + entity.target.xvelocity * hittime, 
				predictY = entity.target.y + entity.target.yvelocity * hittime;
		
		Draw.color(Color.GREEN);
		Draw.line(tile.worldx(), tile.worldy(), entity.target.x, entity.target.y);
		
		Draw.color(Color.RED);
		Draw.line(tile.worldx(), tile.worldy(), entity.target.x + entity.target.xvelocity * hittime, 
				entity.target.y + entity.target.yvelocity * hittime);
		
		Draw.color(Color.PURPLE);
		Draw.thick(2f);
		Draw.lineAngle(tile.worldx(), tile.worldy(), angle, 7f);
		
		Draw.reset();
		
		if(Timers.getTime(tile, "reload") <= 0){
			Timers.run(hittime, ()->{
				Effects.effect("spawn", predictX, predictY);
			});
		}
	}
	
	protected void shoot(Tile tile){
		TurretEntity entity = tile.entity();
		
		float inac = Mathf.range(inaccuracy);
		
		Angles.translation(entity.rotation + inac, width * Vars.tilesize / 2f);
		
		Bullet out = new Bullet(bullet, tile.entity, 
				tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), entity.rotation + inac).add();
		out.damage = (int)(bullet.damage*Vars.multiplier);
	}
	
	protected void bullet(Tile tile, float angle){
		 Bullet out = new Bullet(bullet, tile.entity, tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), angle).add();
		 out.damage = (int)(bullet.damage*Vars.multiplier);
	}
	
	public static class TurretEntity extends TileEntity{
		public TileEntity blockTarget;
		public int ammo;
		public float rotation = 90;
		public Enemy target;
		
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
