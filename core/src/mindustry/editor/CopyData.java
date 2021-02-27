package mindustry.editor;

import arc.func.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;

// Class that holds data about selected tiles.
// It handles rotation of selection.
public class CopyData{
    private int w, h;
    private Tile[][] main, rotated;
    private Seq<Tile> pool = new Seq<>();
    public Selection selection = new Selection();

    public void copy(int dx, int dy, int w, int h){
        resize(w, h);

        loop((ox, oy) -> {
            final int x = ox + dx;
            final int y = ox + dy;

            Tile ref = world.tile(x, y);
            Tile t = pool.pop(() -> new Tile(x, y));
            copyTile(ref, t);

            main[ox][oy] = t;
        });
    }

    public void paste(int dx, int dy) {
        // clamp it
        int
            stx = Math.max(dx, 0),
            sty = Math.max(dy, 0),
            edx = Math.min(dx+w, world.width()),
            edy = Math.min(dy+h, world.width());

        // garbage can do bad
        loop(stx, sty, edx, edy, (x, y) -> world.tile(x, y).remove());

        loop(stx, sty, edx, edy, (x, y) -> copyTile(main[y-dy][x-dx], world.tile(x, y)));
    }

    private void copyTile(Tile scr, Tile dst) {
        if(scr.isCenter()){
            dst.setBlock(scr.block(), scr.team(), scr.build == null ? 0 : scr.build.rotation);
        }

        dst.setFloor(scr.floor());
        dst.setOverlay(scr.overlay());
    }

    public void rotR(){
        flipX(false);
        flipD(false);

        rotate(-1);
    }

    public void rotL(){
        flipY(false);
        flipD(false);

        rotate(1);
    }

    public void flipX(boolean alone){
        loop(w / 2, h, (x, y) -> swap(x, y, w - x - 1, y));

        if(alone){
            rotate(2);
        }
    }

    public void flipY(boolean alone){
        loop(w, h / 2, (x, y) -> swap(x, y, w, h - y - 1));

        if(alone){
            rotate(2);
        }
    }

    public void flipD(boolean alone){
        loop((x, y) -> swap(x, y, y, x, main, rotated));

        if(alone){
            rotate(-1);
        }

        int t = w;
        w = h;
        h = t;

        Tile[][] u = main;
        main = rotated;
        rotated = u;
    }

    private void rotate(int shift){
        loop(t -> {
            if(t.build != null){
                t.build.rotation(t.build.rotation + shift);
            }
        });
    }

    private void resize(int w, int h){
        loop(t -> pool.add(t));

        if(this.w == w && this.h == h) return;

        this.w = w;
        this.h = h;

        main = new Tile[h][w];
        rotated = new Tile[w][h];
    }

    private void swap(int sx, int sy, int dx, int dy){
        swap(sx, sy, dx, dy, main, main);
    }

    // rSwap calls swap with main as source and rotated as source
    private void rSwap(int sx, int sy, int dx, int dy){
        swap(sx, sy, dx, dy, main, rotated);
    }

    // swap swaps value from one 2D array with value of another
    private void swap(int sx, int sy, int dx, int dy, Tile[][] src, Tile[][] dst){
        Tile t = dst[dy][dx];
        dst[dy][dx] = src[sy][sx];
        src[sy][sx] = t;
    }

    private void loop(Cons<Tile> cons){
        loop((x, y) -> cons.get(main[y][x]));
    }

    private void loop(Looper looper){
        loop(w, h, looper);
    }

    private void loop(int w, int h, Looper looper){
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                looper.pos(x, y);
            }
        }
    }

    private void loop(int stx, int sty, int edx, int edy, Looper looper) {
        for(int y = sty; y < edy; y++) {
            for(int x = stx; x < edx; x++) {
                looper.pos(x, y);
            }
        }
    }

    interface Looper{
        void pos(int x, int y);
    }

    public static class Selection {
        int px, py, x, y, w, h;

        public void setOrigin(int px, int py) {
            this.px = px;
            this.py = py;
        }

        public void adjust(int dx, int dy) {
            w = Math.abs(px - dx);
            h = Math.abs(py - dx);
            x = Math.min(px, dx);
            y = Math.min(py, dy);
        }
    }
}
