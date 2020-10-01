package mindustry.entities;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class EntityCollisions{
    //range for tile collision scanning
    private static final int r = 1;
    //move in 1-unit chunks
    private static final float seg = 1f;

    //tile collisions
    private Rect tmp = new Rect();
    private Vec2 vector = new Vec2();
    private Vec2 l1 = new Vec2();
    private Rect r1 = new Rect();
    private Rect r2 = new Rect();

    //entity collisions
    private Seq<Hitboxc> arrOut = new Seq<>();

    public void moveCheck(Hitboxc entity, float deltax, float deltay, SolidPred solidCheck){
        if(!solidCheck.solid(entity.tileX(), entity.tileY())){
            move(entity, deltax, deltay, solidCheck);
        }
    }

    public void move(Hitboxc entity, float deltax, float deltay){
        move(entity, deltax, deltay, EntityCollisions::solid);
    }

    public void move(Hitboxc entity, float deltax, float deltay, SolidPred solidCheck){
        if(Math.abs(deltax) < 0.0001f & Math.abs(deltay)  < 0.0001f) return;

        boolean movedx = false;

        while(Math.abs(deltax) > 0 || !movedx){
            movedx = true;
            moveDelta(entity, Math.min(Math.abs(deltax), seg) * Mathf.sign(deltax), 0, true, solidCheck);

            if(Math.abs(deltax) >= seg){
                deltax -= seg * Mathf.sign(deltax);
            }else{
                deltax = 0f;
            }
        }

        boolean movedy = false;

        while(Math.abs(deltay) > 0 || !movedy){
            movedy = true;
            moveDelta(entity, 0, Math.min(Math.abs(deltay), seg) * Mathf.sign(deltay), false, solidCheck);

            if(Math.abs(deltay) >= seg){
                deltay -= seg * Mathf.sign(deltay);
            }else{
                deltay = 0f;
            }
        }
    }

    public void moveDelta(Hitboxc entity, float deltax, float deltay, boolean x, SolidPred solidCheck){
        entity.hitboxTile(r1);
        entity.hitboxTile(r2);
        r1.x += deltax;
        r1.y += deltay;

        int tilex = Math.round((r1.x + r1.width / 2) / tilesize), tiley = Math.round((r1.y + r1.height / 2) / tilesize);

        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                int wx = dx + tilex, wy = dy + tiley;
                if(solidCheck.solid(wx, wy)){
                    tmp.setSize(tilesize).setCenter(wx * tilesize, wy * tilesize);

                    if(tmp.overlaps(r1)){
                        Vec2 v = Geometry.overlap(r1, tmp, x);
                        if(x) r1.x += v.x;
                        if(!x) r1.y += v.y;
                    }
                }
            }
        }

        entity.trns(r1.x - r2.x, r1.y - r2.y);
    }

    public boolean overlapsTile(Rect rect){
        rect.getCenter(vector);
        int r = 1;

        //assumes tiles are centered
        int tilex = Math.round(vector.x / tilesize);
        int tiley = Math.round(vector.y / tilesize);

        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                int wx = dx + tilex, wy = dy + tiley;
                if(solid(wx, wy)){
                    r2.setSize(tilesize).setCenter(wx * tilesize, wy * tilesize);

                    if(r2.overlaps(rect)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends Hitboxc> void updatePhysics(EntityGroup<T> group){

        QuadTree tree = group.tree();
        tree.clear();

        group.each(s -> {
            s.updateLastPosition();
            tree.insert(s);
        });
    }

    public static boolean legsSolid(int x, int y){
        Tile tile = world.tile(x, y);
        return tile == null || tile.staticDarkness() >= 2;
    }

    public static boolean waterSolid(int x, int y){
        Tile tile = world.tile(x, y);
        return tile == null || (tile.solid() || !tile.floor().isLiquid);
    }

    public static boolean solid(int x, int y){
        Tile tile = world.tile(x, y);
        return tile == null || tile.solid();
    }

    private void checkCollide(Hitboxc a, Hitboxc b){

        a.hitbox(this.r1);
        b.hitbox(this.r2);

        r1.x += (a.lastX() - a.getX());
        r1.y += (a.lastY() - a.getY());
        r2.x += (b.lastX() - b.getX());
        r2.y += (b.lastY() - b.getY());

        float vax = a.getX() - a.lastX();
        float vay = a.getY() - a.lastY();
        float vbx = b.getX() - b.lastX();
        float vby = b.getY() - b.lastY();

        if(a != b && a.collides(b)){
            l1.set(a.getX(), a.getY());
            boolean collide = r1.overlaps(r2) || collide(r1.x, r1.y, r1.width, r1.height, vax, vay,
            r2.x, r2.y, r2.width, r2.height, vbx, vby, l1);
            if(collide){
                a.collision(b, l1.x, l1.y);
                b.collision(a, l1.x, l1.y);
            }
        }
    }

    private boolean collide(float x1, float y1, float w1, float h1, float vx1, float vy1,
                            float x2, float y2, float w2, float h2, float vx2, float vy2, Vec2 out){
        float px = vx1, py = vy1;

        vx1 -= vx2;
        vy1 -= vy2;

        float xInvEntry, yInvEntry;
        float xInvExit, yInvExit;

        if(vx1 > 0.0f){
            xInvEntry = x2 - (x1 + w1);
            xInvExit = (x2 + w2) - x1;
        }else{
            xInvEntry = (x2 + w2) - x1;
            xInvExit = x2 - (x1 + w1);
        }

        if(vy1 > 0.0f){
            yInvEntry = y2 - (y1 + h1);
            yInvExit = (y2 + h2) - y1;
        }else{
            yInvEntry = (y2 + h2) - y1;
            yInvExit = y2 - (y1 + h1);
        }

        float xEntry, yEntry;
        float xExit, yExit;

        xEntry = xInvEntry / vx1;
        xExit = xInvExit / vx1;

        yEntry = yInvEntry / vy1;
        yExit = yInvExit / vy1;

        float entryTime = Math.max(xEntry, yEntry);
        float exitTime = Math.min(xExit, yExit);

        if(entryTime > exitTime || xExit < 0.0f || yExit < 0.0f || xEntry > 1.0f || yEntry > 1.0f){
            return false;
        }else{
            float dx = x1 + w1 / 2f + px * entryTime;
            float dy = y1 + h1 / 2f + py * entryTime;

            out.set(dx, dy);

            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Hitboxc> void collide(EntityGroup<T> groupa){

        groupa.each(solid -> {
            solid.hitbox(r1);
            r1.x += (solid.lastX() - solid.getX());
            r1.y += (solid.lastY() - solid.getY());

            solid.hitbox(r2);
            r2.merge(r1);

            arrOut.clear();

            //get all targets based on what entity wants to collide with
            solid.getCollisions(tree -> tree.intersect(r2, arrOut));

            for(Hitboxc sc : arrOut){
                sc.hitbox(r1);
                if(r2.overlaps(r1)){
                    checkCollide(solid, sc);
                    //break out of loop when this object hits something
                    if(!solid.isAdded()) return;
                }
            }
        });
    }

    public interface SolidPred{
        boolean solid(int x, int y);
    }
}
