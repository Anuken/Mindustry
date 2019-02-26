package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.function.IntPositionConsumer;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.DataPosition;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.ui;

public enum EditorTool{
    pick{
        public void touched(MapEditor editor, int x, int y){
            if(!Structs.inBounds(x, y, editor.getMap().width(), editor.getMap().height())) return;

            byte bf = editor.getMap().read(x, y, DataPosition.floor);
            byte bw = editor.getMap().read(x, y, DataPosition.wall);
            byte link = editor.getMap().read(x, y, DataPosition.link);

            if(link != 0){
                x -= (Pack.leftByte(link) - 8);
                y -= (Pack.rightByte(link) - 8);
                bf = editor.getMap().read(x, y, DataPosition.floor);
                bw = editor.getMap().read(x, y, DataPosition.wall);
            }

            Block block = content.block(bw == 0 ? bf : bw);
            editor.setDrawBlock(block);
            ui.editor.updateSelectedBlock();
        }
    },
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
    spray{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.draw(x, y, isPaint(), editor.getDrawBlock(), 0.012);
        }
    },
    line{
        {

        }
    },
    fill{
        {
            edit = true;
        }

        IntArray stack = new IntArray();
        int width;
        byte be, dest;
        boolean floor;
        MapTileData data;

        public void touched(MapEditor editor, int x, int y){
            if(!Structs.inBounds(x, y, editor.getMap().width(), editor.getMap().height())) return;

            if(editor.getDrawBlock().isMultiblock()){
                //don't fill multiblocks, thanks
                pencil.touched(editor, x, y);
                return;
            }

            data = editor.getMap();

            floor = editor.getDrawBlock() instanceof Floor;

            byte bf = data.read(x, y, DataPosition.floor);
            byte bw = data.read(x, y, DataPosition.wall);
            boolean synth = editor.getDrawBlock().synthetic();
            byte brt = Pack.byteByte((byte) editor.getDrawRotation(), (byte) editor.getDrawTeam().ordinal());

            dest = floor ? bf : bw;
            byte draw = editor.getDrawBlock().id;

            if(dest == draw || content.block(bw) instanceof BlockPart || content.block(bw).isMultiblock()){
                return;
            }

            width = editor.getMap().width();
            int height = editor.getMap().height();

            IntPositionConsumer writer = (px, py) -> {
                TileDataMarker prev = editor.getPrev(px, py, false);

                if(floor){
                    data.write(px, py, DataPosition.floor, draw);
                }else{
                    data.write(px, py, DataPosition.wall, draw);
                }

                if(synth){
                    data.write(px, py, DataPosition.rotationTeam, brt);
                }

                editor.onWrite(px, py, prev);
            };

            if(isAlt()){
                for(int cx = 0; cx < width; cx++){
                    for(int cy = 0; cy < height; cy++){
                        if(eq(cx, cy)){
                            writer.accept(cx, cy);
                        }
                    }
                }
            }else{
                int x1;
                boolean spanAbove, spanBelow;

                stack.clear();

                stack.add(asi(x, y));

                while(stack.size > 0){
                    int popped = stack.pop();
                    x = popped % width;
                    y = popped / width;

                    x1 = x;
                    while(x1 >= 0 && eq(x1, y)) x1--;
                    x1++;
                    spanAbove = spanBelow = false;
                    while(x1 < width && eq(x1, y)){
                        writer.accept(x1, y);

                        if(!spanAbove && y > 0 && eq(x1, y - 1)){
                            stack.add(asi(x1, y - 1));
                            spanAbove = true;
                        }else if(spanAbove && y > 0 && eq(x1, y - 1)){
                            spanAbove = false;
                        }

                        if(!spanBelow && y < height - 1 && eq(x1, y + 1)){
                            stack.add(asi(x1, y + 1));
                            spanBelow = true;
                        }else if(spanBelow && y < height - 1 && eq(x1, y + 1)){
                            spanBelow = false;
                        }
                        x1++;
                    }
                }
            }
        }

        boolean eq(int px, int py){
            byte nbf = data.read(px, py, DataPosition.floor);
            byte nbw = data.read(px, py, DataPosition.wall);
            byte nbe = data.read(px, py, DataPosition.elevation);

            return (floor ? nbf : nbw) == dest && nbe == be;
        }

        int asi(int x, int y){
            return x + y * width;
        }
    },
    zoom;

    boolean edit, draggable;

    public static boolean isPaint(){
        return Core.input.keyDown(KeyCode.CONTROL_LEFT);
    }

    public static boolean isAlt(){
        return Core.input.keyDown(KeyCode.TAB);
    }

    public void touched(MapEditor editor, int x, int y){

    }
}
