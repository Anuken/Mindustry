package mindustry.async;

import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.async.PhysicsProcess.PhysicsWorld.*;
import mindustry.gen.*;

public class PhysicsProcess implements AsyncProcess{
    private PhysicsWorld physics;
    private Seq<PhysicRef> refs = new Seq<>(false);
    //currently only enabled for units
    private EntityGroup<? extends Physicsc> group = Groups.unit;

    @Override
    public void begin(){
        if(physics == null) return;

        //remove stale entities
        refs.removeAll(ref -> {
            if(!ref.entity.isAdded()){
                physics.remove(ref.body);
                ref.entity.physref(null);
                return true;
            }
            return false;
        });

        //find entities without bodies and assign them
        for(Physicsc entity : group){
            boolean grounded = entity.isGrounded();

            if(entity.physref() == null){
                PhysicsBody body = new PhysicsBody();
                body.x = entity.x();
                body.y = entity.y();
                body.mass = entity.mass();
                body.radius = entity.hitSize() / 2f;
                body.flag = grounded ? 1 : 0;

                PhysicRef ref = new PhysicRef(entity, body);
                refs.add(ref);

                entity.physref(ref);

                physics.add(body);
            }

            //save last position
            PhysicRef ref = entity.physref();

            ref.body.flag = grounded ? 1 : 0;
            ref.x = entity.x();
            ref.y = entity.y();
        }
    }

    @Override
    public void process(){
        if(physics == null) return;

        //get last position vectors before step
        for(PhysicRef ref : refs){
            //force set target position
            ref.body.x = ref.x;
            ref.body.y = ref.y;
        }

        physics.update();
    }

    @Override
    public void end(){
        if(physics == null) return;

        //move entities
        for(PhysicRef ref : refs){
            Physicsc entity = ref.entity;

            //move by delta
            entity.move(ref.body.x - ref.x, ref.body.y - ref.y);
        }
    }

    @Override
    public void reset(){
        if(physics != null){
            refs.clear();
            physics = null;
        }
    }

    @Override
    public void init(){
        reset();

        physics = new PhysicsWorld(Vars.world.getQuadBounds(new Rect()));
    }

    public static class PhysicRef{
        public Physicsc entity;
        public PhysicsBody body;
        public float x, y;

        public PhysicRef(Physicsc entity, PhysicsBody body){
            this.entity = entity;
            this.body = body;
        }
    }

    //world for simulating physics in a different thread
    public static class PhysicsWorld{
        //how much to soften movement by
        private static final float scl = 1.25f;

        private final QuadTree<PhysicsBody> tree;
        private final Seq<PhysicsBody> bodies = new Seq<>(false, 16, PhysicsBody.class);
        private final Rect rect = new Rect();
        private final Vec2 vec = new Vec2();

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
                PhysicsBody body = bodies.items[i];
                body.collided = false;
                tree.insert(body);
            }

            for(int i = 0; i < bodies.size; i++){
                PhysicsBody body = bodies.items[i];
                body.hitbox(rect);
                tree.intersect(rect, other -> {
                    if(other.flag != body.flag || other == body || other.collided) return;

                    float rs = body.radius + other.radius;
                    float dst = Mathf.dst(body.x, body.y, other.x, other.y);

                    if(dst < rs){
                        vec.set(body.x - other.x, body.y - other.y).setLength(rs - dst);
                        float ms = body.mass + other.mass;
                        float m1 = other.mass / ms, m2 = body.mass / ms;

                        body.x += vec.x * m1 / scl;
                        body.y += vec.y * m1 / scl;
                        other.x -= vec.x * m2 / scl;
                        other.y -= vec.y * m2 / scl;
                    }
                });
                body.collided = true;
            }
        }

        public static class PhysicsBody implements QuadTreeObject{
            public float x, y, radius, mass;
            public int flag = 0;
            public boolean collided = false;

            @Override
            public void hitbox(Rect out){
                out.setCentered(x, y, radius * 2, radius * 2);
            }
        }
    }
}
