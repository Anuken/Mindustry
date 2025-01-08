package mindustry.world.blocks.logic;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class TileableLogicDisplay extends LogicDisplay{
    protected static final Seq<TileableLogicDisplayBuild> queue = new Seq<>();
    protected static final Seq<TileableLogicDisplayBuild> displays = new Seq<>();
    protected static final IntSet processed = new IntSet();

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

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);

            //don't even bother processing anything when displays are off.
            if(!Vars.renderer.drawDisplays) return;

            if(isRoot()){
                Draw.draw(Draw.z(), () -> {
                    if(buffer == null){
                        buffer = new FrameBuffer(32 * tilesWidth, 32 * tilesHeight);
                        Log.info("create " + buffer.getWidth() + " " + buffer.getHeight());
                        //clear the buffer - some OSs leave garbage in it
                        buffer.begin(Pal.darkerMetal);
                        buffer.end();
                    }
                });

                processCommands();
            }

            Draw.blend(Blending.disabled);
            Draw.draw(Draw.z(), () -> {
                if(rootDisplay.buffer != null){

                    int rtx = (tile.x - originX), rty = (tile.y - originY);

                    Tmp.tr1.set(rootDisplay.buffer.getTexture(), rtx * 32, rty * 32, 32, 32);
                    Draw.rect(Tmp.tr1, x, y, tilesize, -tilesize);
                }
            });
            Draw.blend();
        }

        @Override
        public void flushCommands(LongSeq graphicsBuffer){
            if(isRoot()){
                super.flushCommands(graphicsBuffer);
            }else{
                rootDisplay.flushCommands(graphicsBuffer);
            }
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            linkDisplays(this);
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
        }

        public boolean isRoot(){
            return rootDisplay == this;
        }
    }
}
