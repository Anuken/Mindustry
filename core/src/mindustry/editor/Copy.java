package mindustry.editor;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.world;

/**
 * Class that holds data about selected tiles. It handles rotation of selection, clamping copying and pasting.
 */

public class Copy{

    int
    px, py, // selection origin
    dx, dy, // selection offset
    w, h;   // size of selection

    private CTile[][] main, rotated; // to make rotation faster differently composed space is pre-allocated
    private final Seq<CTile> pool = new Seq<>(); // to reuse tiles

    public Copy(){
        clear(); // to init the min and rotated
    }

    public void setOrigin(int x, int y){
        // prevent off map origin
        px = clampW(x);
        py = clampH(y);
    }

    public void adjust(int x, int y){
        // prevent off map origin
        x = clampW(x);
        y = clampH(y);

        // + 1 because it loops nicer then
        w = Math.abs(px - x) + 1;
        h = Math.abs(py - y) + 1;
        dx = Math.min(px, x);
        dy = Math.min(py, y);
    }

    public boolean empty(){
        return h == 0 || w == 0;
    }

    public void clear(){
        w = 0;
        h = 0;
        resize();
    }

    public void copy(){
        resize();

        loop(w, h, (x, y) -> {
            CTile t = pool.pop(CTile::new);

            t.copy(world.tile(x + dx, y + dy));
            main[y][x] = t;
        });
    }

    public void paste(){
        // clamp it as moving selection out of the map can be useful
        int
        stx = Math.max(dx, 0),
        sty = Math.max(dy, 0),
        edx = Math.min(dx + w, world.width()),
        edy = Math.min(dy + h, world.height());

        // we don't want garbage blocks, but also don't want to remove new blocks
        loop(stx, sty, edx, edy, (x, y) -> world.tile(x, y).remove());
        // final paste
        loop(stx, sty, edx, edy, (x, y) -> main[y - dy][x - dx].paste(world.tile(x, y)));
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

        if(alone) rotate(2);

        loop(w, h, (x, y) -> flipMultiBlock(x, y, -1, 0));
    }


    public void flipY(boolean alone){
        loop(w, h / 2, (x, y) -> swap(x, y, x, h - y - 1));

        if(alone) rotate(2);

        loop(w, h, (x, y) -> flipMultiBlock(x, y, 0, -1));
    }

    public void flipMultiBlock(int x, int y, int dx, int dy){
        CTile a = main[y][x];
        if(a.build.block == null || a.build.block.size % 2 == 1) return; //no need
        x += dx;
        y += dy;

        // can happen
        if(x < 0 || y < 0) {
            a.build.block = null;
            return;
        }

        CTile b = main[y][x];
        CTile.Build tmp = a.build;
        a.build = b.build;
        b.build = tmp;
    }

    public void flipD(boolean alone){
        loop(w, h, (x, y) -> swap(x, y, y, x, main, rotated));

        if(alone) rotate(-1); // for completion sake

        int t = w;
        w = h;
        h = t;

        CTile[][] u = main;
        main = rotated;
        rotated = u;
    }

    private void rotate(int shift){
        loop(w, h, t -> {
            t.build.rotation += shift;

            if(t.build.config instanceof Point2 p){
                p.rotate(shift);
            }else if(t.build.config instanceof Point2[] p){
                for(Object o : p){
                    if(o instanceof Point2 n){
                        n.rotate(shift);
                    }
                }
            }
        });
    }

    private void resize(){
        // collect garbage
        if(main != null && main.length != 0) loop(main[0].length, main.length, t -> pool.add(t));

        main = new CTile[h][w];
        rotated = new CTile[w][h];
    }

    private void swap(int sx, int sy, int dx, int dy){
        swap(sx, sy, dx, dy, main, main);
    }

    // swap swaps value from one 2D array with value of another
    private void swap(int sx, int sy, int dx, int dy, CTile[][] src, CTile[][] dst){
        CTile t = dst[dy][dx];
        dst[dy][dx] = src[sy][sx];
        src[sy][sx] = t;
    }

    private void loop(int w, int h, Cons<CTile> cons){
        loop(0, 0, w, h, (x, y) -> cons.get(main[y][x]));
    }

    private void loop(int w, int h, Looper looper){
        loop(0, 0, w, h, looper);
    }

    private void loop(int stx, int sty, int edx, int edy, Looper looper){
        // i don't like repetitive nested loops that all
        for(int y = sty; y < edy; y++){
            for(int x = stx; x < edx; x++){
                looper.pos(x, y);
            }
        }
    }

    public void move(int dx, int dy){
        this.dx += dx;
        this.dy += dy;
    }

    public boolean contains(int x, int y){
        return dx <= x && dy <= y && dx + w >= x && dy + h >= y;
    }

    private int clampW(int x){
        return Mathf.clamp(x, 0, world.width() - 1);
    }

    private int clampH(int y){
        return Mathf.clamp(y, 0, world.height() - 1);
    }

    /**
     * data container used for transporting copy data
     */
    private static class CTile {
        Block overlay;
        Floor floor;
        Build build = new Build();


        void copy(Tile t) {
            floor = t.floor();
            overlay = t.overlay();
            build.block = t.block();
            if(!t.isCenter()) {
                build.block = null;
            } else if(t.build != null) {
                build.team = t.build.team();
                build.config = t.build.config();
                build.rotation = t.build.rotation();
            }
        }

        void paste(Tile t) {
            if(build.block != null) {
                t.setBlock(build.block, build.team, build.rotation);
                if(t.build != null){
                    t.build.configure(build.config);
                }
            }
            t.setOverlay(overlay);
            t.setFloor(floor);
        }

        private static class Build {
            Block block;
            int rotation;
            Object config;
            Team team;
        }
    }

    interface Looper{
        void pos(int x, int y);
    }

}
