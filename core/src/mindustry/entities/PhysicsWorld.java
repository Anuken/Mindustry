package mindustry.entities;

import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;

public class PhysicsWorld{
    private QuadTree<PhysicsBody> tree;
    private Seq<PhysicsBody> bodies = new Seq<>(false, 16, PhysicsBody.class);
    private Rect rect = new Rect();

    public PhysicsWorld(Rect bounds){
        tree = new QuadTree<>(new Rect(bounds));
    }

    public void add(PhysicsBody body){
        bodies.add(body);
    }

    public void remove(PhysicsBody body){
        bodies.remove(body);
    }

    public void update(){
        tree.clear();
        for(int i = 0; i < bodies.size; i++){
            tree.insert(bodies.items[i]);
        }

        for(int i = 0; i < bodies.size; i++){
            PhysicsBody body = bodies.items[i];
            body.hitbox(rect);
            tree.intersect(rect, other -> {

            });
        }
    }

    public static class PhysicsBody implements QuadTreeObject{
        public float x, y, xv, yv, radius, mass;
        public int flag = 0;

        @Override
        public void hitbox(Rect out){
            out.setCentered(x, y, radius * 2, radius * 2);
        }
    }
}
