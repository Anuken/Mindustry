package mindustry.editor;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.power.*;

import static mindustry.Vars.world;

/**
 * Class that holds data about selected tiles. It handles rotation of selection, clamping copying and pasting.
 */
public class Copy{

    int
    sx, sy,
    px, py, // selection origin
    dx, dy, // selection offset
    w, h;   // size of selection

    boolean selected;

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
        // paste everything but config
        loop(stx, sty, edx, edy, (x, y) -> main[y - dy][x - dx].paste(world.tile(x, y)));
        // final paste, because of power nodes issues
        loop(stx, sty, edx, edy, (x, y) -> main[y - dy][x - dx].pasteConfig(world.tile(x, y)));
    }

    public void rotR(){
        flipX();
        flipD();
    }

    public void rotL(){
        flipY();
        flipD();
    }

    public void flipX(){
        loop(w / 2, h, (x, y) -> swap(x, y, w - x - 1, y));

        config(p -> p.x = -p.x, 2);

        loop(w, h, (x, y) -> flipConnections(x, y, -1, 0));
        loop(w, h, (x, y) -> flipMultiBlock(x, y, -1, 0));
    }


    public void flipY(){
        loop(w, h / 2, (x, y) -> swap(x, y, x, h - y - 1));

        config(p -> p.y = -p.y, 2);

        loop(w, h, (x, y) -> flipConnections(x, y, 0, -1));
        loop(w, h, (x, y) -> flipMultiBlock(x, y, 0, -1));
    }

    public void flipMultiBlock(int x, int y, int dx, int dy){
        CTile a = main[y][x];
        if(a.doNotFlip()) return;


        // can happen
        if(x == 0 || y == 0){
            a.build.block = null;
            return;
        }

        CTile b = main[y + dy][x + dx];
        CTile.Build tmp = a.build;
        a.build = b.build;
        b.build = tmp;
    }

    public void flipConnections(int x, int y, int dx, int dy){
        CTile a = main[y][x];
        boolean noFlip = a.doNotFlip();

        // this is driving me crazy, reason is that if both blocks have to be flipped or both not
        // do nothing, if a is flipped, go opposite of flip, if reverse go in direction of flip
        if(a.build.config instanceof Point2 p){
            if (!containsRaw(y+p.y, x+p.x)) return;
            CTile t = main[y+p.y][x+p.x];
            boolean oNoFlip = t.doNotFlip();
            if(oNoFlip && !noFlip) {
                p.sub(dx, dy);
            } else if(!oNoFlip && noFlip) {
                p.add(dx, dy);
            }
        }else if(a.build.config instanceof Point2[] points){
            for(Point2 p : points){
                if (!containsRaw(y+p.y, x+p.x)) continue;
                CTile t = main[y+p.y][x+p.x];
                boolean oNoFlip = t.doNotFlip();
                if(oNoFlip && !noFlip) {
                    p.sub(dx, dy);
                } else if(!oNoFlip && noFlip){
                    p.add(dx, dy);
                }
            }
        }
    }


    public void flipD(){
        config(p -> p.set(p.y, p.x), 1);

        loop(w, h, (x, y) -> swap(x, y, y, x, main, rotated));

        int t = w;
        w = h;
        h = t;

        CTile[][] u = main;
        main = rotated;
        rotated = u;
    }

    private void config(Cons<Point2> con, int shift){
        loop(w, h, t -> {
            t.build.rotation += shift;

            if(t.build.config instanceof Point2 p){
                con.get(p);
            }else if(t.build.config instanceof Point2[] ps){
                for(Point2 p : ps){
                    con.get(p);
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

    public void center(int x, int y) {
        select(dx + w / 2, dy + h / 2);
        move(x, y);
        deselect();
    }

    public void select(int x, int y) {
        selected = true;
        sx = x - dx;
        sy = y - dy;
    }

    public void deselect() {
        selected = false;
    }

    public void move(int x, int y){
        if(!selected) return;
        dx += x - (sx + dx);
        dy += y - (sy + dy);
    }

    public boolean contains(int x, int y){
        return dx <= x && dy <= y && dx + w >= x && dy + h >= y;
    }

    private boolean containsRaw(int x, int y){
        return 0 <= x && 0 <= y && w > x && h > y;
    }

    private int clampW(int x){
        return Mathf.clamp(x, 0, world.width() - 1);
    }

    private int clampH(int y){
        return Mathf.clamp(y, 0, world.height() - 1);
    }

    /** Data container used for transporting copy data. */
    private static class CTile{
        Block overlay;
        Floor floor;
        Build build = new Build();


        void copy(Tile t){
            floor = t.floor();
            overlay = t.overlay();
            build.block = t.block();
            if(!t.isCenter()){
                build.block = null;
            }else if(t.build != null){
                build.team = t.build.team();
                build.config = t.build.config();
                build.rotation = t.build.rotation();
            }
        }

        void paste(Tile t){
            if(build.block != null){
                t.setBlock(build.block, build.team, build.rotation);
            }
            t.setOverlay(overlay);
            t.setFloor(floor);
        }

        boolean doNotFlip() {
            return build.block == null || build.block.size % 2 == 1;
        }

        void pasteConfig(Tile t) {
            if(t.build != null){
                t.build.configure(build.config);
            }
        }

        private static class Build{
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
