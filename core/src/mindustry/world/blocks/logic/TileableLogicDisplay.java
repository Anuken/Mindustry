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
import mindustry.world.*;

import static mindustry.Vars.*;

public class TileableLogicDisplay extends LogicDisplay{
    protected static final Seq<TileableLogicDisplayBuild> queue = new Seq<>();
    protected static final Seq<TileableLogicDisplayBuild> displays = new Seq<>();
    protected static final IntSet processed = new IntSet();

    //in tiles
    public int maxDisplayDimensions = 12;
    public @Load(value = "@-#", length = 47) TextureRegion[] tileRegion;
    public @Load("@-back") TextureRegion backRegion;

    static final int[] bitmasks = {
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    3,  4,  3,  4, 15, 40, 15, 20,  3,  4,  3,  4, 15, 40, 15, 20,
    5, 28,  5, 28, 29, 10, 29, 23,  5, 28,  5, 28, 31, 11, 31, 32,
    3,  4,  3,  4, 15, 40, 15, 20,  3,  4,  3,  4, 15, 40, 15, 20,
    2, 30,  2, 30,  9, 46,  9, 22,  2, 30,  2, 30, 14, 44, 14,  6,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    3,  0,  3,  0, 15, 42, 15, 12,  3,  0,  3,  0, 15, 42, 15, 12,
    5,  8,  5,  8, 29, 35, 29, 33,  5,  8,  5,  8, 31, 34, 31,  7,
    3,  0,  3,  0, 15, 42, 15, 12,  3,  0,  3,  0, 15, 42, 15, 12,
    2,  1,  2,  1,  9, 45,  9, 19,  2,  1,  2,  1, 14, 18, 14, 13,
    };

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

        int tilesWidth = topX - botX + 1, tilesHeight = topY - botY + 1;

        //the new root display has been assigned
        for(var member : displays){
            member.rootDisplay = root;
            member.tilesWidth = tilesWidth;
            member.tilesHeight = tilesHeight;
            member.originX = botX;
            member.originY = botY;

            //TODO: preserve buffers later
            if(member.buffer != null){
                member.buffer.dispose();
                member.buffer = null;
            }
        }
    }

    public class TileableLogicDisplayBuild extends LogicDisplayBuild{
        //bottom left corner of display
        public TileableLogicDisplayBuild rootDisplay = this;
        //size of display area
        public int tilesWidth = 1, tilesHeight = 1, originX, originY;

        public int bits = 0;

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
            Draw.rect(backRegion, x, y);

            //don't even bother processing anything when displays are off.
            if(!Vars.renderer.drawDisplays) return;

            if(isRoot()){
                Draw.draw(Draw.z(), () -> {
                    if(buffer == null && tilesWidth <= maxDisplayDimensions && tilesHeight <= maxDisplayDimensions){
                        buffer = new FrameBuffer(32 * tilesWidth, 32 * tilesHeight);
                        //clear the buffer - some OSs leave garbage in it
                        buffer.begin(Pal.darkerMetal);
                        buffer.end();
                    }
                });

                processCommands();
            }

            Draw.z(Layer.block + 0.001f);

            //TODO this is slow, many texture switches
            Draw.blend(Blending.disabled);
            Draw.draw(Draw.z(), () -> {
                if(rootDisplay.buffer != null){

                    int rtx = (tile.x - originX), rty = (tile.y - originY);

                    Tmp.tr1.set(rootDisplay.buffer.getTexture(), rtx * 32, rty * 32, 32, 32);
                    Draw.rect(Tmp.tr1, x, y, tilesize, -tilesize);
                }
            });
            Draw.blend();

            Draw.z(Layer.block + 0.002f);

            Draw.rect(tileRegion[bitmasks[bits]], x, y);
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

            linkDisplays(this);

            updateOthers();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            processed.clear();

            for(var other : proximity){
                if(other instanceof TileableLogicDisplayBuild tl && !processed.contains(tl.id)){
                    linkDisplays(tl);
                }
            }

            updateOthers();
        }

        public boolean isRoot(){
            return rootDisplay == this;
        }
    }
}
