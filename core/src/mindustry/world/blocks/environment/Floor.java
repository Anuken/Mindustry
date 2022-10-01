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

import java.util.*;

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
    /** for liquid floors, this is the opacity of the overlay drawn on top. */
    public float overlayAlpha = 0.65f;
    /** whether this floor supports an overlay floor */
    public boolean supportsOverlay = false;
    /** shallow water flag used for generation */
    public boolean shallow = false;
    /** Group of blocks that this block does not draw edges on. */
    public Block blendGroup = this;
    /** Whether this ore generates in maps by default. */
    public boolean oreDefault = false;
    /** Ore generation params. */
    public float oreScale = 24f, oreThreshold = 0.828f;
    /** Wall variant of this block. May be Blocks.air if not found. */
    public Block wall = Blocks.air;
    /** Decoration block. Usually a rock. May be air. */
    public Block decoration = Blocks.air;
    /** Whether units can draw shadows over this. */
    public boolean canShadow = true;
    /** Whether this overlay needs a surface to be on. False for floating blocks, like spawns. */
    public boolean needsSurface = true;
    /** If true, cores can be placed on this floor. */
    public boolean allowCorePlacement = false;
    /** If true, this ore is allowed on walls. */
    public boolean wallOre = false;
    /** Actual ID used for blend groups. Internal. */
    public int blendId = -1;

    protected TextureRegion[][] edges;
    protected Seq<Block> blenders = new Seq<>();
    protected Bits blended = new Bits(256);
    protected int[] dirs = new int[8];
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

        blendId = blendGroup.id;

        if(wall == Blocks.air){
            wall = content.block(name + "-wall");
            if(wall == null) wall = content.block(name.replace("darksand", "dune") + "-wall");
        }

        //keep default value if not found...
        if(wall == null) wall = Blocks.air;

        //try to load the default boulder
        if(decoration == null){
            decoration = content.block(name + "-boulder");
        }

        if(isLiquid && walkEffect == Fx.none){
            walkEffect = Fx.ripple;
        }

        if(isLiquid && walkSound == Sounds.none){
            walkSound = Sounds.splash;
        }
    }

    @Override
    public TextureRegion getDisplayIcon(Tile tile){
        return liquidDrop == null ? super.getDisplayIcon(tile) : liquidDrop.uiIcon;
    }

    @Override
    public String getDisplayName(Tile tile){
        return liquidDrop == null ? super.getDisplayName(tile) : liquidDrop.localizedName;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);
        packer.add(PageType.editor, "editor-" + name, Core.atlas.getPixmap(fullIcon));

        if(blendGroup != this){
            return;
        }

        var image = Core.atlas.getPixmap(icons()[0]);
        var edge = Core.atlas.getPixmap(Core.atlas.find(name + "-edge-stencil", "edge-stencil"));
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

        Draw.alpha(1f);
        drawEdges(tile);
        drawOverlay(tile);
    }

    public void drawOverlay(Tile tile){
        Floor floor = tile.overlay();
        if(floor != Blocks.air && floor != this){
            if(isLiquid){
                Draw.alpha(overlayAlpha);
            }
            floor.drawBase(tile);
            if(isLiquid){
                Draw.alpha(1f);
            }
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    //TODO currently broken for dynamically edited floor tiles
    /** @return true if this floor should be updated in the render loop, e.g. for effects. Do NOT overuse this! */
    public boolean updateRender(Tile tile){
        return false;
    }

    public void renderUpdate(UpdateRenderState tile){

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

        Arrays.fill(dirs, 0);
        blenders.clear();
        blended.clear();

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.nearby(point);
            if(other != null && other.floor().cacheLayer == layer && other.floor().edges() != null){
                if(!blended.getAndSet(other.floor().id)){
                    blenders.add(other.floor());
                    dirs[i] = other.floorID();
                }
            }
        }

        drawBlended(tile, false);
    }

    protected void drawEdges(Tile tile){
        blenders.clear();
        blended.clear();
        Arrays.fill(dirs, 0);
        CacheLayer realCache = tile.floor().cacheLayer;

        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile other = tile.nearby(point);

            if(other != null && doEdge(tile, other, other.floor()) && other.floor().cacheLayer == realCache && other.floor().edges() != null){

                if(!blended.getAndSet(other.floor().id)){
                    blenders.add(other.floor());
                }
                dirs[i] = other.floorID();
            }
        }

        drawBlended(tile, true);
    }

    protected void drawBlended(Tile tile, boolean checkId){
        blenders.sort(a -> a.id);

        for(Block block : blenders){
            for(int i = 0; i < 8; i++){
                Point2 point = Geometry.d8[i];
                Tile other = tile.nearby(point);
                if(other != null && other.floor() == block && (!checkId || dirs[i] == block.id)){
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
            if(other != null && doEdge(tile, other, other.floor())){
                Color color = other.floor().mapColor;
                Draw.color(color.r, color.g, color.b, 1f);
                Draw.rect(edgeRegion, tile.worldx(), tile.worldy(), i*90);
            }
        }
        Draw.color();
    }

    public int realBlendId(Tile tile){
        if(tile.floor().isLiquid && !tile.overlay().isAir() && !(tile.overlay() instanceof OreBlock)){
            return -((tile.overlay().blendId) | (tile.floor().blendId << 15));
        }
        return blendId;
    }

    protected TextureRegion[][] edges(){
        return ((Floor)blendGroup).edges;
    }

    protected boolean doEdge(Tile tile, Tile otherTile, Floor other){
        return other.realBlendId(otherTile) > realBlendId(tile) || edges() == null;
    }

    TextureRegion edge(Floor block, int x, int y){
        return block.edges()[x][2 - y];
    }

    public static class UpdateRenderState{
        public Tile tile;
        public Floor floor;
        public float data;

        public UpdateRenderState(Tile tile, Floor floor){
            this.tile = tile;
            this.floor = floor;
        }
    }

}
