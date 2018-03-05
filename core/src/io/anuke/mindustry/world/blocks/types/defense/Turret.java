package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Translator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Turret extends Block{
	static final int targetInterval = 15;
	static boolean drawDebug = false;
	
	protected final int timerTarget = timers++;
	protected final int timerReload = timers++;
	protected final int timerSound = timers++;
	
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected int shots = 1;
	protected float shotDelayScale = 0;
	protected String shootsound = "shoot";
	protected BulletType bullet = BulletType.iron;
	protected Item ammo;
	protected int ammoMultiplier = 20;
	protected int maxammo = 400;
	protected float rotatespeed = 0.2f;
	protected float shootCone = 5f;
	protected Effect shootEffect = null;
	protected float shootShake = 0f;
	protected int soundReload = 0;
	protected Translator tr = new Translator();

	public Turret(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.turret;

		bars.add(new BlockBar(Color.GREEN, true, tile -> (float)tile.<TurretEntity>entity().ammo / maxammo));
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		if(ammo != null) list.add("[turretinfo]Ammo: " + ammo);
		if(ammo != null) list.add("[turretinfo]Ammo Capacity: " + maxammo);
		if(ammo != null) list.add("[turretinfo]Ammo/Item: " + ammoMultiplier);
		list.add("[turretinfo]Range: " + (int)range);
		list.add("[turretinfo]Inaccuracy: " + (int)inaccuracy);
		list.add("[turretinfo]Damage/Shot: " + bullet.damage);
		list.add("[turretinfo]Shots/Second: " + Strings.toFixed(60f/reload, 1));
		list.add("[turretinfo]Shots: " + shots);
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Turret;
	}
	
	@Override
	public void draw(Tile tile){
		if(isMultiblock()){
			Draw.rect("block-" + width + "x" + height, tile.drawx(), tile.drawy());
		}else{
			Draw.rect("block", tile.drawx(), tile.drawy());
		}
	}
	
	@Override
	public void drawLayer(Tile tile){
		TurretEntity entity = tile.entity();

		Draw.rect(name(), tile.drawx(), tile.drawy(), entity.rotation - 90);
		
		if(debug && drawDebug){
			drawTargeting(tile);
		}
	}
	
	@Override
	public void drawSelect(Tile tile){
		Draw.color(Color.GREEN);
		Lines.dashCircle(tile.drawx(), tile.drawy(), range);
		Draw.reset();
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		Draw.color(Color.PURPLE);
		Lines.stroke(1f);
		Lines.dashCircle(x * tilesize, y * tilesize, range);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == ammo && tile.<TurretEntity>entity().ammo < maxammo;
	}
	
	@Override
	public void update(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(ammo != null && entity.hasItem(ammo)){
			entity.ammo += ammoMultiplier;
			entity.removeItem(ammo, 1);
		}
		
		if(entity.target != null && entity.target.isDead())
			entity.target = null;
		
		if(hasAmmo(tile) || (debug && infiniteAmmo)){
			
			if(entity.timer.get(timerTarget, targetInterval)){
				entity.target = (Enemy)Entities.getClosest(enemyGroup,
						tile.worldx(), tile.worldy(), range, e-> e instanceof Enemy && !((Enemy)e).isDead());
			}
			
			if(entity.target != null){
				
				float targetRot = Angles.predictAngle(tile.worldx(), tile.worldy(), 
						entity.target.x, entity.target.y, entity.target.velocity.x, entity.target.velocity.y, bullet.speed);
				
				if(Float.isNaN(entity.rotation)){
					entity.rotation = 0;
				}
				entity.rotation = Mathf.slerpDelta(entity.rotation, targetRot,
						rotatespeed);

				if(Angles.angleDist(entity.rotation, targetRot) < shootCone && entity.timer.get(timerReload, reload)){
					if(shootsound != null && entity.timer.get(timerSound, soundReload)) Effects.sound(shootsound, entity);
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
				entity.target.x, entity.target.y, entity.target.velocity.x, entity.target.velocity.y, bullet.speed);
		
		float predictX = entity.target.x + entity.target.velocity.x * hittime,
				predictY = entity.target.y + entity.target.velocity.y * hittime;
		
		Draw.color(Color.GREEN);
		Lines.line(tile.worldx(), tile.worldy(), entity.target.x, entity.target.y);
		
		Draw.color(Color.RED);
		Lines.line(tile.worldx(), tile.worldy(), entity.target.x + entity.target.velocity.x * hittime,
				entity.target.y + entity.target.velocity.y * hittime);
		
		Draw.color(Color.PURPLE);
		Lines.stroke(2f);
		Lines.lineAngle(tile.worldx(), tile.worldy(), angle, 7f);
		
		Draw.reset();
		
		if(Timers.getTime(tile, "reload") <= 0){
			Timers.run(hittime, ()->{
				Effects.effect(Fx.spawn, predictX, predictY);
			});
		}
	}
	
	protected void shoot(Tile tile){
		TurretEntity entity = tile.entity();

		tr.trns(entity.rotation, width * tilesize/2);
		
		for(int i = 0; i < shots; i ++){
			if(Mathf.zero(shotDelayScale)){
				bullet(tile, entity.rotation + Mathf.range(inaccuracy));
			}else{
				Timers.run(i * shotDelayScale, () -> {
					tr.trns(entity.rotation, width * tilesize/2f);
					bullet(tile, entity.rotation + Mathf.range(inaccuracy));
				});
			}
			
		}
		
		if(shootEffect != null){
			Effects.effect(shootEffect, tile.drawx() + tr.x,
				tile.drawy() + tr.y, entity.rotation);
		}
		
		if(shootShake > 0){
			Effects.shake(shootShake, shootShake, tile.entity);
		}
	}
	
	protected void bullet(Tile tile, float angle){
		new Bullet(bullet, tile.entity, tile.drawx() + tr.x, tile.drawy() + tr.y, angle).add();
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
