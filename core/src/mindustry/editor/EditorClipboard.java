package mindustry.editor;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class EditorClipboard{
    private static final BuildPlan scratch = new BuildPlan();

    public int width, height;
    public short[] floor, overlay, block;
    public byte[] data, floorData, overlayData;
    public int[] extra;
    public Seq<ClipBuild> builds = new Seq<>();
    public boolean dirty;
    /** World position of the captured area's origin. */
    public int srcX, srcY;

    public static class ClipBuild{
        public Block block;
        public int x, y, rotation, team;
        public Object config;
    }

    public boolean capture(int x1, int y1, int x2, int y2){
        int minX = Math.max(Math.min(x1, x2), 0), minY = Math.max(Math.min(y1, y2), 0);
        int maxX = Math.min(Math.max(x1, x2), state.world.width - 1), maxY = Math.min(Math.max(y1, y2), state.world.height - 1);
        int ominX = minX, ominY = minY, omaxX = maxX, omaxY = maxY;

        if(minX > maxX || minY > maxY) return false;

        //grow the rectangle until it fully contains every building it originally touched
        for(int x = ominX; x <= omaxX; x++){
            for(int y = ominY; y <= omaxY; y++){
                Tile tile = editor.tile(x, y);
                if(tile.build == null) continue;

                Block b = tile.build.block;
                int off = (b.size - 1) / 2;
                int bx = tile.build.tile.x, by = tile.build.tile.y;
                int fx1 = Math.max(bx - off, 0), fy1 = Math.max(by - off, 0);
                int fx2 = Math.min(bx + b.size / 2, state.world.width - 1), fy2 = Math.min(by + b.size / 2, state.world.height - 1);

                if(fx1 < minX || fy1 < minY || fx2 > maxX || fy2 > maxY){
                    minX = Math.min(minX, fx1);
                    minY = Math.min(minY, fy1);
                    maxX = Math.max(maxX, fx2);
                    maxY = Math.max(maxY, fy2);
                }
            }
        }


        width = maxX - minX + 1;
        height = maxY - minY + 1;
        srcX = minX;
        srcY = minY;

        int size = width * height;
        floor = new short[size];
        overlay = new short[size];
        block = new short[size];
        data = new byte[size];
        floorData = new byte[size];
        overlayData = new byte[size];
        extra = new int[size];
        builds.clear();

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = editor.tile(x + minX, y + minY);
                int i = x + y * width;

                floor[i] = tile.floorID();
                overlay[i] = tile.overlayID();
                block[i] = tile.build != null ? 0 : tile.blockID();
                data[i] = tile.block() instanceof StaticWall ? 0 : tile.data;
                floorData[i] = tile.floorData;
                overlayData[i] = tile.overlayData;
                extra[i] = tile.extraData;

                if(tile.build != null && tile.isCenter()){
                    ClipBuild c = new ClipBuild();
                    c.block = tile.block();
                    c.x = x;
                    c.y = y;
                    c.rotation = tile.build.rotation;
                    c.team = tile.team().id;
                    c.config = tile.build.config();
                    builds.add(c);
                }
            }
        }

        dirty = true;
        return true;
    }

    public void rotate(int direction){
        transform(direction >= 0 ? 0 : 1);
    }

    public void flip(boolean x){
        transform(x ? 2 : 3);
    }

    //mode: 0 = rotate CCW, 1 = rotate CW, 2 = flip X, 3 = flip Y
    private void transform(int mode){
        int w = width, h = height;
        boolean swap = mode < 2;
        int nw = swap ? h : w, nh = swap ? w : h;

        short[] nFloor = new short[w * h], nOverlay = new short[w * h], nBlock = new short[w * h];
        byte[] nData = new byte[w * h], nFloorData = new byte[w * h], nOverlayData = new byte[w * h];
        int[] nExtra = new int[w * h];

        for(int x = 0; x < w; x++){
            for(int y = 0; y < h; y++){
                int i = x + y * w;
                int j = mapX(mode, x, y, w, h) + mapY(mode, x, y, w, h) * nw;

                nFloor[j] = floor[i];
                nOverlay[j] = overlay[i];
                nBlock[j] = block[i];
                nFloorData[j] = floorData[i];
                nExtra[j] = extra[i];
                nData[j] = data[i];
                nOverlayData[j] = overlayData[i];

                if(block[i] != 0 && content.block(block[i]) instanceof Cliff){
                    nData[j] = (byte)remapCliff(mode, data[i] & 0xff);
                }

                if(content.block(overlay[i]) instanceof CharacterOverlay || content.block(overlay[i]) instanceof RuneOverlay){
                    nOverlayData[j] = remapChar(mode, overlayData[i]);
                }
            }
        }

        for(EditorClipboard.ClipBuild b : builds){
            int size = b.block.size;
            int off = (size - 1) / 2;
            int ax = b.x - off, ay = b.y - off, bx = b.x + size / 2, by = b.y + size / 2;
            int nx = Math.min(mapX(mode, ax, ay, w, h), mapX(mode, bx, by, w, h));
            int ny = Math.min(mapY(mode, ax, ay, w, h), mapY(mode, bx, by, w, h));
            b.x = nx + off;
            b.y = ny + off;

            float coff = size % 2 == 0 ? -0.5f : 0f;

            if(mode < 2){
                boolean ccw = mode == 0;
                b.config = BuildPlan.pointConfig(b.block, b.config, p -> {
                    float cx = p.x + coff, cy = p.y + coff;
                    float lx = cx;

                    if(ccw){
                        cx = -cy;
                        cy = lx;
                    }else{
                        cx = cy;
                        cy = -lx;
                    }
                    p.set(Mathf.floor(cx - coff), Mathf.floor(cy - coff));
                });
                b.rotation = b.block.planRotation(Mathf.mod(b.rotation + (ccw ? 1 : -1), 4));
            }else{
                boolean fx = mode == 2;
                b.config = BuildPlan.pointConfig(b.block, b.config, p -> {
                    if(fx){
                        if(size % 2 == 0) p.x--;
                        p.x = -p.x;
                    }else{
                        if(size % 2 == 0) p.y--;
                        p.y = -p.y;
                    }
                });

                scratch.block = b.block;
                scratch.rotation = b.rotation;
                b.block.flipRotation(scratch, fx);
                b.rotation = scratch.rotation;
            }
        }

        width = nw;
        height = nh;
        floor = nFloor;
        overlay = nOverlay;
        block = nBlock;
        data = nData;
        floorData = nFloorData;
        overlayData = nOverlayData;
        extra = nExtra;
        dirty = true;
    }

    private static int mapX(int mode, int x, int y, int w, int h){
        return switch(mode){
            case 0 -> h - 1 - y;
            case 1 -> y;
            case 2 -> w - 1 - x;
            default -> x;
        };
    }

    private static int mapY(int mode, int x, int y, int w, int h){
        return switch(mode){
            case 0 -> x;
            case 1 -> w - 1 - x;
            case 2 -> y;
            default -> h - 1 - y;
        };
    }

    private static int remapCliff(int mode, int mask){
        int out = 0;
        for(int i = 0; i < 8; i++){
            if((mask & (1 << i)) == 0) continue;

            int j = switch(mode){
                case 0 -> (i + 2) % 8;
                case 1 -> (i + 6) % 8;
                case 2 -> (4 - i + 8) % 8;
                default -> (8 - i) % 8;
            };
            out |= 1 << j;
        }
        return out;
    }

    private static byte remapChar(int mode, byte value){
        int rot = CharOverlayData.rotation(value);

        rot = switch(mode){
            case 0 -> rot + 1;
            case 1 -> rot - 1;
            case 2 -> (rot % 2 == 0) ? rot + 2 : rot;
            default -> (rot % 2 != 0) ? rot + 2 : rot;
        };

        return CharOverlayData.get(CharOverlayData.character(value), (byte)Mathf.mod(rot, 4));
    }

    public void paste(int originX, int originY){
        if(floor == null) return;

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int wx = x + originX, wy = y + originY;
                if(!Structs.inBounds(wx, wy, state.world.width, state.world.height)) continue;

                Tile t = editor.tile(wx, wy);
                editor.addTileOp(TileOp.get(t.x, t.y, DrawOperation.opData, TileOpData.get(t.data, t.floorData, t.overlayData)));
                editor.addTileOp(TileOp.get(t.x, t.y, DrawOperation.opDataExtra, t.extraData));

                if(t.block() != Blocks.air) t.setBlock(Blocks.air);
            }
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int wx = x + originX, wy = y + originY;
                if(!Structs.inBounds(wx, wy, state.world.width, state.world.height)) continue;

                Tile t = editor.tile(wx, wy);
                int i = x + y * width;

                t.setFloor((Floor)content.block(floor[i]));
                t.setOverlay(content.block(overlay[i]));

                if(block[i] != 0){
                    t.setBlock(content.block(block[i]), Team.derelict);
                }
            }
        }

        Seq<Building> placed = new Seq<>();
        Seq<ClipBuild> placedFrom = new Seq<>();

        for(ClipBuild b : builds){
            int off = (b.block.size - 1) / 2;
            int x1 = b.x + originX - off, y1 = b.y + originY - off;
            int x2 = b.x + originX + b.block.size / 2, y2 = b.y + originY + b.block.size / 2;

            if(!Structs.inBounds(x1, y1, state.world.width, state.world.height) || !Structs.inBounds(x2, y2, state.world.width, state.world.height)) continue;

            Tile t = editor.tile(b.x + originX, b.y + originY);
            t.setBlock(b.block, Team.get(b.team), b.rotation);

            if(t.build != null && t.isCenter() && t.block() == b.block){
                placed.add(t.build);
                placedFrom.add(b);
            }
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int wx = x + originX, wy = y + originY;
                if(!Structs.inBounds(wx, wy, state.world.width, state.world.height)) continue;

                Tile t = editor.tile(wx, wy);
                int i = x + y * width;

                t.data = data[i];
                t.floorData = floorData[i];
                t.overlayData = overlayData[i];
                t.extraData = extra[i];
                editor.renderer.updateStatic(wx, wy);
            }
        }

        boolean prev = state.rules.editor;
        state.rules.editor = true;
        for(int i = 0; i < placed.size; i++){
            Object config = placedFrom.get(i).config;
            if(config != null) placed.get(i).configureAny(config);
        }
        state.rules.editor = prev;

        editor.flushOp();
        ui.editor.resetSaved();
    }
}
