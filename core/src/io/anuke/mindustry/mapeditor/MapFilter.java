package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.Simplex;
import io.anuke.ucore.util.Mathf;

public class MapFilter{
	private ObjectMap<String, Boolean> prefs = new OrderedMap<String, Boolean>(){
		{
			put("replace", true);
			put("terrain", false);
			put("circle", false);
			put("distort", false);
			put("sand", false);
			put("grass", false);
			put("stone", false);
			put("allgrass", false);
			put("allsnow", false);
			put("allsand", false);
			put("lavarock", false);
			put("water", false);
			put("oil", false);
			put("lavariver", false);
			put("slavariver", false);
			put("river", false);
			put("iceriver", false);
			put("oilriver", false);
		}
	};
	private Simplex sim = new Simplex();
	private RidgedPerlin rid = new RidgedPerlin(1, 10, 20f);
	private RidgedPerlin rid2 = new RidgedPerlin(1, 6, 1f);
	private RidgedPerlin rid3 = new RidgedPerlin(1, 6, 1f);
	
	public void randomize(){
		sim.setSeed(Mathf.random(999999));
		rid.setSeed(Mathf.random(999999));
		rid2.setSeed(Mathf.random(999999));
		rid3.setSeed(Mathf.random(999999));
	}
	
	public ObjectMap<String, Boolean> getPrefs(){
		return prefs;
	}
	
	public Pixmap process(Pixmap pixmap){
		if(prefs.get("terrain")){
			for(int x = 0; x < pixmap.getWidth(); x++){
				for(int y = 0; y < pixmap.getHeight(); y++){
					float dist = Vector2.dst((float) x / pixmap.getWidth(), (float) y / pixmap.getHeight(), 0.5f, 0.5f) * 2f;
					double noise = sim.octaveNoise2D(6, 0.6, 1 / 180.0, x, y + 9999) / (prefs.get("circle") ? 1.7 : 1f) + dist / 10f;

					if(dist > 0.8){
						noise += 2 * (dist - 0.8);
					}

					Block block = noise > 0.6 ? Blocks.stoneblock : Blocks.stone;

					pixmap.drawPixel(x, y, ColorMapper.getColor(block));
				}
			}
		}

		Pixmap src = Pixmaps.copy(pixmap);

		for(int x = 0; x < pixmap.getWidth(); x++){
			for(int y = 0; y < pixmap.getHeight(); y++){
				int dx = 0, dy = 0;

				if(prefs.get("distort")){
					double intensity = 12;
					double scale = 80;
					double octaves = 4;
					double falloff = 0.6;
					double nx = (sim.octaveNoise2D(octaves, falloff, 1 / scale, x, y) - 0.5f) * intensity;
					double ny = (sim.octaveNoise2D(octaves, falloff, 1 / scale, x, y + 99999) - 0.5f) * intensity;
					dx = (int) nx;
					dy = (int) ny;
				}

				int pix = src.getPixel(x + dx, y + dy);

				BlockPair pair = ColorMapper.get(pix);
				Block block = pair == null ? null : pair.wall == Blocks.air ? pair.floor : pair.wall;

				if(block == null)
					continue;

				boolean floor = block instanceof Floor;

				double noise = sim.octaveNoise2D(4, 0.6, 1 / 170.0, x, y) + sim.octaveNoise2D(1, 1.0, 1 / 5.0, x, y) / 18.0;
				double nwater = sim.octaveNoise2D(1, 1.0, 1 / 130.0, x, y);
				noise += nwater / 5.0;

				double noil = sim.octaveNoise2D(1, 1.0, 1 / 150.0, x + 9999, y) + sim.octaveNoise2D(1, 1.0, 1 / 2.0, x, y) / 290.0;

				if(!floor || prefs.get("replace")){
					
					if(prefs.get("allgrass")){
						block = floor ? Blocks.grass : Blocks.grassblock;
					}else if(prefs.get("allsnow")){
						block = floor ? Blocks.snow : Blocks.snowblock;
					}else if(prefs.get("allsand")){
						block = floor ? Blocks.sand : Blocks.sandblock;
					}else if(prefs.get("replace")){
						block = floor ? Blocks.stone : Blocks.stoneblock;
					}
						
					if(noise > 0.7 && prefs.get("grass")){
						block = floor ? Blocks.grass : Blocks.grassblock;
					}
					if(noise > 0.7 && prefs.get("lavarock")){
						block = floor ? Blocks.blackstone : Blocks.blackstoneblock;
					}
					if(noise > 0.7 && prefs.get("sand")){
						block = floor ? Blocks.sand : Blocks.sandblock;
					}
					if(noise > 0.8 && prefs.get("stone")){
						block = floor ? Blocks.stone : Blocks.stoneblock;
					}
				}

				if(floor){
					if(nwater > 0.93 && prefs.get("water")){
						block = Blocks.water;
						if(nwater > 0.943){
							block = Blocks.deepwater;
						}
					}

					if(noil > 0.95 && prefs.get("oil")){
						block = Blocks.dirt;
						if(noil > 0.955){
							block = Blocks.oil;
						}
					}
				}

				if(floor && prefs.get("lavariver")){
					double lava = rid.getValue(x, y, 1 / 100f);
					double t = 0.6;
					if(lava > t){
						block = Blocks.lava;
					}else if(lava > t - 0.2){
						block = Blocks.blackstone;
					}
				}
				
				if(floor && prefs.get("slavariver")){
					double lava = rid.getValue(x, y, 1 / 40f);
					double t = 0.7;
					if(lava > t){
						block = Blocks.lava;
					}else if(lava > t - 0.3){
						block = Blocks.blackstone;
					}
				}

				if(floor && prefs.get("oilriver")){
					double lava = rid3.getValue(x, y, 1 / 100f);
					double t = 0.9;
					if(lava > t){
						block = Blocks.oil;
					}else if(lava > t - 0.2){
						block = Blocks.dirt;
					}
				}

				if(floor && prefs.get("river")){
					double riv = rid2.getValue(x, y, 1 / 140f);
					double t = 0.4;

					if(riv > t + 0.1){
						block = Blocks.deepwater;
					}else if(riv > t){
						block = Blocks.water;
					}else if(riv > t - 0.2){
						block = Blocks.grass;
					}
				}
				
				if(floor && prefs.get("iceriver")){
					double riv = rid2.getValue(x, y, 1 / 140f);
					double t = 0.4;

					if(riv > t + 0.1){
						block = Blocks.ice;
					}else if(riv > t){
						block = Blocks.ice;
					}else if(riv > t - 0.2){
						block = Blocks.snow;
					}
				}

				pixmap.drawPixel(x, y, ColorMapper.getColor(block));
			}
		}

		src.dispose();

		return pixmap;
	}
}
