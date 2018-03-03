package io.anuke.mindustry.entities;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Wall;
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
	public Tile tile;
	public int[] items = new int[Item.getAllItems().size];
	public Timer timer;
	public float health;
	public boolean dead = false;
	public boolean added;
	
	/**Sets this tile entity data to this tile, and adds it if necessary.*/
	public TileEntity init(Tile tile, boolean added){
		this.tile = tile;
		this.added = added;
		x = tile.worldx();
		y = tile.worldy();

		health = tile.block().health;
		
		timer = new Timer(tile.block().timers);
		
		if(added){
			add();
		}
		
		return this;
	}
	
	public void write(DataOutputStream stream) throws IOException{
		
	}
	
	public void read(DataInputStream stream) throws IOException{
		
	}

	public void readNetwork(DataInputStream stream, float elapsed) throws IOException{
		read(stream);
	}
	
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
				remove();
			}
		}
	}
	
	public void collision(Bullet other){
		damage(other.getDamage());
	}
	
	public void damage(int damage){
		if(dead) return;
		
		int amount = tile.block().handleDamage(tile, damage);
		health -= amount;
		if(health <= 0) onDeath();

		if(Net.server()){
			NetEvents.handleBlockDamaged(this);
		}
	}
	
	public boolean collide(Bullet other){
		return true;
	}
	
	@Override
	public void update(){
		synchronized (Tile.tileSetLock) {
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
	
	public int totalItems(){
		int sum = 0;
		for(int i = 0; i < items.length; i ++){
			sum += items[i];
		}
		return sum;
	}
	
	public int getItem(Item item){
		return items[item.id];
	}
	
	public boolean hasItem(Item item){
		return getItem(item) > 0;
	}
	
	public boolean hasItem(Item item, int amount){
		return getItem(item) >= amount;
	}
	
	public void addItem(Item item, int amount){
		items[item.id] += amount;
	}
	
	public void removeItem(Item item, int amount){
		items[item.id] -= amount;
	}
	
	@Override
	public TileEntity add(){
		return add(tileGroup);
	}
}
