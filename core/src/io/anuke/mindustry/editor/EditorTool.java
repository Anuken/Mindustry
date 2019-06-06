package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.function.IntPositionConsumer;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

public enum EditorTool{
    zoom,
    pick{
        public void touched(MapEditor editor, int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;

            Tile tile = editor.tile(x, y).link();

            editor.drawBlock = tile.block() == Blocks.air ? tile.overlay() == Blocks.air ? tile.floor() : tile.overlay() : tile.block();
        }
    },
    line,
    pencil{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.draw(x, y, isPaint());
        }
    },
    eraser{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.draw(x, y, isPaint(), Blocks.air);
        }
    },
    fill{
        {
            edit = true;
        }

        IntArray stack = new IntArray();
        Block dest;
        boolean isFloor;
        MapEditor data;

        public void touched(MapEditor editor, int x, int y){
            if(!Structs.inBounds(x, y, editor.width(), editor.height())) return;
            Tile tile = editor.tile(x, y);

            if(editor.drawBlock.isMultiblock()){
                //don't fill multiblocks, thanks
                pencil.touched(editor, x, y);
                return;
            }

            data = editor;
            isFloor = editor.drawBlock instanceof Floor;

            Block floor = tile.floor();
            Block block = tile.block();
            boolean synth = editor.drawBlock.synthetic();

            Block draw = editor.drawBlock;
            dest = draw instanceof OverlayFloor ? tile.overlay() : isFloor ? floor : block;

            if(dest == draw || block instanceof BlockPart || block.isMultiblock()){
                return;
            }

            boolean alt = isAlt();

            int width = editor.width();
            int height = editor.height();

            IntPositionConsumer writer = (px, py) -> {
                Tile write = editor.tile(px, py);

                if(isFloor){
                    if(alt && !(draw instanceof OverlayFloor)){
                        Block ore = write.overlay();
                        write.setFloor((Floor)draw);
                        write.setOverlay(ore);
                    }else{
                        write.setFloor((Floor)draw);
                    }
                }else{
                    write.setBlock(draw);
                }

                if(synth){
                    write.setTeam(editor.drawTeam);
                }

                if(draw.rotate){
                    write.rotation((byte)editor.rotation);
                }
            };

            if(isAlt()){
                //fill all of the same type regardless of borders
                for(int cx = 0; cx < width; cx++){
                    for(int cy = 0; cy < height; cy++){
                        if(eq(cx, cy)){
                            writer.accept(cx, cy);
                        }
                    }
                }
            }else if(isAlt2()){
                //fill all teams.
                for(int cx = 0; cx < width; cx++){
                    for(int cy = 0; cy < height; cy++){
                        Tile write = editor.tile(cx, cy);
                        if(write.block().synthetic()){
                            write.setTeam(editor.drawTeam);
                        }
                    }
                }
            }else{
                //normal fill
                int x1;

                stack.clear();

                stack.add(Pos.get(x, y));

                while(stack.size > 0){
                    int popped = stack.pop();
                    x = Pos.x(popped);
                    y = Pos.y(popped);

                    x1 = x;
                    while(x1 >= 0 && eq(x1, y)) x1--;
                    x1++;
                    boolean spanAbove = false, spanBelow = false;
                    while(x1 < width && eq(x1, y)){
                        writer.accept(x1, y);

                        if(!spanAbove && y > 0 && eq(x1, y - 1)){
                            stack.add(Pos.get(x1, y - 1));
                            spanAbove = true;
                        }else if(spanAbove && !eq(x1, y - 1)){
                            spanAbove = false;
                        }

                        if(!spanBelow && y < height - 1 && eq(x1, y + 1)){
                            stack.add(Pos.get(x1, y + 1));
                            spanBelow = true;
                        }else if(spanBelow && y < height - 1 && !eq(x1, y + 1)){
                            spanBelow = false;
                        }
                        x1++;
                    }
                }
            }
        }

        boolean eq(int px, int py){
            Tile tile = data.tile(px, py);

            return (data.drawBlock instanceof OverlayFloor ? tile.overlay() : isFloor ? tile.floor() : tile.block()) == dest && !(data.drawBlock instanceof OverlayFloor && tile.floor().isLiquid);
        }
    },
    spray{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.draw(x, y, isPaint(), editor.drawBlock, 0.012);
        }
    };

    boolean edit, draggable;

    public static boolean isPaint(){
        return Core.input.keyDown(KeyCode.CONTROL_LEFT);
    }

    public static boolean isAlt(){
        return Core.input.keyDown(KeyCode.TAB);
    }

    public static boolean isAlt2(){
        return Core.input.keyDown(KeyCode.GRAVE);
    }

    public void touched(MapEditor editor, int x, int y){

    }
}
