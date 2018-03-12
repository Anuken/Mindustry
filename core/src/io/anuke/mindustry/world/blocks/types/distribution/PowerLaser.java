package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.generation.Generator;
import io.anuke.ucore.graphics.Draw;

public class PowerLaser extends Generator{
	public Color color = Color.valueOf("e54135");
	public String base = null;

	public PowerLaser(String name) {
		super(name);
		rotate = true;
		solid = true;
		explosive = false;
		laserDirections = 1;
		health = 50;
		hasInventory = false;
	}

	@Override
	public TextureRegion[] getIcon(){
		String bname = base == null ? "laser-base" : base;
		return new TextureRegion[]{Draw.region(bname), Draw.region(name)};
	}

	@Override
	public void draw(Tile tile) {
		Draw.rect(base == null ? "laser-base" : base, tile.drawx(), tile.drawy());
		Draw.rect(name(), tile.drawx(), tile.drawy(), tile.getRotation() * 90 - 90);
	}

	@Override
	public void update(Tile tile){
		distributeLaserPower(tile);
	}
	
	@Override
	public boolean acceptPower(Tile tile, Tile from, float amount){
		PowerEntity entity = tile.entity();
		
		return entity.power.amount <= powerCapacity;
	}
}
