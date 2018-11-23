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
import io.anuke.ucore.function.BiPredicate;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Structs;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

public class Floor extends Block{
    //TODO implement proper bitmasking
    protected static IntIntMap bitmask = Structs.mapInt(2, 1, 8, 2, 10, 3, 11, 4, 16, 5, 18, 6, 22, 7, 24, 8,
            26, 9, 27, 10, 30, 11, 31, 12, 64, 13, 66, 14, 72, 15, 74, 16, 75, 17, 80, 18,
            82, 19, 86, 20, 88, 21, 90, 22, 91, 23, 94, 24, 95, 25, 104, 26, 106, 27, 107, 28,
            120, 29, 122, 30, 123, 31, 126, 32, 127, 33, 208, 34, 210, 35, 214, 36, 216, 37,
            218, 38, 219, 39, 222, 40, 223, 41, 248, 42, 250, 43, 251, 44, 254, 45, 255, 46, 0, 47);
    /** number of different variant regions to use */
    public int variants;
    /** edge fallback, used mainly for ores */
    public String edge = "stone";
    /** Multiplies unit velocity by this when walked on. */
    public float speedMultiplier = 1f;
    /** Multiplies unit drag by this when walked on. */
    public float dragMultiplier = 1f;
    /** Damage taken per tick on this tile. */
    public float damageTaken = 0f;
    /** How many ticks it takes to drown on this. */
    public float drownTime = 0f;
    /** Effect when walking on this floor. */
    public Effect walkEffect = BlockFx.ripple;
    /** Effect displayed when drowning on this floor. */
    public Effect drownUpdateEffect = BlockFx.bubble;
    /** Status effect applied when walking on. */
    public StatusEffect status = StatusEffects.none;
    /** Intensity of applied status effect. */
    public float statusIntensity = 0.6f;
    /** Color of this floor's liquid. Used for tinting sprites. */
    public Color liquidColor;
    /** liquids that drop from this block, used for pumps */
    public Liquid liquidDrop = null;
    /** Whether ores generate on this block. */
    public boolean hasOres = false;
    /** whether this block can be drowned in */
    public boolean isLiquid;
    /** if true, this block cannot be mined by players. useful for annoying things like stone. */
    public boolean playerUnmineable = false;
    protected TextureRegion edgeRegion;
    protected TextureRegion[] edgeRegions;
    protected TextureRegion[] cliffRegions;
    protected TextureRegion[] variantRegions;
    protected Vector2[] offsets;
    protected Predicate<Floor> blends = block -> block != this && !block.blendOverride(this);
    protected BiPredicate<Tile, Tile> tileBlends = (tile, other) -> false;
    protected boolean blend = true;

    public Floor(String name){
        super(name);
        variants = 3;
    }

    @Override
    public void load(){
        super.load();

        if(blend){
            edgeRegion = Draw.hasRegion(name + "edge") ? Draw.region(name + "edge") : Draw.region(edge + "edge");
            edgeRegions = new TextureRegion[8];
            offsets = new Vector2[8];

            for(int i = 0; i < 8; i++){
                int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

                TextureRegion result = new TextureRegion();

                int sx = -dx * 8 + 2, sy = -dy * 8 + 2;
                int x = Mathf.clamp(sx, 0, 12);
                int y = Mathf.clamp(sy, 0, 12);
                int w = Mathf.clamp(sx + 8, 0, 12) - x, h = Mathf.clamp(sy + 8, 0, 12) - y;

                float rx = Mathf.clamp(dx * 8, 0, 8 - w);
                float ry = Mathf.clamp(dy * 8, 0, 8 - h);

                result.setTexture(edgeRegion.getTexture());
                result.setRegion(edgeRegion.getRegionX() + x, edgeRegion.getRegionY() + y + h, w, -h);

                edgeRegions[i] = result;
                offsets[i] = new Vector2(-4 + rx, -4 + ry);
            }

            cliffRegions = new TextureRegion[4];
            cliffRegions[0] = Draw.region(name + "-cliff-edge-2");
            cliffRegions[1] = Draw.region(name + "-cliff-edge");
            cliffRegions[2] = Draw.region(name + "-cliff-edge-1");
            cliffRegions[3] = Draw.region(name + "-cliff-side");
        }

        //load variant regions for drawing
        if(variants > 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Draw.region(name + (i + 1));
            }
        }else{
            variantRegions = new TextureRegion[1];
            variantRegions[0] = Draw.region(name);
        }
    }

    @Override
    public void init(){
        super.init();

        if(isLiquid && liquidColor == null){
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

        Draw.rect(variantRegions[Mathf.randomSeed(tile.id(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        if(tile.hasCliffs() && cliffRegions != null){
            for(int i = 0; i < 4; i++){
                if((tile.getCliffs() & (1 << i * 2)) != 0){
                    Draw.colorl(i > 1 ? 0.6f : 1f);

                    boolean above = (tile.getCliffs() & (1 << ((i + 1) % 4) * 2)) != 0, below = (tile.getCliffs() & (1 << (Mathf.mod(i - 1, 4)) * 2)) != 0;

                    if(above && below){
                        Draw.rect(cliffRegions[0], tile.worldx(), tile.worldy(), i * 90);
                    }else if(above){
                        Draw.rect(cliffRegions[1], tile.worldx(), tile.worldy(), i * 90);
                    }else if(below){
                        Draw.rect(cliffRegions[2], tile.worldx(), tile.worldy(), i * 90);
                    }else{
                        Draw.rect(cliffRegions[3], tile.worldx(), tile.worldy(), i * 90);
                    }
                }
            }
        }
        Draw.reset();

        drawEdges(tile, false);
    }

    public boolean blendOverride(Block block){
        return false;
    }

    protected void drawEdges(Tile tile, boolean sameLayer){
        if(!blend || tile.getCliffs() > 0) return;

        for(int i = 0; i < 8; i++){
            int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

            Tile other = tile.getNearby(dx, dy);

            if(other == null) continue;

            Floor floor = other.floor();

            if(floor.edgeRegions == null || (floor.id <= this.id && !(tile.getElevation() != -1 && other.getElevation() > tile.getElevation())) || (!blends.test(floor) && !tileBlends.test(tile, other)) || (floor.cacheLayer.ordinal() > this.cacheLayer.ordinal() && !sameLayer) ||
                    (sameLayer && floor.cacheLayer == this.cacheLayer)) continue;

            TextureRegion region = floor.edgeRegions[i];

            Draw.crect(region, tile.worldx() + floor.offsets[i].x, tile.worldy() + floor.offsets[i].y, region.getRegionWidth(), region.getRegionHeight());
        }
    }

}