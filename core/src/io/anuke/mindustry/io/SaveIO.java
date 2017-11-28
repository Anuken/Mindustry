package io.anuke.mindustry.io;

import static io.anuke.mindustry.Vars.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.*;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.Entities;

/*
 * Save format:
 * 
 * --META--
 * 
 * Save file version ID (int)
 * Last saved timestamp (long)
 * 
 * --STATE DATA--
 * Wave (int)
 * Wave countdown time (float)
 * 
 * Gamemode Ordinal (byte)
 * Map ordinal (byte)
 * 
 * Player X (float)
 * Player Y (float)
 * Player health (int)
 * 
 * Amount of weapons (byte)
 * (weapon list)
 *   Weapon enum ordinal (byte)
 * 
 * Amount of items (byte)
 * (item list) 
 *   Item ID (byte)
 *   Item amount (int)
 * 
 * --ENEMY DATA--
 * Amount of enemies (int)
 * (enemy list)
 *   enemy type ID (byte)
 *   spawn lane (byte)
 *   x (float)
 *   y (float)
 *   tier (byte)
 *   health (int)
 *   
 * 
 * --MAP DATA--
 * Seed (int)
 * Amount of tiles (int)
 * (tile list)
 *   Tile position, as a single integer, in the format x+y*width
 *   Tile link - (byte)
 *   Tile type (boolean)- whether the block has a tile entity attached
 *   Block ID - the block ID (byte)
 *   (the following only applies to tile entity blocks)
 *   Block rotation (byte)
 *   Block health (int)
 *   Amount of items (byte)
 *   (item list) 
 *     Item ID (byte)
 *     Item amount (int)
 *   Additional tile entity data (varies, check the TileEntity classes)
 */
public class SaveIO{
	/**Save file version ID. Should be incremented every breaking release.*/
	private static final int fileVersionID = 11;
	
	//TODO automatic registration of types?
	private static final Array<Class<? extends Enemy>> enemyIDs = Array.with(
		Enemy.class,
		FastEnemy.class,
		RapidEnemy.class,
		FlamerEnemy.class,
		TankEnemy.class,
		BlastEnemy.class,
		MortarEnemy.class,
		TestEnemy.class,
		HealerEnemy.class,
		TitanEnemy.class,
		EmpEnemy.class
	);
	
	private static final ObjectMap<Class<? extends Enemy>, Byte> idEnemies = new ObjectMap<Class<? extends Enemy>, Byte>(){{
		for(int i = 0; i < enemyIDs.size; i ++){
			put(enemyIDs.get(i), (byte)i);
		}
	}};
	
	public static void saveToSlot(int slot){
		write(fileFor(slot));
	}
	
	public static void loadFromSlot(int slot){
		load(fileFor(slot));
	}
	
	public static boolean isSaveValid(int slot){
		try(DataInputStream stream = new DataInputStream(fileFor(slot).read())){
			return stream.readInt() == fileVersionID;
		}catch (Exception e){
			return false;
		}
	}
	
	public static String getTimeString(int slot){
		
		try(DataInputStream stream = new DataInputStream(fileFor(slot).read())){
			stream.readInt();
			Date date = new Date(stream.readLong());
			return Mindustry.formatter.format(date);
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static int getWave(int slot){
		
		try(DataInputStream stream = new DataInputStream(fileFor(slot).read())){
			stream.readInt(); //read version
			stream.readLong(); //read last saved time
			stream.readByte(); //read the gamemode
			stream.readByte(); //read the map
			return stream.readInt(); //read the wave
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static GameMode getMode(int slot){
		
		try(DataInputStream stream = new DataInputStream(fileFor(slot).read())){
			stream.readInt(); //read version
			stream.readLong(); //read last saved time
			return GameMode.values()[stream.readByte()]; //read the gamemode
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static Map getMap(int slot){
		
		try(DataInputStream stream = new DataInputStream(fileFor(slot).read())){
			stream.readInt(); //read version
			stream.readLong(); //read last saved time
			stream.readByte(); //read the gamemode
			return Map.values()[stream.readByte()]; //read the map
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static FileHandle fileFor(int slot){
		return Gdx.files.local("mindustry-saves/" + slot + ".mins");
	}
	
	public static void write(FileHandle file){
		
		try(DataOutputStream stream = new DataOutputStream(file.write(false))){
			
			//--META--
			stream.writeInt(fileVersionID); //version id
			stream.writeLong(TimeUtils.millis()); //last saved
			
			//--GENERAL STATE--
			stream.writeByte(Vars.control.getMode().ordinal()); //gamemode
			stream.writeByte(Vars.world.getMap().ordinal()); //map ID
			
			stream.writeInt(Vars.control.getWave()); //wave
			stream.writeFloat(Vars.control.getWaveCountdown()); //wave countdown
			
			stream.writeFloat(Vars.player.x); //player x/y
			stream.writeFloat(Vars.player.y);
			
			stream.writeInt(Vars.player.health); //player health
			
			stream.writeByte(Vars.control.getWeapons().size - 1); //amount of weapons
			
			//start at 1, because the first weapon is always the starter - ignore that
			for(int i = 1; i < Vars.control.getWeapons().size; i ++){
				stream.writeByte(Vars.control.getWeapons().get(i).ordinal()); //weapon ordinal
			}
			
			//--INVENTORY--
			
			stream.writeByte(Vars.control.getItems().size); //amount of items
			
			for(Item item : Vars.control.getItems().keys()){
				stream.writeByte(item.ordinal()); //item ID
				stream.writeInt(Vars.control.getAmount(item)); //item amount
			}
			
			//--ENEMIES--
			
			int totalEnemies = 0;
			
			for(Enemy entity : Entities.get(Enemy.class)){
				if(idEnemies.containsKey(entity.getClass())){
					totalEnemies ++;
				}
			}
			
			stream.writeInt(totalEnemies); //enemy amount
			
			for(Enemy enemy :  Entities.get(Enemy.class)){
				if(idEnemies.containsKey(enemy.getClass())){
					stream.writeByte(idEnemies.get(enemy.getClass())); //type
					stream.writeByte(enemy.spawn); //lane
					stream.writeFloat(enemy.x); //x
					stream.writeFloat(enemy.y); //y
					stream.writeByte(enemy.tier); //tier
					stream.writeInt(enemy.health); //health
				}
			}
			
			//--MAP DATA--
			
			//seed
			stream.writeInt(Vars.world.getSeed());
			
			int totalblocks = 0;
			
			for(int x = 0; x < Vars.world.width(); x ++){
				for(int y = 0; y < Vars.world.height(); y ++){
					Tile tile = Vars.world.tile(x, y);
					
					if(tile.breakable()){
						totalblocks ++;
					}
				}
			}
			
			//tile amount
			stream.writeInt(totalblocks);
			
			for(int x = 0; x < Vars.world.width(); x ++){
				for(int y = 0; y < Vars.world.height(); y ++){
					Tile tile = Vars.world.tile(x, y);
					
					if(tile.breakable()){
						
						stream.writeInt(x + y*Vars.world.width()); //tile pos
						stream.writeByte(tile.link);
						stream.writeBoolean(tile.entity != null); //whether it has a tile entity
						stream.writeInt(tile.block().id); //block ID
						
						if(tile.entity != null){
							stream.writeByte(tile.getRotation()); //rotation
							stream.writeInt(tile.entity.health); //health
							stream.writeByte(tile.entity.items.size); //amount of items
							
							for(Item item : tile.entity.items.keys()){
								stream.writeByte(item.ordinal()); //item ID
								stream.writeInt(tile.entity.items.get(item)); //item amount
							}
							
							tile.entity.write(stream);
						}
					}
				}
			}
			
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static void load(FileHandle file){
		
		try(DataInputStream stream = new DataInputStream(file.read())){
			Item[] itemEnums = Item.values();
			
			int version = stream.readInt();
			/*long loadTime = */stream.readLong();
			
			if(version != fileVersionID){
				//TODO throw an exception?
			}
			
			//general state
			byte mode = stream.readByte();
			byte mapid = stream.readByte();
			
			int wave = stream.readInt();
			float wavetime = stream.readFloat();
			
			float playerx = stream.readFloat();
			float playery = stream.readFloat();
			
			int playerhealth = stream.readInt();
			
			Vars.player.x = playerx;
			Vars.player.y = playery;
			Vars.player.health = playerhealth;
			Vars.control.setMode(GameMode.values()[mode]);
			Core.camera.position.set(playerx, playery, 0);
			
			//weapons
			
			Vars.control.getWeapons().clear();
			Vars.control.getWeapons().add(Weapon.blaster);
			
			int weapons = stream.readByte();
			
			for(int i = 0; i < weapons; i ++){
				Vars.control.addWeapon(Weapon.values()[stream.readByte()]);
			}
			
			Vars.ui.updateWeapons();
			
			//inventory
			
			int totalItems = stream.readByte();
			
			Vars.control.getItems().clear();
			
			for(int i = 0; i < totalItems; i ++){
				Item item = itemEnums[stream.readByte()];
				int amount = stream.readInt();
				Vars.control.getItems().put(item, amount);
			}
			
			Vars.ui.updateItems();
			
			//enemies
			
			Entities.clear();
			
			int enemies = stream.readInt();
			
			Array<Enemy> enemiesToUpdate = new Array<>();
			
			for(int i = 0; i < enemies; i ++){
				byte type = stream.readByte();
				int lane = stream.readByte();
				float x = stream.readFloat();
				float y = stream.readFloat();
				byte tier = stream.readByte();
				int health = stream.readInt();
				
				try{
					Enemy enemy = (Enemy)ClassReflection.getConstructor(enemyIDs.get(type), int.class).newInstance(lane);
					enemy.health = health;
					enemy.x = x;
					enemy.y = y;
					enemy.tier = tier;
					enemy.add(Entities.getGroup(Enemy.class));
					enemiesToUpdate.add(enemy);
				}catch (Exception e){
					throw new RuntimeException(e);
				}
			}
			
			Vars.control.setWaveData(enemies, wave, wavetime);
			
			if(!android)
				Vars.player.add();
			
			//map
			
			int seed = stream.readInt();
			int tiles = stream.readInt();
			
			Vars.world.loadMap(Map.values()[mapid], seed);
			Vars.renderer.clearTiles();
			
			for(Enemy enemy : enemiesToUpdate){
				enemy.node = -2;
			}
			
			for(int x = 0; x < Vars.world.width(); x ++){
				for(int y = 0; y < Vars.world.height(); y ++){
					Tile tile = Vars.world.tile(x, y);
					
					//remove breakables like rocks
					if(tile.breakable()){
						Vars.world.tile(x, y).setBlock(Blocks.air);
					}
				}
			}
			
			for(int i = 0; i < tiles; i ++){
				int pos = stream.readInt();
				byte link = stream.readByte();
				boolean hasEntity = stream.readBoolean();
				int blockid = stream.readInt();
				
				Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());
				tile.setBlock(Block.getByID(blockid));
				tile.link = link;
				
				if(hasEntity){
					byte rotation = stream.readByte();
					int health = stream.readInt();
					int items = stream.readByte();
					
					tile.entity.health = health;
					tile.setRotation(rotation);
					
					for(int j = 0; j < items; j ++){
						int itemid = stream.readByte();
						int itemamount = stream.readInt();
						tile.entity.items.put(itemEnums[itemid], itemamount);
					}
					
					tile.entity.read(stream);
				}
			}
			
		}catch (IOException e){
			throw new RuntimeException(e);
		}
	}
}
