package io.anuke.mindustry.world.blocks;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.tilesize;

public class Floor extends Block{
    /** number of different variant regions to use */
    public int variants = 3;
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
    /** liquids that drop from this block, used for pumps */
    public Liquid liquidDrop = null;
    /** item that drops from this block, used for drills */
    public Item itemDrop = null;
    /** whether this block can be drowned in */
    public boolean isLiquid;
    /** if true, this block cannot be mined by players. useful for annoying things like sand. */
    public boolean playerUnmineable = false;
    /** Group of blocks that this block does not draw edges on. */
    public Block blendGroup = this;
    /** Effect displayed when randomly updated. */
    public Effect updateEffect = Fx.none;
    /** Array of affinities to certain things. */
    public Attributes attributes = new Attributes();

    protected TextureRegion[][] edges;
    protected byte eq = 0;
    protected Array<Block> blenders = new Array<>();
    protected IntSet blended = new IntSet();
    protected TextureRegion edgeRegion;

    public Floor(String name){
        super(name);
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
        edgeRegion = Core.atlas.find("edge");
    }

    @Override
    public void createIcons(PixmapPacker out, PixmapPacker editor){
        super.createIcons(out, editor);
        editor.pack("editor-" + name, Core.atlas.getPixmap((AtlasRegion)icon(Cicon.full)).crop());

        if(blendGroup != this){
            return;
        }

        if(variants > 0){
            for(int i = 0; i < variants; i++){
                String rname = name + (i + 1);
                editor.pack("editor-" + rname, Core.atlas.getPixmap(rname).crop());
            }
        }

        Color color = new Color();
        Color color2 = new Color();
        PixmapRegion image = Core.atlas.getPixmap((AtlasRegion)generateIcons()[0]);
        PixmapRegion edge = Core.atlas.getPixmap("edge-stencil");
        Pixmap result = new Pixmap(edge.width, edge.height);

        for(int x = 0; x < edge.width; x++){
            for(int y = 0; y < edge.height; y++){
                edge.getPixel(x, y, color);
                result.draw(x, y, color.mul(color2.set(image.getPixel(x % image.width, y % image.height))));
            }
        }

        out.pack(name + "-edge", result);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    @Override
    public void draw(Tile tile){
        Mathf.random.setSeed(tile.pos());

        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile);

        Floor floor = tile.overlay();
        if(floor != Blocks.air && floor != this){ //ore should never have itself on top, but it's possible, so prevent a crash in that case
            floor.draw(tile);
        }
    }

    public boolean isDeep(){
        return drownTime > 0;
    }

    public void drawNonLayer(Tile tile){
        Mathf.random.setSeed(tile.pos());

        drawEdges(tile, true);
    }

    protected void drawEdges(Tile tile){
        drawEdges(tile, false);
    }

    protected void drawEdges(Tile tile, boolean sameLayer){
        blenders.clear();
        blended.clear();
        eq = 0;

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.getNearby(point);
            if(other != null && doEdge(other.floor(), sameLayer) && other.floor().edges() != null){
                if(blended.add(other.floor().id)){
                    blenders.add(other.floor());
                }
                eq |= (1 << i);
            }
        }

        blenders.sort((a, b) -> Integer.compare(a.id, b.id));

        for(Block block : blenders){
            for(int i = 0; i < 8; i++){
                Point2 point = Geometry.d8[i];
                Tile other = tile.getNearby(point);
                if(other != null && other.floor() == block){
                    TextureRegion region = edge((Floor)block, 2 - (point.x + 1), 2 - (point.y + 1));
                    Draw.rect(region, tile.worldx(), tile.worldy());

                    if(!sameLayer && block.cacheLayer.ordinal() > cacheLayer.ordinal()){
                        Draw.rect(block.variantRegions()[0], tile.worldx() + point.x*tilesize, tile.worldy() + point.y*tilesize);
                    }
                }
            }
        }

    }

    //'new' style of edges with shadows instead of colors, not used currently
    protected void drawEdgesFlat(Tile tile, boolean sameLayer){
        for(int i = 0; i < 4; i++){
            Tile other = tile.getNearby(i);
            if(other != null && doEdge(other.floor(), sameLayer)){
                Color color = other.floor().color;
                Draw.color(color.r, color.g, color.b, 1f);
                Draw.rect(edgeRegion, tile.worldx(), tile.worldy(), i*90);
            }
        }
        Draw.color();
    }


    protected TextureRegion[][] edges(){
        return ((Floor)blendGroup).edges;
    }

    protected boolean doEdge(Floor other, boolean sameLayer){
        return (other.blendGroup.id > blendGroup.id || edges() == null) && other.edgeOnto(this) && (other.cacheLayer.ordinal() > this.cacheLayer.ordinal() || !sameLayer);
    }

    protected boolean edgeOnto(Floor other){
        return true;
    }

    TextureRegion edge(Floor block, int x, int y){
        return block.edges()[x][2 - y];
    }

}