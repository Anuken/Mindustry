package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.ShieldBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;

public class Shield extends Entity{
	public boolean active;
	private float uptime = 0f;
	private final Tile tile;
	//TODO
	
	public Shield(Tile tile){
		this.tile = tile;
		this.x = tile.worldx();
		this.y = tile.worldy();
	}
	
	public float drawSize(){
		return 150;
	}
	
	@Override
	public void update(){
		if(active){
			uptime += Timers.delta() / 90f;
		}else{
			uptime -= Timers.delta() / 60f;
			if(uptime < 0)
				remove();
		}
		uptime = Mathf.clamp(uptime);
		
		if(!(tile.block() instanceof ShieldBlock)){
			remove();
			return;
		}
		
		ShieldBlock block = (ShieldBlock)tile.block();
		
		Entities.getNearby(x, y, block.shieldRadius * 2*uptime + 10, entity->{
			if(entity instanceof BulletEntity){
				BulletEntity bullet = (BulletEntity)entity;
				
				float dst =  entity.distanceTo(this);
				
				if(Math.abs(dst - block.shieldRadius) < 2){
					bullet.velocity.scl(-1);
				}
			}
		});
	}
	
	@Override
	public void draw(){
		if(!(tile.block() instanceof ShieldBlock)){
			return;
		}
		
		ShieldBlock block = (ShieldBlock)tile.block();
		
		float rad = block.shieldRadius*2 + Mathf.sin(Timers.time(), 25f, 2f);
		rad *= uptime;
		
		Graphics.surface("shield", false);
		Draw.color(Color.ROYAL);
		Draw.thick(2f);
		Draw.rect("circle2", x, y, rad, rad);
		Draw.reset();
		Graphics.surface();
	}
	
	public void removeDelay(){
		active = false;
	}
	
	@Override
	public void added(){
		active = true;
	}
	
	@Override
	public void removed(){
		active = false;
		uptime = 0f;
	}
	
}
