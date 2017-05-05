package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class WeaponBlocks{
	public static Block
	
	turret = new Turret("turret"){
		{
			range = 50;
			reload = 10f;
			bullet = BulletType.stone;
			ammo = Item.stone;
		}
	},
	
	doubleturret = new Turret("doubleturret"){
		{
			range = 40;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Item.stone;
			health = 50;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			vector.set(4, -2).rotate(entity.rotation);
				bullet(tile, entity.rotation);
				
			vector.set(4, 2).rotate(entity.rotation);
				bullet(tile, entity.rotation);
		}
	},
	
	machineturret = new Turret("machineturret"){
		{
			range = 65;
			reload = 7f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 65;
		}
	},
	
	shotgunturret = new Turret("shotgunturret"){
		{
			range = 50;
			reload = 30f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 70;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			for(int i = 0; i < 7; i ++)
				Timers.run(i/1.5f, ()->{
					vector.set(4, 0).setAngle(entity.rotation);
					bullet(tile, entity.rotation + Mathf.range(30));
				});
		}
	},
	
	flameturret = new Turret("flameturret"){
		{
			range = 35f;
			reload = 5f;
			bullet = BulletType.flame;
			ammo = Item.coal;
			health = 90;
		}
	},
	
	sniperturret = new Turret("sniperturret"){
		{
			range = 120;
			reload = 60f;
			bullet = BulletType.sniper;
			ammo = Item.steel;
			health = 60;
		}
	},
	
	repairturret = new RepairTurret("repairturret"){
		{
			range = 30;
			reload = 40f;
			health = 60;
		}
	},
	
	megarepairturret = new RepairTurret("megarepairturret"){
		{
			range = 50;
			reload = 20f;
			health = 90;
		}
	};
}
