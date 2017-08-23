package io.anuke.mindustry.entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;

public class TileEntity extends Entity{
	public Tile tile;
	public ObjectMap<Item, Integer> items = new ObjectMap<>();
	public int maxhealth, health;
	public boolean dead = false;
	
	public TileEntity init(Tile tile){
		this.tile = tile;
		x = tile.worldx();
		y = tile.worldy();
		
		maxhealth = tile.block().health;
		health = maxhealth;
		
		return this;
	}
	
	public void write(DataOutputStream stream){
		
	}
	
	public void read(TileEntity entity, DataInputStream stream){
		
	}
	
	public void onDeath(){
		dead = true;
		
		if(tile.block() == ProductionBlocks.core){
			Vars.control.coreDestroyed();
		}
		
		tile.setBlock(Blocks.air);
		Effects.shake(4f, 4f);
		Effects.effect("explosion", this);
		
		Effects.sound("break", this);
	}
	
	public void collision(Bullet other){
		health -= other.getDamage();
		if(health <= 0) onDeath();
	}
	
	public boolean collide(Bullet other){
		return other.owner instanceof Enemy;
	}
	
	@Override
	public void update(){
		if(health != 0 && !tile.block().name().contains("block") &&
				Mathf.chance(0.009f*delta*(1f-(float)health/maxhealth))){
			
			Effects.effect("smoke", x+Mathf.range(4), y+Mathf.range(4));
		}
		
		tile.block().update(tile);
	}
	
	public int totalItems(){
		int sum = 0;
		for(Item item : Item.values()){
			sum += items.get(item, 0);
		}
		return sum;
	}
	
	public boolean hasItem(Item item){
		return items.get(item, 0) > 0;
	}
	
	public void addItem(Item item, int amount){
		items.put(item, items.get(item, 0) + amount);
	}
	
	public void removeItem(Item item, int amount){
		items.put(item, items.get(item, 0) - amount);
	}
}
