package io.anuke.moment.entities;

import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.moment.Moment;
import io.anuke.moment.ai.Pathfind;
import io.anuke.moment.resource.Item;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.Effects;
import io.anuke.ucore.entities.SolidEntity;

public class TileEntity extends DestructibleEntity{
	public final Tile tile;
	public DelayedRemovalArray<ItemPos> convey = new DelayedRemovalArray<>();
	public ObjectMap<Item, Integer> items = new ObjectMap<>();
	public int shots;
	public TileEntity link;
	public float rotation;
	
	public TileEntity(Tile tile){
		this.tile = tile;
		x = tile.worldx();
		y = tile.worldy();
		hitsize = TileType.tilesize;
		
		maxhealth = tile.block().health;
		heal();
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && ((Bullet)other).owner instanceof Enemy;
	}
	
	@Override
	public void onDeath(){
		if(tile.block() == TileType.core){
			Moment.i.coreDestroyed();
		}
		tile.setBlock(TileType.air);
		Pathfind.updatePath();
		Effects.shake(4f, 4f);
		Effects.effect("explosion", this);
	}
	
	@Override
	public void update(){
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
	
	public void addConvey(Item item){
		addConvey(item, 0);
	}
	
	public void addConvey(Item item, float pos){
		ItemPos posa = new ItemPos(item);
		posa.pos = pos;
		convey.add(posa);
	}
	
	public void removeConvey(ItemPos pos){
		convey.removeValue(pos, true);
	}
	
	static public class ItemPos{
		public Item item;
		public float pos;
		
		public ItemPos(Item item){
			this.item = item;
		}
	}
}
