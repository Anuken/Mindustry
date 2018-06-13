package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntIntMap;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Floor extends Block{
	//TODO implement proper bitmasking
	protected static IntIntMap bitmask = Mathf.mapInt(2, 1, 8, 2, 10, 3, 11, 4, 16, 5, 18, 6, 22, 7, 24, 8,
			26, 9, 27, 10, 30, 11, 31, 12, 64, 13, 66, 14, 72, 15, 74, 16, 75, 17, 80, 18,
			82, 19, 86, 20, 88, 21, 90, 22, 91, 23, 94, 24, 95, 25, 104, 26, 106, 27, 107, 28,
			120, 29, 122, 30, 123, 31, 126, 32, 127, 33, 208, 34, 210, 35, 214, 36, 216, 37,
			218, 38, 219, 39, 222, 40, 223, 41, 248, 42, 250, 43, 251, 44, 254, 45, 255, 46, 0, 47);

	protected TextureRegion edgeRegion;
	protected TextureRegion[] edgeRegions;
	protected TextureRegion[] cliffRegions;
	protected Vector2[] offsets;
	protected Predicate<Block> blends = block -> block != this;
	protected boolean blend = true;

    /**edge fallback, used mainly for ores*/
    public String edge = "stone";
	/**Multiplies unit velocity by this when walked on.*/
	public float speedMultiplier = 1f;
	/**Multiplies unit drag by this when walked on.*/
	public float dragMultiplier = 1f;
	/**Damage taken per tick on this tile.*/
	public float damageTaken = 0f;
	/**How many ticks it takes to drown on this.*/
	public float drownTime = 0f;
	/**Effect when walking on this floor.*/
	public Effect walkEffect = BlockFx.ripple;
	/**Effect displayed when drowning on this floor.*/
    public Effect drownUpdateEffect = BlockFx.bubble;
    /**Status effect applied when walking on.*/
	public StatusEffect status = StatusEffects.none;
	/**Intensity of applied status effect.*/
	public float statusIntensity = 0.6f;
	/**Color of this floor's liquid. Used for tinting sprites.*/
	public Color liquidColor;
	/**liquids that drop from this block, used for pumps*/
	public Liquid liquidDrop = null;
	
	public Floor(String name) {
		super(name);
		variants = 3;
	}

	@Override
	public void load() {
		super.load();
		if(blend) {
			edgeRegion = Draw.hasRegion(name + "edge") ? Draw.region(name + "edge") : Draw.region(edge + "edge");
			edgeRegions = new TextureRegion[8];
			offsets = new Vector2[8];

			for(int i = 0; i < 8; i ++){
				int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

				TextureRegion result = new TextureRegion();

				int sx = -dx*8+2, sy = -dy*8+2;
				int x = Mathf.clamp(sx, 0, 12);
				int y = Mathf.clamp(sy, 0, 12);
				int w = Mathf.clamp(sx+8, 0, 12) - x, h = Mathf.clamp(sy+8, 0, 12) - y;

				float rx = Mathf.clamp(dx*8, 0, 8-w);
				float ry = Mathf.clamp(dy*8, 0, 8-h);

				result.setTexture(edgeRegion.getTexture());
				result.setRegion(edgeRegion.getRegionX()+x, edgeRegion.getRegionY()+y+h, w, -h);

				edgeRegions[i] = result;
				offsets[i] = new Vector2(-4 + rx, -4 + ry);
			}

			if(Draw.hasRegion(name + "-cliff")){
				cliffRegions = new TextureRegion[8];
				TextureRegion base = Draw.region(name + "-cliff");

				for(int i = 0; i < 8; i ++){
					int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

					TextureRegion region = new TextureRegion();
					region.setTexture(base.getTexture());
					region.setRegion(base.getRegionX() + tilesize + tilesize*dx, base.getRegionY() + tilesize - tilesize*dy, tilesize, tilesize);
					cliffRegions[i] = region;
				}
			}
		}
	}

	@Override
	public void init(){
		super.init();

		if(liquid && liquidColor == null){
			throw new RuntimeException("All liquids must define a liquidColor! Problematic block: " + name);
		}
	}

	@Override
	public void drawNonLayer(Tile tile){
		MathUtils.random.setSeed(tile.id());

		drawEdges(tile, true);
	}
	
	@Override
	public void draw(Tile tile){
		MathUtils.random.setSeed(tile.id());

		Draw.rect(variants > 0 ? (name() + MathUtils.random(1, variants))  : name(), tile.worldx(), tile.worldy());

		if(Draw.hasRegion(name + "-cliff-side") && tile.cliffs != 0){
			for(int i = 0; i < 4; i ++){
				if((tile.cliffs & (1 << i*2)) != 0) {
					Draw.colorl(i > 1 ? 0.6f : 1f);

					boolean above = (tile.cliffs & (1 << ((i+1)%4)*2)) != 0, below = (tile.cliffs & (1 << (Mathf.mod(i-1, 4))*2)) != 0;

					if(above && below){
						Draw.rect(name + "-cliff-edge-2", tile.worldx(), tile.worldy(), i * 90);
					}else if(above){
						Draw.rect(name + "-cliff-edge", tile.worldx(), tile.worldy(), i * 90);
					}else if(below){
						Draw.rect(name + "-cliff-edge-1", tile.worldx(), tile.worldy(), i * 90);
					}else{
						Draw.rect(name + "-cliff-side", tile.worldx(), tile.worldy(), i * 90);
					}
				}
			}
		}
		Draw.reset();

		drawEdges(tile, false);
	}

	private void drawEdges(Tile tile, boolean sameLayer){
		if(!blend || tile.cliffs > 0) return;

		for(int i = 0; i < 8; i ++){
			int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

			Tile other = world.tile(tile.x+dx, tile.y+dy);

			if(other == null) continue;

			Floor floor = other.floor();

			if(floor.id <= this.id || !blends.test(floor) || (floor.cacheLayer.ordinal() > this.cacheLayer.ordinal() && !sameLayer) ||
					(sameLayer && floor.cacheLayer == this.cacheLayer)) continue;

			TextureRegion region = floor.edgeRegions[i];

			Draw.crect(region, tile.worldx() + floor.offsets[i].x, tile.worldy() + floor.offsets[i].y, region.getRegionWidth(), region.getRegionHeight());
		}
	}

}
