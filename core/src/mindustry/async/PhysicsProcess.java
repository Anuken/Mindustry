package mindustry.async;

import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.async.PhysicsProcess.PhysicsWorld.*;
import mindustry.core.*;
import mindustry.gen.*;

import java.util.concurrent.*;

public class PhysicsProcess implements AsyncProcess{
    static final int mobileIterations = 1, desktopIterations = 1;

    public static final int
    layers = 4,
    layerGround = 0,
    layerLegs = 1,
    layerFlying = 2,
    layerUnderwater = 3;

    //one independent world per layer; layers never interact, so each can run on its own thread
    private PhysicsWorld[] physics;
    private Seq<PhysicRef> refs = new Seq<>(false, 20, PhysicRef.class);
    private Seq<Future<?>> futures = new Seq<>(false, layers, Future.class);

    private static volatile long maxPhysicsTime = 0;

    public void add(Unit unit){
        if(unit == null || unit.type == null || !unit.type.physics || unit.hasPhysicsRef) return;
        if(physics == null) init();

        unit.hasPhysicsRef = true;

        PhysicsBody body = new PhysicsBody();
        body.x = unit.x;
        body.y = unit.y;
        body.mass = unit.mass();
        body.radius = unit.hitSize * Vars.unitCollisionRadiusScale;

        PhysicRef ref = new PhysicRef(unit, body);
        refs.add(ref);

        if(ref.lastLayer >= 0) physics[ref.lastLayer].add(body);
    }

    @Override
    public void begin(){
        if(physics == null) return;
        boolean local = !Vars.net.client();

        PerfCounter.unitPhysicsWait.begin();

        //wait for every layer's async step to finish before touching body positions
        for(int i = 0; i < futures.size; i++){
            try{
                futures.items[i].get();
            }catch(InterruptedException | ExecutionException e){
                throw new RuntimeException(e);
            }
        }
        futures.clear();

        PerfCounter.unitPhysicsWait.end();
        PerfCounter.unitPhysicsAsync.add(maxPhysicsTime);
        maxPhysicsTime = 0;

        //move entities
        for(PhysicRef ref : refs){
            Physicsc entity = ref.entity;

            //move by delta
            entity.move(ref.body.x - ref.startX, ref.body.y - ref.startY);
        }

        var items = refs.items;

        for(int i = 0; i < refs.size; i++){
            PhysicRef ref = items[i];
            //stale entity, remove it
            if(!ref.entity.isAdded()){
                ref.entity.hasPhysicsRef = false;
                refs.remove(i);
                i --;

                if(ref.lastLayer >= 0) physics[ref.lastLayer].remove(ref.body);
            }else{
                //check if layer changed
                int newLayer = ref.entity.collisionLayer();
                if(newLayer != ref.lastLayer){
                    if(ref.lastLayer >= 0) physics[ref.lastLayer].remove(ref.body);
                    ref.lastLayer = newLayer;
                    if(newLayer >= 0) physics[newLayer].add(ref.body);
                }

                ref.startX = ref.body.x = ref.entity.x;
                ref.startY = ref.body.y = ref.entity.y;
                ref.body.local = local || ref.entity.isLocal();
            }
        }

        //one task per world
        futures.clear();
        for(int i = 0; i < layers; i++){
            PhysicsWorld world = physics[i];
            futures.add(Vars.mainExecutor.submit(world::update));
        }
    }

    @Override
    public boolean shouldProcess(){
        return false;
    }

    @Override
    public void reset(){
        if(physics != null){
            refs.clear();
            futures.clear();
            physics = null;
        }
    }

    @Override
    public void init(){
        reset();

        Rect bounds = Vars.world.getQuadBounds(new Rect());
        physics = new PhysicsWorld[layers];
        for(int i = 0; i < layers; i++){
            physics[i] = new PhysicsWorld(bounds);
        }
    }

    public static class PhysicRef{
        public Unit entity;
        public PhysicsBody body;
        //position before simulation start
        public float startX, startY;
        public int lastLayer;

        public PhysicRef(Unit entity, PhysicsBody body){
            this.entity = entity;
            this.body = body;
            this.lastLayer = entity.collisionLayer();
            startX = entity.x;
            startY = entity.y;
        }
    }

    //world for simulating a single collision layer's physics, meant to run on its own thread
    public static class PhysicsWorld{
        //how much to soften movement by
        private static final float scl = 1.25f;

        private final QuadTree<PhysicsBody> tree;
        private final Seq<PhysicsBody> bodies = new Seq<>(false, 16, PhysicsBody.class);
        private final Seq<PhysicsBody> seq = new Seq<>(PhysicsBody.class);
        private final Vec2 vec = new Vec2();
        private final Rand rand = new Rand();

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
            long begin = Time.nanos();

            var bodyItems = bodies.items;
            int bodySize = bodies.size;

            int iterations = Vars.net.client() || OS.isMobile ? mobileIterations : desktopIterations;

            for(int iter = 0; iter < iterations; iter++){
                tree.fill(bodies);

                for(int i = 0; i < bodySize; i++){
                    bodyItems[i].collided = false;
                }

                for(int i = 0; i < bodySize; i++){
                    PhysicsBody body = bodyItems[i];
                    //for clients, the only body that collides is the local one; all other physics simulations are handled by the server.
                    if(!body.local) continue;

                    seq.size = 0;
                    tree.intersect(body.x - body.radius, body.y - body.radius, body.radius * 2, body.radius * 2, seq);
                    int size = seq.size;
                    var items = seq.items;

                    for(int j = 0; j < size; j++){
                        PhysicsBody other = items[j];

                        if(other == body || other.collided) continue;

                        float rs = body.radius + other.radius;
                        float dx = body.x - other.x, dy = body.y - other.y;
                        float dst2 = dx * dx + dy * dy;

                        //skip the sqrt entirely for non-colliding pairs
                        if(dst2 < rs * rs){
                            float dst = Mathf.sqrt(dst2);
                            vec.set(dx, dy);

                            if(vec.isZero()){ //exact stacked bodies will move in random directions away from each other
                                vec.trns(rand.random(360f), rs - dst);
                            }else{
                                vec.setLength(rs - dst);
                            }

                            float ms = body.mass + other.mass;
                            float m1 = other.mass / ms, m2 = body.mass / ms;

                            //first body is always local due to guard check above
                            body.x += vec.x * m1 / scl;
                            body.y += vec.y * m1 / scl;

                            if(other.local){
                                other.x -= vec.x * m2 / scl;
                                other.y -= vec.y * m2 / scl;

                            }
                        }
                    }

                    body.collided = true;
                }
            }

            maxPhysicsTime = Math.max(maxPhysicsTime, Time.timeSinceNanos(begin));
        }

        public static class PhysicsBody implements QuadTreeObject{
            public float x, y, radius, mass;
            public boolean collided = false, local = true;

            @Override
            public void hitbox(Rect out){
                out.setCentered(x, y, radius * 2, radius * 2);
            }
        }
    }
}