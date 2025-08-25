package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class TileableLogicDisplay extends LogicDisplay{
    protected static final Seq<TileableLogicDisplayBuild> queue = new Seq<>();
    protected static final Seq<TileableLogicDisplayBuild> displays = new Seq<>();
    protected static final ObjectSet<FrameBuffer> buffers = new ObjectSet<>();
    protected static final IntSet processed = new IntSet();

    //in tiles
    public int maxDisplayDimensions = 16;
    public @Load(value = "@-#", length = 47) TextureRegion[] tileRegion;
    public @Load("@-back") TextureRegion backRegion;

    public TileableLogicDisplay(String name){
        super(name);

        displaySize = 32;
    }

    public static void linkDisplays(TileableLogicDisplayBuild start){
        TileableLogicDisplayBuild root = null;

        int topX = start.tile.x, topY = start.tile.y, botX = start.tile.x, botY = start.tile.y;

        queue.clear();
        displays.clear();
        processed.clear();

        queue.add(start);
        displays.add(start);

        while(!queue.isEmpty()){
            var next = queue.pop();
            processed.add(next.id);

            //assign root based on bottom leftmost position
            if(root == null || next.tile.x < root.tile.x || next.tile.y < root.tile.y){
                root = next;
            }

            topX = Math.max(next.tile.x, topX);
            topY = Math.max(next.tile.y, topY);
            botX = Math.min(next.tile.x, botX);
            botY = Math.min(next.tile.y, botY);

            for(var prox : next.proximity){
                if(prox instanceof TileableLogicDisplayBuild disp && processed.add(disp.id)){
                    queue.add(disp);
                    displays.add(disp);
                }
            }
        }

        if(root.prevBuffers == null){
            root.prevBuffers = new Seq<>();
        }

        //add all new buffers
        buffers.clear();
        for(var member : displays){
            if(member.buffer != null && buffers.add(member.buffer)){
                root.prevBuffers.add(new MergeBuffer(member.buffer, member.originX, member.originY, member.tilesWidth, member.tilesHeight));
            }
        }

        int tilesWidth = topX - botX + 1, tilesHeight = topY - botY + 1;

        //the new root display has been assigned
        for(var member : displays){
            member.needsUpdate = false;
            member.rootDisplay = root;
            member.tilesWidth = tilesWidth;
            member.tilesHeight = tilesHeight;
            member.originX = botX;
            member.originY = botY;
            member.buffer = null;
        }
    }

    static class MergeBuffer{
        FrameBuffer buffer;
        int x, y, width, height;

        MergeBuffer(FrameBuffer buffer, int x, int y, int width, int height){
            this.buffer = buffer;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString(){
            return "MergeBuffer{" +
            "x=" + x +
            ", y=" + y +
            ", width=" + width +
            ", height=" + height +
            '}';
        }
    }

    public class TileableLogicDisplayBuild extends LogicDisplayBuild{
        //size of display area
        public int tilesWidth = 1, tilesHeight = 1, originX, originY;
        public @Nullable Seq<MergeBuffer> prevBuffers;

        public int bits = 0;
        public boolean needsUpdate = false;

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case displayWidth -> tilesWidth * 32f - 12f;    // accounts for display frame (2 * 6 pixels)
                case displayHeight -> tilesHeight * 32f - 12f;
                default -> super.sense(sensor);
            };
        }

        @Override
        public void display(Table table){
            super.display(table);

            if(tilesWidth > maxDisplayDimensions || tilesHeight > maxDisplayDimensions){
                table.row().add(Core.bundle.format("bar.displaytoolarge", maxDisplayDimensions, maxDisplayDimensions)).color(Color.scarlet).growX().wrap();
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            bits = 0;

            for(int i = 0; i < 8; i++){
                Tile other = tile.nearby(Geometry.d8(i));
                if(other != null && other.block() == block && other.team() == team){
                    bits |= (1 << i);
                }
            }
        }

        @Override
        public void draw(){
            //TODO if this is called before draw() on the root display is called, it will wipe it
            if(needsUpdate){
                needsUpdate = false;
                linkDisplays(this);
            }

            Draw.rect(backRegion, x, y);

            //don't even bother processing anything when displays are off.
            if(!Vars.renderer.drawDisplays) return;

            if(isRoot()){
                Draw.draw(Draw.z(), () -> {
                    if(buffer == null && tilesWidth <= maxDisplayDimensions && tilesHeight <= maxDisplayDimensions){
                        buffer = new FrameBuffer(32 * tilesWidth, 32 * tilesHeight);

                        Tmp.m1.set(Draw.proj());
                        Tmp.m2.set(Draw.trans());
                        Draw.proj(0, 0, buffer.getWidth(), buffer.getHeight());

                        //clear the buffer - some OSs leave garbage in it
                        buffer.begin(Pal.darkerMetal);
                        if(prevBuffers != null){
                            for(var other : prevBuffers){
                                Draw.rect(Draw.wrap(other.buffer.getTexture()), (other.x - originX) * 32 + other.buffer.getWidth()/2f, (other.y - originY) * 32 + other.buffer.getHeight()/2f, other.buffer.getWidth(), -other.buffer.getHeight());
                                Draw.flush();
                            }
                        }

                        buffer.end();
                        Draw.proj(Tmp.m1);
                        Draw.trans(Tmp.m2);
                        Draw.reset();
                    }

                    if(prevBuffers != null){
                        for(var other : prevBuffers){
                            if(!other.buffer.isDisposed()){
                                other.buffer.dispose();
                            }
                        }
                        prevBuffers.clear();
                    }
                });

                processCommands();
            }

            float offset = 0.001f + (rootDisplay.buffer == null ? 0f : (rootDisplay.buffer.hashCode() % 1_000_000) / 1_000_000f * 0.01f);

            Draw.z(Layer.block + offset);

            //TODO: for square regions, this can be optimized to draw only one thing
            Draw.blend(Blending.disabled);
            Draw.draw(Draw.z(), () -> {
                if(rootDisplay.buffer != null){

                    int rtx = (tile.x - originX), rty = (tile.y - originY);

                    // Offset the region to account for display frame (6 pixels)
                    Tmp.tr1.set(rootDisplay.buffer.getTexture(), rtx * 32 - 6, rty * 32 - 6, 32, 32);
                    Draw.rect(Tmp.tr1, x, y, tilesize, -tilesize);
                }
            });
            Draw.blend();

            Draw.z(Layer.block + 0.02f);

            Draw.rect(tileRegion[TileBitmask.values[bits]], x, y);
        }

        @Override
        public void flushCommands(LongSeq graphicsBuffer){
            if(isRoot()){
                super.flushCommands(graphicsBuffer);
            }else{
                rootDisplay.flushCommands(graphicsBuffer);
            }
        }

        public void updateOthers(){
            for(int i = 0; i < 4; i++){
                Tile other = tile.nearby(Geometry.d8edge(i));
                if(other != null && other.block() == block && other.team() == team){
                    other.build.onProximityUpdate();
                }
            }
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            needsUpdate = true;

            updateOthers();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            processed.clear();

            for(var other : proximity){
                if(other instanceof TileableLogicDisplayBuild tl && !processed.contains(tl.id)){
                    tl.needsUpdate = true;
                }
            }

            updateOthers();
        }

        public boolean isRoot(){
            return rootDisplay == this;
        }
    }
}
