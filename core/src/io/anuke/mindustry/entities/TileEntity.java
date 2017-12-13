package io.anuke.mindustry.entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

public class TileEntity extends Entity{
	public Tile tile;
	public int[] items = new int[Item.values().length];
	public Timer timer;
	public int maxhealth, health;
	public boolean dead = false;
	public boolean added;
	
	/**Sets this tile entity data to this tile, and adds it if necessary.*/
	public TileEntity init(Tile tile, boolean added){
		this.tile = tile;
		this.added = added;
		x = tile.worldx();
		y = tile.worldy();
		
		maxhealth = tile.block().health;
		health = maxhealth;
		
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
	
	public void onDeath(){
		dead = true;
		
		if(tile.block() == ProductionBlocks.core){
			Vars.control.coreDestroyed();
		}
		
		Block block = tile.block();
		
		block.onDestroyed(tile);
		
		Vars.world.removeBlock(tile);
		remove();
	}
	
	public void collision(Bullet other){
		damage(other.getDamage());
	}
	
	public void damage(int damage){
		if(dead) return;
		
		int amount = tile.block().handleDamage(tile, damage);
		health -= amount;
		if(health <= 0) onDeath();
	}
	
	public boolean collide(Bullet other){
		return other.owner instanceof Enemy;
	}
	
	@Override
	public void update(){
		if(health != 0 && health < tile.block().health && !(tile.block() instanceof Wall) &&
				Mathf.chance(0.009f*Timers.delta()*(1f-(float)health/maxhealth))){
			
			Effects.effect(Fx.smoke, x+Mathf.range(4), y+Mathf.range(4));
		}
		
		if(health <= 0){
			onDeath();
		}
		
		tile.block().update(tile);
	}
	
	public int totalItems(){
		int sum = 0;
		for(int i = 0; i < items.length; i ++){
			sum += items[i];
		}
		return sum;
	}
	
	public int getItem(Item item){
		return items[item.ordinal()];
	}
	
	public boolean hasItem(Item item){
		return getItem(item) > 0;
	}
	
	public boolean hasItem(Item item, int amount){
		return getItem(item) >= amount;
	}
	
	public void addItem(Item item, int amount){
		items[item.ordinal()] += amount;
	}
	
	public void removeItem(Item item, int amount){
		items[item.ordinal()] -= amount;;
	}
	
	@Override
	public TileEntity add(){
		return add(Vars.control.tileGroup);
	}
}
