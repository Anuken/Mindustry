package io.anuke.mindustry.world.blocks;

import io.anuke.arc.Core;
import io.anuke.arc.entities.Effects;
import io.anuke.arc.entities.Effects.Effect;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.mindustry.content.Blocks;
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
    /** Heat of this block, 0 at baseline. Used for calculating output of thermal generators.*/
    public float heat = 0f;
    /** if true, this block cannot be mined by players. useful for annoying things like sand. */
    public boolean playerUnmineable = false;
    /**Style of the edge stencil. Loaded by looking up "edge-stencil-{name}".*/
    public String edgeStyle = "smooth";
    /**Group of blocks that this block does not draw edges on.*/
    public Block blendGroup = this;
    /**Effect displayed when randomly updated.*/
    public Effect updateEffect = Fx.none;

    protected TextureRegion[][] edges;
    protected byte eq = 0;

    public Floor(String name){
        super(name);
        variants = 3;
    }

    @Override
    public void load(){
        super.load();

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

        int size = (int)(tilesize / Draw.scl);
        if(Core.atlas.has(name + "-edge")){
            edges = Core.atlas.find(name + "-edge").split(size, size);
        }
        region = variantRegions[0];
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    @Override
    public void randomUpdate(Tile tile){
        if(tile.block() == Blocks.air){
            Effects.effect(updateEffect, tile.worldx() + Mathf.range(tilesize/2f), tile.worldy() + Mathf.range(tilesize/2f));
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
    public void draw(Tile tile){
        Mathf.random.setSeed(tile.pos());

        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile);
    }

    protected void drawEdges(Tile tile){
        eq = 0;

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.getNearby(point);
            if(other != null && doEdge(other.floor()) && other.floor().edges() != null){
                eq |= (1 << i);
            }
        }

        for(int i = 0; i < 8; i++){
            if(eq(i)){
                Point2 point = Geometry.d8[i];
                Tile other = tile.getNearby(point);

                TextureRegion region = edge(other.floor(), type(i), 2-(point.x + 1), 2-(point.y + 1));
                Draw.rect(region, tile.worldx(), tile.worldy());
            }
        }
    }

    protected TextureRegion[][] edges(){
        return ((Floor)blendGroup).edges;
    }

    protected boolean doEdge(Floor other){
        return (other.blendGroup.id > id || edges() == null) && other.edgeOnto(this);
    }

    protected boolean edgeOnto(Floor other){
        return true;
    }

    int type(int i){
        if(!eq(i - 1) && !eq(i + 1)){
            //case 0: touching
            return 0;
        }else if(eq(i - 1) && eq(i - 2) && eq(i + 1) && eq(i + 2)){
            //case 2: surrounded
            return 2;
        }else if(eq(i - 1) && eq(i + 1)){
            //case 1: flat
            return 1;
        }else{
            //case 0 is rounded, so it's the safest choice, should work for most possibilities
            return 0;
        }
    }

    boolean eq(int i){
        return (eq & (1 << Mathf.mod(i, 8))) != 0;
    }

    TextureRegion edge(Floor block, int type, int x, int y){
        return block.edges()[x + type*3][2-y];
    }

}