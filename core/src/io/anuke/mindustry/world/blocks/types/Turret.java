package io.anuke.mindustry.world.blocks.types;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

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
	
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected String shootsound = "shoot";
	protected BulletType bullet = BulletType.iron;
	protected Item ammo;
	protected int ammoMultiplier = 20;
	protected int maxammo = 400;
	protected float rotatespeed = 0.2f;
	protected float shootCone = 8f;
	protected float overPrediction = 0f;

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
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 13, fract);
	}
	
	@Override
	public void drawPlace(int x, int y, boolean valid){
		//TODO?
		Draw.color(Color.PURPLE);
		Draw.thick(1f);
		Draw.dashcircle(x*Vars.tilesize, y*Vars.tilesize, range);
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
			entity.ammo += ammoMultiplier;
			entity.removeItem(ammo, 1);
		}
		
		if(entity.target != null && entity.target.isDead())
			entity.target = null;
		
		if(entity.ammo > 0){
			
			if(Timers.get(entity, "target", targetInterval)){
				entity.target = (Enemy)Entities.getClosest(tile.worldx(), tile.worldy(), range, e->{
					return e instanceof Enemy && !((Enemy)e).isDead();
				});
			}
			
			if(entity.target != null){
				
				float targetRot = Angles.predictAngle(tile.worldx(), tile.worldy(), 
						entity.target.x, entity.target.y, entity.target.xvelocity, entity.target.yvelocity, bullet.speed + overPrediction);
				
				entity.rotation = MathUtils.lerpAngleDeg(entity.rotation, targetRot, 
						rotatespeed*Timers.delta());
				
				float reload = Vars.multiplier*this.reload;
				if(Angles.angleDist(entity.rotation, targetRot) < shootCone && Timers.get(tile, "reload", reload)){
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
		
		float inac = Mathf.range(inaccuracy);
		
		vector.set(0, 4).setAngle(entity.rotation + inac);
		Bullet out = new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, entity.rotation + inac).add();
		out.damage = (int)(bullet.damage*Vars.multiplier);
	}
	
	protected void bullet(Tile tile, float angle){
		 Bullet out = new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, angle).add();
		 out.damage = (int)(bullet.damage*Vars.multiplier);
	}
	
	public static class TurretEntity extends TileEntity{
		public TileEntity blockTarget;
		public int ammo;
		public float rotation;
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
