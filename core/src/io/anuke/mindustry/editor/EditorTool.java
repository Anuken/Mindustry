package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.DataPosition;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.function.IntPositionConsumer;
import io.anuke.ucore.util.Structs;
import io.anuke.ucore.util.Bits;

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
                x -= (Bits.getLeftByte(link) - 8);
                y -= (Bits.getRightByte(link) - 8);
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
            editor.draw(x, y);
        }
    },
    eraser{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.draw(x, y, Blocks.air);
        }
    },
    elevation{
        {
            edit = true;
            draggable = true;
        }

        @Override
        public void touched(MapEditor editor, int x, int y){
            editor.elevate(x, y);
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
            be = data.read(x, y, DataPosition.elevation);
            boolean synth = editor.getDrawBlock().synthetic();
            byte brt = Bits.packByte((byte) editor.getDrawRotation(), (byte) editor.getDrawTeam().ordinal());

            dest = floor ? bf : bw;
            byte draw = editor.getDrawBlock().id;

            if(dest == draw){
                return;
            }

            width = editor.getMap().width();
            int height = editor.getMap().height();

            int x1;
            boolean spanAbove, spanBelow;

            stack.clear();

            stack.add(asi(x, y));

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

    public void touched(MapEditor editor, int x, int y){

    }
}
