package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class Floor extends Block{
	protected TextureRegion edgeRegion;
	protected TextureRegion[] edgeRegions;
	protected Vector2[] offsets;
	protected Predicate<Block> blends = block -> block != this;
	protected boolean blend = true;

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

		drawEdges(tile, false);
	}

	private void drawEdges(Tile tile, boolean sameLayer){
		if(!blend) return;

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
