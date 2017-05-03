package io.anuke.mindustry.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Moment;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.TileEntity.ItemPos;
import io.anuke.mindustry.resource.Item;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public enum TileType{
	air{
		//nothing gets drawn
		public void draw(Tile tile){
		}
	},
	grass, 
	stone, 
	dirt, 
	iron, 
	coal, 
	dirtblock(true),
	stoneblock(true), 
	stonewall(true, true){{health = 50;}},
	ironwall(true, true){{health = 80;}},
	steelwall(true, true){{health = 110;}},
	stonedrill(true, true){
		public void update(Tile tile){

			if(tile.floor() == TileType.stone && Timers.get(tile, 60 * 6)){
				offloadNear(tile, Item.stone);
				Effects.effect("spark", tile.x * tilesize, tile.y * tilesize);
			}

			if(Timers.get(tile.hashCode() + "dump", 30)){
				tryDump(tile);
			}
		}

		public String description(){
			return "Mines 1 stone every 6 seconds.";
		}

		public String error(Tile tile){
			if(tile.floor() != TileType.stone)
				return "Not on stone block!";
			return null;
		}
	},
	irondrill(true, true){
		public void update(Tile tile){

			if(tile.floor() == TileType.iron && Timers.get(tile, 60 * 6)){
				offloadNear(tile, Item.iron);
				Effects.effect("spark", tile.x * tilesize, tile.y * tilesize);
			}

			if(Timers.get(tile.hashCode() + "dump", 30)){
				tryDump(tile);
			}
		}

		public String description(){
			return "Mines 1 iron every 6 seconds.";
		}

		public String error(Tile tile){
			if(tile.floor() != TileType.iron)
				return "Not on iron ore block!";
			return null;
		}
	},
	coaldrill(true, true){
		public void update(Tile tile){

			if(tile.floor() == TileType.coal && Timers.get(tile, 60 * 6)){
				offloadNear(tile, Item.coal);
				Effects.effect("spark", tile.x * tilesize, tile.y * tilesize);
			}

			if(Timers.get(tile.hashCode() + "dump", 30)){
				tryDump(tile);
			}
		}

		public String description(){
			return "Mines 1 coal every 6 seconds.";
		}

		public String error(Tile tile){
			if(tile.floor() != TileType.coal)
				return "Not on coal block!";
			return null;
		}
	},
	conveyor(false, true, true){
		float speed = 0.02f;

		public void draw(Tile tile){
			Draw.rect(name() + (Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed ? "" : "move"), tile.worldx(), tile.worldy(), tile.rotation * 90);
			
			vector.set(tilesize, 0).rotate(tile.rotation * 90);
			vector2.set(-tilesize / 2, 0).rotate(tile.rotation * 90);

			for(ItemPos pos : tile.entity.convey){
				Draw.rect("icon-" + pos.item.name(), tile.x * tilesize + vector.x * pos.pos + vector2.x, tile.y * tilesize + vector.y * pos.pos + vector2.y, 4, 4);
			}
		}

		public void update(Tile tile){
			tile.entity.convey.begin();

			for(ItemPos pos : tile.entity.convey){
				pos.pos += speed * Gdx.graphics.getDeltaTime() * 60f;
				if(pos.pos >= 1f && offloadDir(tile, pos.item)){
					tile.entity.removeConvey(pos);
					continue;
				}
				pos.pos = Mathf.clamp(pos.pos);
			}

			tile.entity.convey.end();
		}

		@Override
		public boolean accepts(Item item){
			return true;
		}

		public String description(){
			return "Moves Items";
		}

		@Override
		void handleItem(Tile tile, Item item, float f){
			tile.entity.addConvey(item, f);
		}
	},
	steelconveyor(false, true, true){
		float speed = 0.03f;

		public void draw(Tile tile){
			Draw.rect(name() + (Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed ? "" : "move"), tile.worldx(), tile.worldy(), tile.rotation * 90);

			vector.set(tilesize, 0).rotate(tile.rotation * 90);
			vector2.set(-tilesize / 2, 0).rotate(tile.rotation * 90);

			for(ItemPos pos : tile.entity.convey){
				Draw.rect("icon-" + pos.item.name(), tile.x * tilesize + vector.x * pos.pos + vector2.x, tile.y * tilesize + vector.y * pos.pos + vector2.y, 4, 4);
			}
		}

		public void update(Tile tile){
			tile.entity.convey.begin();

			for(ItemPos pos : tile.entity.convey){
				pos.pos += speed * Gdx.graphics.getDeltaTime() * 60f;
				if(pos.pos >= 1f && offloadDir(tile, pos.item)){
					tile.entity.removeConvey(pos);
					continue;
				}
				pos.pos = Mathf.clamp(pos.pos);
			}

			tile.entity.convey.end();
		}

		@Override
		public boolean accepts(Item item){
			return true;
		}

		public String description(){
			return "Moves Items\nFaster than a normal conveyor";
		}

		@Override
		void handleItem(Tile tile, Item item, float f){
			tile.entity.addConvey(item, f);
		}
	},
	router(true, true, false){

		public void update(Tile tile){
			if(Timers.get(tile, 10) && tile.entity.totalItems() > 0){
				tryDump(tile, tile.rotation++);
				tile.rotation %= 4;
			}
		}

		@Override
		public boolean accepts(Item item){
			return true;
		}

		public String description(){
			return "Splits conveyor belt input";
		}
	},
	smelter(true, true, false){
		{health=70;}

		public void update(Tile tile){
			
			if(tile.entity.hasItem(Item.coal) && tile.entity.hasItem(Item.iron)){
				tile.entity.removeItem(Item.coal, 1);
				tile.entity.removeItem(Item.iron, 1);
				offloadNear(tile, Item.steel);
				Effects.effect("smelt", tile.entity);
			}
			
			if(Timers.get(tile, 20) && tile.entity.hasItem(Item.steel)){
				tryDump(tile);
			}
		}

		@Override
		public boolean accepts(Item item){
			return item == Item.iron || item == Item.coal;
		}

		public String description(){
			return "Smelts iron and coal into steel";
		}
	},
	core(true, true, false){
		{
			health = 300;
		}

		@Override
		void handleItem(Tile tile, Item item, float f){
			Moment.i.addItem(item, 1);
		}

		@Override
		public boolean accepts(Item item){
			return true;
		}
	},
	turret(true, true, false){
		{
			range = 40;
			reload = 10f;
			bullet = BulletType.stone;
			ammo = Item.stone;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Shoots things.";
		}
	},
	doubleturret(true, true, false){
		{
			range = 40;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Item.stone;
			health = 50;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Shoots things.";
		}
		
		@Override
		void shoot(Tile tile){
			vector.set(4, -2).rotate(tile.entity.rotation);
				bullet(tile, tile.entity.rotation);
			vector.set(4, 2).rotate(tile.entity.rotation);
				bullet(tile, tile.entity.rotation);
		}
	},
	machineturret(true, true, false){
		{
			range = 65;
			reload = 7f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 65;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Shoots things.";
		}
	},
	flameturret(true, true, false){
		{
			range = 35f;
			reload = 5f;
			bullet = BulletType.flame;
			ammo = Item.coal;
			health = 85;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Burns things.";
		}
	},
	sniperturret(true, true, false){
		{
			range = 100;
			reload = 60f;
			bullet = BulletType.sniper;
			ammo = Item.steel;
			health = 60;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Shoots things.";
		}
	},
	shotgunturret(true, true, false){
		{
			range = 50;
			reload = 40f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 70;
		}

		public void update(Tile tile){
			updateTurret(tile);
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Shoots things.";
		}
		
		@Override
		void shoot(Tile tile){
			
			for(int i = 0; i < 6; i ++)
				Timers.run(i/1.5f, ()->{
					vector.set(4, 0).setAngle(tile.entity.rotation);
					bullet(tile, tile.entity.rotation + Mathf.range(16));
				});
				
		}
	},
	repairturret(true, true, false){
		{
			range = 30;
			reload = 40f;
			health = 50;
		}

		public void update(Tile tile){
			/*
			 * if(tile.entity.hasItem(ammo)){ tile.entity.shots += 20;
			 * tile.entity.removeItem(ammo, 1); }
			 */
			//if(tile.entity.shots > 0){
			tile.entity.link = findTileTarget(tile.worldx(), tile.worldy(), tile, range, true);

			if(tile.entity.link != null){
				tile.entity.rotation = tile.entity.angleTo(tile.entity.link);

				if(Timers.get(tile, reload)){
					tile.entity.link.health++;
				}
			}
			//}
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			if(tile.entity.link != null){
				float x = tile.worldx(), y = tile.worldy();
				float x2 = tile.entity.link.x, y2 = tile.entity.link.y;

				Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 14f));
				Draw.alpha(0.3f);
				Draw.thickness(4f);
				Draw.line(x, y, x2, y2);
				Draw.thickness(2f);
				Draw.rect("circle", x2, y2, 7f, 7f);
				Draw.alpha(1f);
				Draw.thickness(2f);
				Draw.line(x, y, x2, y2);
				Draw.thickness(1f);
				Draw.rect("circle", x2, y2, 5f, 5f);
				Draw.clear();
			}
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Heals nearby tiles.";
		}
	},
	megarepairturret(true, true, false){
		{
			range = 30;
			reload = 20f;
			health = 80;
		}

		public void update(Tile tile){
			tile.entity.link = findTileTarget(tile.worldx(), tile.worldy(), tile, range, true);

			if(tile.entity.link != null){
				tile.entity.rotation = tile.entity.angleTo(tile.entity.link);

				if(Timers.get(tile, reload)){
					tile.entity.link.health++;
				}
			}
		}

		public void draw(Tile tile){
			Draw.rect("block", tile.worldx(), tile.worldy());
		}

		public void drawOver(Tile tile){
			if(tile.entity.link != null){
				float x = tile.worldx(), y = tile.worldy();
				float x2 = tile.entity.link.x, y2 = tile.entity.link.y;

				Draw.color(Hue.rgb(132, 242, 242, (MathUtils.sin(Timers.time()) + 1f) / 13f));
				Draw.alpha(0.3f);
				Draw.thickness(4f);
				Draw.line(x, y, x2, y2);
				Draw.thickness(2f);
				Draw.rect("circle", x2, y2, 7f, 7f);
				Draw.alpha(1f);
				Draw.thickness(2f);
				Draw.line(x, y, x2, y2);
				Draw.thickness(1f);
				Draw.rect("circle", x2, y2, 5f, 5f);
				Draw.clear();
			}
			Draw.rect(name(), tile.worldx(), tile.worldy(), tile.entity.rotation - 90);
		}

		public String description(){
			return "Heals nearby tiles.";
		}
	};
	static Vector2 vector = new Vector2();
	static Vector2 vector2 = new Vector2();

	public boolean solid, update, rotate;
	public static final int tilesize = 8;
	public int health = 40;

	//turrets
	public float range = 50f;
	public float reload = 10f;
	public BulletType bullet;
	public Item ammo;

	private TileType() {
		solid = false;
	}

	private TileType(boolean solid) {
		this.solid = solid;
	}

	private TileType(boolean solid, boolean update) {
		this.solid = solid;
		this.update = update;
	}

	private TileType(boolean solid, boolean update, boolean rotate) {
		this.solid = solid;
		this.update = update;
		this.rotate = rotate;
	}

	public void init(TileEntity entity){

	}

	void updateTurret(Tile tile){
		if(tile.entity.hasItem(ammo)){
			tile.entity.shots += 20;
			tile.entity.removeItem(ammo, 1);
		}
		
		if(tile.entity.shots > 0){
			Enemy enemy = findTarget(tile, range);
			if(enemy != null){
				tile.entity.rotation = MathUtils.lerpAngleDeg(tile.entity.rotation, Angles.predictAngle(tile.worldx(), tile.worldy(), enemy.x, enemy.y, enemy.xvelocity, enemy.yvelocity, bullet.speed - 0.1f), 0.2f);
				if(Timers.get(tile, reload)){
					Sounds.play("shoot");
					shoot(tile);
					tile.entity.shots--;
				}
			}
		}
	}
	
	void shoot(Tile tile){
		vector.set(0, 4).setAngle(tile.entity.rotation);
		new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, tile.entity.rotation).add();
	}
	
	void bullet(Tile tile, float angle){
		new Bullet(bullet, tile.entity, tile.worldx()+vector.x, tile.worldy()+vector.y, angle).add();
	}

	Enemy findTarget(Tile tile, float range){
		Entity closest = null;
		float dst = 0;

		Array<SolidEntity> array = Entities.getNearby(tile.worldx(), tile.worldy(), range*2);

		for(Entity e : array){

			if(e instanceof Enemy){
				float ndst = Vector2.dst(tile.worldx(), tile.worldy(), e.x, e.y);
				if(ndst < range && (closest == null || ndst < dst)){
					dst = ndst;
					closest = e;
				}
			}
		}

		return (Enemy) closest;
	}

	public static TileEntity findTileTarget(float x, float y, Tile tile, float range, boolean damaged){
		Entity closest = null;
		float dst = 0;
		
		int rad = (int)(range/tilesize)+1;
		int tilex = Mathf.scl2(x, tilesize);
		int tiley = Mathf.scl2(y, tilesize);
		
		for(int rx = -rad; rx <= rad; rx ++){
			for(int ry = -rad; ry <= rad; ry ++){
				Tile other = Moment.i.tile(rx+tilex, ry+tiley);
				
				if(other == null || other.entity == null ||(tile != null && other.entity == tile.entity)) continue;
				
				TileEntity e = other.entity;
				
				if(damaged && ((TileEntity) e).health >= ((TileEntity) e).tile.block().health)
					continue;
				
				float ndst = Vector2.dst(x, y, e.x, e.y);
				if(ndst < range && (closest == null || ndst < dst)){
					dst = ndst;
					closest = e;
				}
			}
		}

		return (TileEntity) closest;
	}

	void handleItem(Tile tile, Item item, float f){
		tile.entity.addItem(item, 1);
	}

	/**
	 * Tries to put this item into a nearby container, if there are no available
	 * containers, it gets added to the block's inventory.
	 */
	void offloadNear(Tile tile, Item item){
		int i = 0;
		for(Tile other : tile.getNearby()){
			if(other != null && other.block().accepts(item)
					//don't output to things facing this thing
					&& !(other.block().rotate && (other.rotation + 2) % 4 == i)){
				
				other.block().handleItem(other, item, 0);
				return;
			}
			i++;
		}
		handleItem(tile, item, 0);
	}

	/** Try dumping any item near the tile. */
	boolean tryDump(Tile tile){
		return tryDump(tile, -1);
	}

	/**
	 * Try dumping any item near the tile. -1 = any direction
	 */
	boolean tryDump(Tile tile, int direction){
		int i = 0;
		
		for(Tile other : tile.getNearby()){
			if(i == direction || direction == -1)
				for(Item item : Item.values()){
					
					if(tile.entity.hasItem(item) && other != null && other.block().accepts(item) &&
					//don't output to things facing this thing
							!(other.block().rotate && (other.rotation + 2) % 4 == i)){
						other.block().handleItem(other, item, 0);
						tile.entity.removeItem(item, 1);
						return true;
					}
				}
			i++;
		}

		return false;
	}

	/**
	 * Try offloading an item to a nearby container. Returns true if success.
	 */
	boolean offloadDir(Tile tile, Item item){
		Tile other = tile.getNearby()[tile.rotation];
		if(other != null && other.block().accepts(item)){
			int ch = Math.abs(other.rotation - tile.rotation);
			other.block().handleItem(other, item, ch == 1 ? 0.5f : ch == 2 ? 1f : 0f);
			//other.entity.addCovey(item, ch == 1 ? 0.5f : ch ==2 ? 1f : 0f);
			return true;
		}
		return false;
	}

	public boolean accepts(Item item){
		return item == ammo;
	}

	public String description(){
		return "[no description]";
	}

	public void draw(Tile tile){
		if(tile.floor() == this){
			MathUtils.random.setSeed(tile.id());
			Draw.rect(name() + MathUtils.random(1, 3), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}else{
			Draw.rect(name(), tile.worldx(), tile.worldy(), rotate ? tile.rotation * 90 : 0);
		}
	}

	public String error(Tile tile){
		if(ammo != null && !tile.entity.hasItem(ammo) && tile.entity.shots <= 0)
			return "No ammo!";

		return null;
	}

	public void drawOver(Tile tile){
		/*
		 * String error = error(tile); if(error != null){ Draw.color("scarlet");
		 * Draw.square(tile.worldx(), tile.worldy(), 6); Draw.clear(); }
		 */
	}

	public void update(Tile tile){

	}
}
