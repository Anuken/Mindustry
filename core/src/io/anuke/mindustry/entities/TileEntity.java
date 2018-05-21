package io.anuke.mindustry.entities;

import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.modules.InventoryModule;
import io.anuke.mindustry.world.blocks.types.modules.LiquidModule;
import io.anuke.mindustry.world.blocks.types.modules.PowerModule;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tileGroup;
import static io.anuke.mindustry.Vars.world;

public class TileEntity extends Entity{
	public static final float timeToSleep = 60f*4; //4 seconds to fall asleep
	public static int sleepingEntities = 0;

	public Tile tile;
	public Timer timer;
	public float health;
	public boolean dead = false;

	public PowerModule power;
	public InventoryModule items;
	public LiquidModule liquids;

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
			add(tileGroup);
			sleeping = false;
			sleepingEntities --;
		}
	}
	
	public void write(DataOutputStream stream) throws IOException{}
	public void read(DataInputStream stream) throws IOException{}
	
	public void onDeath(){
		onDeath(false);
	}

	public void onDeath(boolean force){
		if(Net.server()){
			NetEvents.handleBlockDestroyed(this);
		}

		if(!Net.active() || Net.server() || force){

			if(!dead) {
				dead = true;
				Block block = tile.block();

				block.onDestroyed(tile);
				world.removeBlock(tile);
				block.afterDestroyed(tile, this);
				remove();
			}
		}
	}

	public boolean collide(Bullet other){
		return true;
	}
	
	public void collision(Bullet other){
		damage(other.getDamage());
	}
	
	public void damage(float damage){
		if(dead) return;
		
		float amount = tile.block().handleDamage(tile, damage);
		health -= amount;
		if(health <= 0) onDeath();

		if(Net.server()){
			NetEvents.handleBlockDamaged(this);
		}
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
	public TileEntity add(){
		return add(tileGroup);
	}
}
