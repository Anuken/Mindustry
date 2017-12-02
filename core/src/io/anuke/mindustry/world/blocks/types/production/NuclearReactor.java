package io.anuke.mindustry.world.blocks.types.production;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class NuclearReactor extends LiquidItemPowerGenerator{
	protected Color coolColor = new Color(1, 1, 1, 0f);
	protected Color hotColor = Color.valueOf("ff9575a3");
	protected int fuelUseTime = 120; //time to consume 1 fuel
	protected float powerMultiplier = 0.08f; //power per frame, depends on full capacity 
	protected float heating = 0.007f; //heating per frame
	protected float coolantPower = 0.007f; //how much heat decreases per coolant unit
	protected float smokeThreshold = 0.3f; //threshold at which block starts smoking
	protected int explosionRadius = 19;
	protected int explosionDamage = 128;
	protected float flashThreshold = 0.46f;
	
	public NuclearReactor(String name) {
		super(name);
		generateItem = Item.uranium;
		generateLiquid = Liquid.water;
		itemCapacity = 30;
		liquidCapacity = 50;
		explosionEffect = Fx.nuclearShockwave;
		explosive = true;
		powerCapacity = 80f;
	}
	
	@Override
	public void update(Tile tile){
		NuclearReactorEntity entity = tile.entity();
		
		int fuel = entity.getItem(generateItem);
		float fullness = (float)fuel / itemCapacity;
		
		if(fuel > 0){
			entity.heat += fullness * heating;
			entity.power += powerMultiplier * fullness;
			if(Timers.get(tile, "fuelRemove", fuelUseTime)){
				entity.removeItem(generateItem, 1);
			}
		}
		
		if(entity.liquidAmount > 0){
			float maxCool = Math.min(entity.liquidAmount * coolantPower, entity.heat);
			entity.heat -= maxCool; //TODO steam when cooling large amounts?
			entity.liquidAmount -= maxCool / coolantPower;
		}
		
		if(entity.heat > smokeThreshold){
			float smoke = 1.0f + (entity.heat - smokeThreshold) / (1f - smokeThreshold); //ranges from 1.0 to 2.0
			if(Mathf.chance(smoke / 20.0 * Timers.delta())){
				Effects.effect(Fx.reactorsmoke, tile.worldx() + Mathf.range(width * Vars.tilesize / 2f),
						tile.worldy() + Mathf.random(height * Vars.tilesize / 2f));
			}
		}
		
		if(entity.heat >= 1f){
			onDestroyed(tile);
			Vars.world.removeBlock(tile);
			
		}else{
			distributeLaserPower(tile);
		}
	}
	
	@Override
	public void drawLiquidCenter(Tile tile){
		Vector2 offset = getPlaceOffset();
		Draw.rect(name + "-center", tile.worldx() + offset.x, tile.worldy() + offset.y);
	}
	
	@Override
	public void onDestroyed(Tile tile){
		super.onDestroyed(tile);
		
		NuclearReactorEntity entity = tile.entity();
		
		int fuel = entity.getItem(generateItem);
		
		if(fuel < 5 && entity.heat < 0.5f) return;
		
		int waves = 6;
		float delay = 8f;
		
		for(int i = 0; i < waves; i ++){
			float rad = (float)i /waves * explosionRadius;
			Timers.run(i * delay, ()->{
				tile.damageNearby((int)rad, explosionDamage / waves, 0.4f);
			});
		}
		
		Effects.shake(6f, 16f, tile.worldx(), tile.worldy());
		Effects.effect(explosionEffect, tile.worldx(), tile.worldy());
		for(int i = 0; i < 6; i ++){
			Timers.run(Mathf.random(40), ()->{
				Effects.effect(Fx.nuclearcloud, tile.worldx(), tile.worldy());
			});
		}
		
		DamageArea.damageEntities(tile.worldx(), tile.worldy(), explosionRadius * Vars.tilesize, explosionDamage * 4);
		
		
		for(int i = 0; i < 20; i ++){
			Timers.run(Mathf.random(50), ()->{
				Tmp.v1.setToRandomDirection().setLength(Mathf.random(40f));
				Effects.effect(Fx.explosion, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
			});
		}
		
		for(int i = 0; i < 70; i ++){
			Timers.run(Mathf.random(80), ()->{
				Tmp.v1.setToRandomDirection().setLength(Mathf.random(120f));
				Effects.effect(Fx.nuclearsmoke, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
			});
		}
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		super.drawPixelOverlay(tile);
		
		NuclearReactorEntity entity = tile.entity();
		
		float fract = entity.heat;
		if(fract > 0)
			fract = Mathf.clamp(fract + 0.2f, 0.24f, 1f);
		
		Vector2 offset = getPlaceOffset();
		
		Vars.renderer.drawBar(Color.ORANGE, tile.worldx() + offset.x, 
				tile.worldy() + Vars.tilesize * height/2f + 10 + offset.y, fract);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		NuclearReactorEntity entity = tile.entity();
		Vector2 offset = getPlaceOffset();
		
		Draw.color(coolColor, hotColor, entity.heat);
		Draw.rect("white", tile.worldx() + offset.x, tile.worldy() + offset.y, width * Vars.tilesize, height * Vars.tilesize);
		
		if(entity.heat > flashThreshold){
			float flash = 1f + ((entity.heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
			entity.flash += flash * Timers.delta();
			Draw.color(Color.RED, Color.YELLOW, Mathf.absin(entity.flash, 9f, 1f));
			Draw.alpha(0.6f);
			Draw.rect(name + "-lights", tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		Draw.reset();
	}
	
	@Override
	public TileEntity getEntity(){
		return new NuclearReactorEntity();
	}
	
	public static class NuclearReactorEntity extends LiquidPowerEntity{
		public float heat;
		public float flash;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeFloat(heat);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			heat = stream.readFloat();
		}
	}
}
