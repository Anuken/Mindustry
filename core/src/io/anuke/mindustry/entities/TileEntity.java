package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.mindustry.world.blocks.modules.InventoryModule;
import io.anuke.mindustry.world.blocks.modules.LiquidModule;
import io.anuke.mindustry.world.blocks.modules.PowerModule;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tileGroup;
import static io.anuke.mindustry.Vars.world;

public class TileEntity extends BaseEntity implements TargetTrait {
	public static final float timeToSleep = 60f*4; //4 seconds to fall asleep
	/**This value is only used for debugging.*/
	public static int sleepingEntities = 0;

	public Tile tile;
	public Timer timer;
	public float health;

	public PowerModule power;
	public InventoryModule items;
	public LiquidModule liquids;

	private boolean dead = false;
	private boolean sleeping;
	private float sleepTime;
	
	/**Sets this tile entity data to this tile, and adds it if necessary.*/
	public TileEntity init(Tile tile, boolean added){
		this.tile = tile;
		x = tile.drawx();
		y = tile.drawy();

		health = tile.block().health;
		
		timer = new Timer(tile.block().timers);
		
		if(added){
			//if(!tile.block().autoSleep) { //TODO only autosleep when creating a fresh block!
				add();
			/*}else{
				sleeping = true;
				sleepingEntities ++;
			}*/
		}
		
		return this;
	}

	/**Call when nothing is happening to the entity.
	 * This increments the internal sleep timer.*/
	public void sleep(){
		sleepTime += Timers.delta();
		if(!sleeping && sleepTime >= timeToSleep){
			remove();
			sleeping = true;
			sleepingEntities ++;
		}
	}

	/**Call when something just happened to the entity.
	 * If the entity was sleeping, this enables it. This also resets the sleep timer.*/
	public void wakeUp(){
		sleepTime = 0f;
		if(sleeping){
			add();
			sleeping = false;
			sleepingEntities --;
		}
	}

	public boolean isSleeping(){
		return sleeping;
	}

	public boolean isDead() {
		return dead;
	}

	public void write(DataOutputStream stream) throws IOException{}
	public void read(DataInputStream stream) throws IOException{}

	private void onDeath(){
		if(!dead) {
			dead = true;
			Block block = tile.block();

			block.onDestroyed(tile);
			world.removeBlock(tile);
			block.afterDestroyed(tile, this);
			remove();
		}
	}

	public boolean collide(Bullet other){
		return true;
	}
	
	public void collision(Bullet other){
		tile.block().handleBulletHit(this, other);
	}
	
	public void damage(float damage){
		if(dead) return;

		CallBlocks.onTileDamage(tile, health - tile.block().handleDamage(tile, damage));

		if(health <= 0){
			CallBlocks.onTileDestroyed(tile);
		}
	}

	@Override
	public Team getTeam() {
		return tile.getTeam();
	}

	@Override
	public Vector2 getVelocity() {
		return Vector2.Zero;
	}

	@Override
	public void update(){
		synchronized (Tile.tileSetLock) {
			//TODO better smoke effect, this one is awful
			if (health != 0 && health < tile.block().health && !(tile.block() instanceof Wall) &&
					Mathf.chance(0.009f * Timers.delta() * (1f - health / tile.block().health))) {

				Effects.effect(Fx.smoke, x + Mathf.range(4), y + Mathf.range(4));
			}

			if (health <= 0) {
				onDeath();
			}

			tile.block().update(tile);
		}
	}

	@Override
	public EntityGroup targetGroup() {
		return tileGroup;
	}

	@Remote(called = Loc.server, in = In.blocks)
	public static void onTileDamage(Tile tile, float health){
		tile.entity.health = health;
	}

	@Remote(called = Loc.server, in = In.blocks)
	public static void onTileDestroyed(Tile tile){
		tile.entity.onDeath();
	}
}
