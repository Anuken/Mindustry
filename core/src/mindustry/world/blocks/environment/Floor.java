package mindustry.world.blocks.environment;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class Floor extends Block{
    /** edge fallback, used mainly for ores */
    public String edge = "stone";
    /** Multiplies unit velocity by this when walked on. */
    public float speedMultiplier = 1f;
    /** Multiplies unit drag by this when walked on. */
    public float dragMultiplier = 1f;
    /** Damage taken per tick on this tile. */
    public float damageTaken = 0f;
    /** How many ticks it takes to drown on this. 0 to disable. */
    public float drownTime = 0f;
    /** Effect when walking on this floor. */
    public Effect walkEffect = Fx.none;
    /** Sound made when walking. */
    public Sound walkSound = Sounds.none;
    /** Volume of sound made when walking. */
    public float walkSoundVolume = 0.1f, walkSoundPitchMin = 0.8f, walkSoundPitchMax = 1.2f;
    /** Effect displayed when drowning on this floor. */
    public Effect drownUpdateEffect = Fx.bubble;
    /** Status effect applied when walking on. */
    public StatusEffect status = StatusEffects.none;
    /** Intensity of applied status effect. */
    public float statusDuration = 60f;
    /** liquids that drop from this block, used for pumps. */
    public @Nullable Liquid liquidDrop = null;
    /** Multiplier for pumped liquids, used for deep water. */
    public float liquidMultiplier = 1f;
    /** whether this block is liquid. */
    public boolean isLiquid;
    /** if true, this block cannot be mined by players. useful for annoying things like sand. */
    public boolean playerUnmineable = false;
    /** Group of blocks that this block does not draw edges on. */
    public Block blendGroup = this;
    /** Array of affinities to certain things. */
    public Attributes attributes = new Attributes();
    /** Whether this ore generates in maps by default. */
    public boolean oreDefault = false;
    /** Ore generation params. */
    public float oreScale = 24f, oreThreshold = 0.828f;
    /** Wall variant of this block. May be Blocks.air if not found. */
    public Block wall = Blocks.air;
    /** Decoration block. Usually a rock. May be air. */
    public Block decoration = Blocks.air;
    /** Whether this overlay needs a surface to be on. False for floating blocks, like spawns. */
    public boolean needsSurface = true;

    protected TextureRegion[][] edges;
    protected Seq<Block> blenders = new Seq<>();
    protected Bits blended = new Bits(256);
    protected TextureRegion edgeRegion;

    public Floor(String name){
        super(name);
        variants = 3;
    }

    public Floor(String name, int variants){
        super(name);
        this.variants = variants;
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
    public void init(){
        super.init();

        if(wall == Blocks.air){
            wall = content.block(name + "-wall");
            if(wall == null) wall = content.block(name.replace("darksand", "dune") + "-wall");
        }

        //keep default value if not found...
        if(wall == null) wall = Blocks.air;

        if(decoration == Blocks.air){
            decoration = content.blocks().min(b -> b instanceof Prop && b.minfo.mod == null && b.breakable ? mapColor.diff(b.mapColor) : Float.POSITIVE_INFINITY);
        }

        if(isLiquid && walkEffect == Fx.none){
            walkEffect = Fx.ripple;
        }

        if(isLiquid && walkSound == Sounds.none){
            walkSound = Sounds.splash;
        }
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);
        packer.add(PageType.editor, "editor-" + name, Core.atlas.getPixmap(fullIcon));

        if(blendGroup != this){
            return;
        }

        PixmapRegion image = Core.atlas.getPixmap(icons()[0]);
        PixmapRegion edge = Core.atlas.getPixmap("edge-stencil");
        Pixmap result = new Pixmap(edge.width, edge.height);

        for(int x = 0; x < edge.width; x++){
            for(int y = 0; y < edge.height; y++){
                result.set(x, y, Color.muli(edge.get(x, y), image.get(x % image.width, y % image.height)));
            }
        }

        packer.add(PageType.environment, name + "-edge", result);
    }

    @Override
    public void drawBase(Tile tile){
        Mathf.rand.setSeed(tile.pos());

        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile);

        Floor floor = tile.overlay();
        if(floor != Blocks.air && floor != this){ //ore should never have itself on top, but it's possible, so prevent a crash in that case
            floor.drawBase(tile);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    /** @return whether this floor has a valid surface on which to place things, e.g. scorch marks. */
    public boolean hasSurface(){
        return !isLiquid && !solid;
    }

    public boolean isDeep(){
        return drownTime > 0;
    }

    public void drawNonLayer(Tile tile, CacheLayer layer){
        Mathf.rand.setSeed(tile.pos());

        blenders.clear();
        blended.clear();

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.nearby(point);
            if(other != null && other.floor().cacheLayer == layer && other.floor().edges() != null){
                if(!blended.getAndSet(other.floor().id)){
                    blenders.add(other.floor());
                }
            }
        }

        drawBlended(tile);
    }

    protected void drawEdges(Tile tile){
        blenders.clear();
        blended.clear();

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.nearby(point);
            if(other != null && doEdge(other.floor()) && other.floor().cacheLayer == cacheLayer && other.floor().edges() != null){
                if(!blended.getAndSet(other.floor().id)){
                    blenders.add(other.floor());
                }
            }
        }

        drawBlended(tile);
    }

    protected void drawBlended(Tile tile){
        blenders.sort(a -> a.id);

        for(Block block : blenders){
            for(int i = 0; i < 8; i++){
                Point2 point = Geometry.d8[i];
                Tile other = tile.nearby(point);
                if(other != null && other.floor() == block){
                    TextureRegion region = edge((Floor)block, 1 - point.x, 1 - point.y);
                    Draw.rect(region, tile.worldx(), tile.worldy());
                }
            }
        }
    }

    //'new' style of edges with shadows instead of colors, not used currently
    protected void drawEdgesFlat(Tile tile, boolean sameLayer){
        for(int i = 0; i < 4; i++){
            Tile other = tile.nearby(i);
            if(other != null && doEdge(other.floor())){
                Color color = other.floor().mapColor;
                Draw.color(color.r, color.g, color.b, 1f);
                Draw.rect(edgeRegion, tile.worldx(), tile.worldy(), i*90);
            }
        }
        Draw.color();
    }


    protected TextureRegion[][] edges(){
        return ((Floor)blendGroup).edges;
    }

    protected boolean doEdge(Floor other){
        return other.blendGroup.id > blendGroup.id || edges() == null;
    }

    TextureRegion edge(Floor block, int x, int y){
        return block.edges()[x][2 - y];
    }

}
