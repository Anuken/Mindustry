package io.anuke.mindustry.world.blocks;

import io.anuke.arc.Core;
import io.anuke.arc.entities.Effects.Effect;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;

public class Floor extends Block{
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
    public Effect walkEffect = Fx.ripple;
    /** Effect displayed when drowning on this floor. */
    public Effect drownUpdateEffect = Fx.bubble;
    /** Status effect applied when walking on. */
    public StatusEffect status = StatusEffects.none;
    /** Intensity of applied status effect. */
    public float statusDuration = 60f;
    /** Color of this floor's liquid. Used for tinting sprites. */
    public Color liquidColor;
    /** liquids that drop from this block, used for pumps */
    public Liquid liquidDrop = null;
    /** item that drops from this block, used for drills */
    public Item itemDrop = null;
    /** Whether ores generate on this block. */
    public boolean hasOres = false;
    /** whether this block can be drowned in */
    public boolean isLiquid;
    /** if true, this block cannot be mined by players. useful for annoying things like sand. */
    public boolean playerUnmineable = false;
    protected TextureRegion edgeRegion;
    protected TextureRegion[] edgeRegions;
    protected TextureRegion[] variantRegions;
    protected Vector2[] offsets;
    protected Predicate<Floor> blends = block -> block != this && !block.blendOverride(this);
    protected boolean blend = true;

    public Floor(String name){
        super(name);
        variants = 3;
    }

    @Override
    public void load(){
        super.load();

        if(blend){
            edgeRegion = Core.atlas.has(name + "edge") ? Core.atlas.find(name + "edge") : Core.atlas.find(edge + "edge");
            edgeRegions = new TextureRegion[8];
            offsets = new Vector2[8];

            for(int i = 0; i < 8; i++){
                int dx = Geometry.d8[i].x, dy = Geometry.d8[i].y;

                TextureRegion result = new TextureRegion();

                int padSize = (int)(tilesize/Draw.scl/2);
                int texSize = (int)(tilesize/Draw.scl);
                int totSize = padSize + texSize;

                int sx = -dx * texSize + padSize/2, sy = -dy * texSize + padSize/2;
                int x = Mathf.clamp(sx, 0, totSize);
                int y = Mathf.clamp(sy, 0, totSize);
                int w = Mathf.clamp(sx + texSize, 0, totSize) - x, h = Mathf.clamp(sy + texSize, 0, totSize) - y;

                float rx = Mathf.clamp(dx * texSize, 0, texSize - w);
                float ry = Mathf.clamp(dy * texSize, 0, texSize - h);

                result.setTexture(edgeRegion.getTexture());
                result.set(edgeRegion.getX() + x, edgeRegion.getY() + y + h, w, -h);

                edgeRegions[i] = result;
                offsets[i] = new Vector2(-padSize + rx, -padSize + ry);
            }
        }

        //load variant regions for drawing
        if(variants > 0){
            variantRegions = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegions[i] = Core.atlas.find(name + (i + 1));
            }
        }else{
            variantRegions = new TextureRegion[1];
            variantRegions[0] = Core.atlas.find(name);
        }

        region = variantRegions[0];
    }

    @Override
    public void init(){
        super.init();

        if(isLiquid && liquidColor == null){
            throw new RuntimeException("All liquids must define a liquidColor! Problematic block: " + name);
        }
    }

    @Override
    public void draw(Tile tile){
        Mathf.random.setSeed(tile.pos());

        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        //drawEdges(tile, false);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    public boolean blendOverride(Block block){
        return false;
    }

}