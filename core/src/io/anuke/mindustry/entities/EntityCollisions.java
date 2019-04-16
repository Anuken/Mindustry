package io.anuke.mindustry.entities;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.entities.traits.Entity;
import io.anuke.mindustry.entities.traits.SolidTrait;

public class EntityCollisions{
    //range for tile collision scanning
    private static final int r = 1;
    //move in 1-unit chunks
    private static final float seg = 1f;

    //tile collisions
    private float tilesize;
    private Rectangle tmp = new Rectangle();
    private TileCollider collider;
    private TileHitboxProvider hitboxProvider;
    private Vector2 vector = new Vector2();
    private Vector2 l1 = new Vector2();
    private Rectangle r1 = new Rectangle();
    private Rectangle r2 = new Rectangle();

    //entity collisions
    private IntSet collided = new IntSet();
    private Array<SolidTrait> arrOut = new Array<>();

    public void setCollider(float tilesize, TileCollider collider, TileHitboxProvider hitbox){
        this.tilesize = tilesize;
        this.collider = collider;
        this.hitboxProvider = hitbox;
    }

    public void setCollider(float tilesize, TileCollider collider){
        setCollider(tilesize, collider, (x, y, out) -> out.setSize(tilesize).setCenter(x * tilesize, y * tilesize));
    }

    public void move(SolidTrait entity, float deltax, float deltay){

        boolean movedx = false;

        while(Math.abs(deltax) > 0 || !movedx){
            movedx = true;
            moveInternal(entity, Math.min(Math.abs(deltax), seg) * Mathf.sign(deltax), 0, true);

            if(Math.abs(deltax) >= seg){
                deltax -= seg * Mathf.sign(deltax);
            }else{
                deltax = 0f;
            }
        }

        boolean movedy = false;

        while(Math.abs(deltay) > 0 || !movedy){
            movedy = true;
            moveInternal(entity, 0, Math.min(Math.abs(deltay), seg) * Mathf.sign(deltay), false);

            if(Math.abs(deltay) >= seg){
                deltay -= seg * Mathf.sign(deltay);
            }else{
                deltay = 0f;
            }
        }
    }

    public void moveInternal(SolidTrait entity, float deltax, float deltay, boolean x){
        if(collider == null)
            throw new IllegalArgumentException("No tile collider specified! Call setCollider() first.");

        Rectangle rect = r1;
        entity.hitboxTile(rect);
        entity.hitboxTile(r2);
        rect.x += deltax;
        rect.y += deltay;

        int tilex = Math.round((rect.x + rect.width / 2) / tilesize), tiley = Math.round((rect.y + rect.height / 2) / tilesize);

        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                int wx = dx + tilex, wy = dy + tiley;
                if(collider.solid(wx, wy) && entity.collidesGrid(wx, wy)){

                    hitboxProvider.getHitbox(wx, wy, tmp);

                    if(tmp.overlaps(rect)){
                        Vector2 v = Geometry.overlap(rect, tmp, x);
                        rect.x += v.x;
                        rect.y += v.y;
                    }
                }
            }
        }

        entity.setX(entity.getX() + rect.x - r2.x);
        entity.setY(entity.getY() + rect.y - r2.y);
    }

    public boolean overlapsTile(Rectangle rect){
        if(collider == null)
            throw new IllegalArgumentException("No tile collider specified! Call setCollider() first.");

        rect.getCenter(vector);
        int r = 1;

        //assumes tiles are centered
        int tilex = Math.round(vector.x / tilesize);
        int tiley = Math.round(vector.y / tilesize);

        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                int wx = dx + tilex, wy = dy + tiley;
                if(collider.solid(wx, wy)){
                    hitboxProvider.getHitbox(wx, wy, r2);

                    if(r2.overlaps(rect)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public <T extends Entity> void updatePhysics(EntityGroup<T> group){
        collided.clear();

        QuadTree tree = group.tree();
        tree.clear();

        for(Entity entity : group.all()){
            if(entity instanceof SolidTrait){
                SolidTrait s = (SolidTrait)entity;
                s.lastPosition().set(s.getX(), s.getY());
                tree.insert(s);
            }
        }
    }

    private void checkCollide(Entity entity, Entity other){

        SolidTrait a = (SolidTrait)entity;
        SolidTrait b = (SolidTrait)other;

        a.hitbox(this.r1);
        b.hitbox(this.r2);

        r1.x += (a.lastPosition().x - a.getX());
        r1.y += (a.lastPosition().y - a.getY());
        r2.x += (b.lastPosition().x - b.getX());
        r2.y += (b.lastPosition().y - b.getY());

        float vax = a.getX() - a.lastPosition().x;
        float vay = a.getY() - a.lastPosition().y;
        float vbx = b.getX() - b.lastPosition().x;
        float vby = b.getY() - b.lastPosition().y;

        if(a != b && a.collides(b) && b.collides(a)){
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
                            float x2, float y2, float w2, float h2, float vx2, float vy2, Vector2 out){
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

    public void collideGroups(EntityGroup<?> groupa, EntityGroup<?> groupb){
        collided.clear();

        for(Entity entity : groupa.all()){
            if(!(entity instanceof SolidTrait) || collided.contains(entity.getID()))
                continue;

            SolidTrait solid = (SolidTrait)entity;

            solid.hitbox(r1);
            r1.x += (solid.lastPosition().x - solid.getX());
            r1.y += (solid.lastPosition().y - solid.getY());

            solid.hitbox(r2);
            r2.merge(r1);

            arrOut.clear();
            groupb.tree().getIntersect(arrOut, r2);

            for(SolidTrait sc : arrOut){
                sc.hitbox(r1);
                if(r2.overlaps(r1) && !collided.contains(sc.getID())){
                    checkCollide(entity, sc);
                }
            }

            collided.add(entity.getID());
        }
    }

    public interface TileCollider{
        boolean solid(int x, int y);
    }

    public interface TileHitboxProvider{
        void getHitbox(int x, int y, Rectangle out);
    }
}
