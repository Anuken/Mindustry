package mindustry.editor;

import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public enum EditorTool{
    zoom(KeyCode.v),
    pick(KeyCode.i){
        public void touched(int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;

            Tile tile = editor.tile(x, y);
            editor.drawBlock = tile.block() == Blocks.air || !tile.block().inEditor ? tile.overlay() == Blocks.air ? tile.floor() : tile.overlay() : tile.block();
        }
    },
    line(KeyCode.l, "replace", "orthogonal"){

        @Override
        public void touchedLine(int x1, int y1, int x2, int y2){
            //straight
            if(mode == 1){
                if(Math.abs(x2 - x1) > Math.abs(y2 - y1)){
                    y2 = y1;
                }else{
                    x2 = x1;
                }
            }

            Bresenham2.line(x1, y1, x2, y2, (x, y) -> {
                if(mode == 0){
                    //replace
                    editor.drawBlocksReplace(x, y);
                }else{
                    //normal
                    editor.drawBlocks(x, y);
                }
            });
        }
    },
    //the "under liquid" rendering is too buggy to make public
    pencil(KeyCode.b, "replace", "square", "drawteams"/*, "underliquid"*/){
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){
            if(mode == -1){
                //normal mode
                editor.drawBlocks(x, y);
            }else if(mode == 0){
                //replace mode
                editor.drawBlocksReplace(x, y);
            }else if(mode == 1){
                //square mode
                editor.drawBlocks(x, y, true, false, tile -> true);
            }else if(mode == 2){
                //draw teams
                editor.drawCircle(x, y, tile -> tile.setTeam(editor.drawTeam));
            }else if(mode == 3){
                editor.drawBlocks(x, y, false, true, tile -> tile.floor().isLiquid);
            }

        }
    },
    eraser(KeyCode.e, "eraseores"){
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){
            editor.drawCircle(x, y, tile -> {
                if(mode == -1){
                    //erase block
                    tile.remove();
                }else if(mode == 0){
                    //erase ore
                    tile.clearOverlay();
                }
            });
        }
    },
    fill(KeyCode.g, "replaceall", "fillteams"){
        {
            edit = true;
        }

        IntSeq stack = new IntSeq();

        @Override
        public void touched(int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;
            Tile tile = editor.tile(x, y);

            if(editor.drawBlock.isMultiblock()){
                //don't fill multiblocks, thanks
                pencil.touched(x, y);
                return;
            }

            //mode 0 or 1, fill everything with the floor/tile or replace it
            if(mode == 0 || mode == -1){
                //can't fill parts or multiblocks
                if(tile.block().isMultiblock()){
                    return;
                }

                Boolf<Tile> tester;
                Cons<Tile> setter;

                if(editor.drawBlock.isOverlay()){
                    Block dest = tile.overlay();
                    if(dest == editor.drawBlock) return;
                    tester = t -> t.overlay() == dest && (t.floor().hasSurface() || !t.floor().needsSurface);
                    setter = t -> t.setOverlay(editor.drawBlock);
                }else if(editor.drawBlock.isFloor()){
                    Block dest = tile.floor();
                    if(dest == editor.drawBlock) return;
                    tester = t -> t.floor() == dest;
                    setter = t -> t.setFloorUnder(editor.drawBlock.asFloor());
                }else{
                    Block dest = tile.block();
                    if(dest == editor.drawBlock) return;
                    tester = t -> t.block() == dest;
                    setter = t -> t.setBlock(editor.drawBlock, editor.drawTeam);
                }

                //replace only when the mode is 0 using the specified functions
                fill(x, y, mode == 0, tester, setter);
            }else if(mode == 1){ //mode 1 is team fill

                //only fill synthetic blocks, it's meaningless otherwise
                if(tile.synthetic()){
                    Team dest = tile.team();
                    if(dest == editor.drawTeam) return;
                    fill(x, y, true, t -> t.getTeamID() == dest.id && t.synthetic(), t -> t.setTeam(editor.drawTeam));
                }
            }
        }

        void fill(int x, int y, boolean replace, Boolf<Tile> tester, Cons<Tile> filler){
            int width = editor.width(), height = editor.height();

            if(replace){
                //just do it on everything
                for(int cx = 0; cx < width; cx++){
                    for(int cy = 0; cy < height; cy++){
                        Tile tile = editor.tile(cx, cy);
                        if(tester.get(tile)){
                            filler.get(tile);
                        }
                    }
                }

            }else{
                //perform flood fill
                int x1;

                stack.clear();
                stack.add(Point2.pack(x, y));

                try{
                    while(stack.size > 0 && stack.size < width*height){
                        int popped = stack.pop();
                        x = Point2.x(popped);
                        y = Point2.y(popped);

                        x1 = x;
                        while(x1 >= 0 && tester.get(editor.tile(x1, y))) x1--;
                        x1++;
                        boolean spanAbove = false, spanBelow = false;
                        while(x1 < width && tester.get(editor.tile(x1, y))){
                            filler.get(editor.tile(x1, y));

                            if(!spanAbove && y > 0 && tester.get(editor.tile(x1, y - 1))){
                                stack.add(Point2.pack(x1, y - 1));
                                spanAbove = true;
                            }else if(spanAbove && !tester.get(editor.tile(x1, y - 1))){
                                spanAbove = false;
                            }

                            if(!spanBelow && y < height - 1 && tester.get(editor.tile(x1, y + 1))){
                                stack.add(Point2.pack(x1, y + 1));
                                spanBelow = true;
                            }else if(spanBelow && y < height - 1 && !tester.get(editor.tile(x1, y + 1))){
                                spanBelow = false;
                            }
                            x1++;
                        }
                    }
                    stack.clear();
                }catch(OutOfMemoryError e){
                    //hack
                    stack = null;
                    System.gc();
                    e.printStackTrace();
                    stack = new IntSeq();
                }
            }
        }
    },
    spray(KeyCode.r, "replace"){
        final double chance = 0.012;

        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(int x, int y){

            //floor spray
            if(editor.drawBlock.isFloor()){
                editor.drawCircle(x, y, tile -> {
                    if(Mathf.chance(chance)){
                        tile.setFloor(editor.drawBlock.asFloor());
                    }
                });
            }else if(mode == 0){ //replace-only mode, doesn't affect air
                editor.drawBlocks(x, y, tile -> Mathf.chance(chance) && tile.block() != Blocks.air);
            }else{
                editor.drawBlocks(x, y, tile -> Mathf.chance(chance));
            }
        }
    };

    public static final EditorTool[] all = values();

    /** All the internal alternate placement modes of this tool. */
    public final String[] altModes;
    /** Key to activate this tool. */
    public KeyCode key = KeyCode.unset;
    /** The current alternate placement mode. -1 is the standard mode, no changes.*/
    public int mode = -1;
    /** Whether this tool causes canvas changes when touched.*/
    public boolean edit;
    /** Whether this tool should be dragged across the canvas when the mouse moves.*/
    public boolean draggable;

    EditorTool(){
        this(new String[]{});
    }

    EditorTool(KeyCode code){
        this(new String[]{});
        this.key = code;
    }

    EditorTool(String... altModes){
        this.altModes = altModes;
    }

    EditorTool(KeyCode code, String... altModes){
        this.altModes = altModes;
        this.key = code;
    }

    public void touched(int x, int y){}

    public void touchedLine(int x1, int y1, int x2, int y2){}
}
