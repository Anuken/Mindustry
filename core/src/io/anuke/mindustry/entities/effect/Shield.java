package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.ShieldBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.entities.Entity;

public class Shield extends Entity{
	public boolean active;
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
		if(!(tile.block() instanceof ShieldBlock)){
			remove();
		}
	}
	
	@Override
	public void draw(){
		Graphics.surface("shield", false);
		Draw.color(Color.ROYAL);
		Draw.thick(2f);
		Draw.rect("circle2", (int)x + 0.5f, (int)y + 0.5f, 102f, 102f);
		Draw.reset();
		Graphics.surface();
	}
	/*
	@Override
	public void drawOver(){
		Graphics.surface("shield", false);
		Draw.thick(1f);
		Draw.color(Color.SKY);
		Draw.circle(x, y, ((Timers.time() + 50f) % 100f) / 2f);
		Draw.circle(x, y, (Timers.time() % 100f) / 2f);
		Draw.reset();
		Graphics.surface();
	}*/
	
	@Override
	public void added(){
		active = true;
	}
	
	@Override
	public void removed(){
		active = false;
	}
}
