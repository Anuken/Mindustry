package mindustry.async;

import arc.*;
import arc.box2d.*;
import arc.box2d.BodyDef.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import mindustry.entities.*;
import mindustry.gen.*;

/**
 * Processes collisions.
 *
 * Each entity is assigned a final filter layer, then given a body and inserted into a physics world.
 *
 * Async:
 * The body's position is set to its entity position, and the body velocity is set to the entity delta.
 * Collisions are resolved and stored in a list, then processed synchronously.
 *
 */
public class CollisionProcess implements AsyncProcess, ContactListener, ContactFilter{
    private Pool<CollisionRef> pool = Pools.get(CollisionRef.class, CollisionRef::new);

    private Physics physics;
    private Array<CollisionRef> refs = new Array<>(false);
    private BodyDef def;
    private FixtureDef fdef;

    private EntityGroup<? extends Collisionc> group = Groups.collision;
    private Array<Collisionc> collisions = new Array<>(Collisionc.class);

    public CollisionProcess(){
        def = new BodyDef();
        def.type = BodyType.DynamicBody;

        fdef = new FixtureDef();
        fdef.density = 1;
        fdef.isSensor = true;
    }

    @Override
    public void begin(){
        if(physics == null) return;

        //remove stale entities
        refs.removeAll(ref -> {
            if(!ref.entity.isAdded()){
                physics.destroyBody(ref.body);
                pool.free(ref);
                return true;
            }
            return false;
        });

        collisions.clear();

        //find entities without bodies and assign them
        for(Collisionc entity : group){
            if(entity.colref() == null){
                //add bodies to entities that have none
                fdef.shape = new CircleShape(entity.hitSize() / 2f);

                def.position.set(entity);

                Body body = physics.createBody(def);
                body.createFixture(fdef);

                CollisionRef ref = pool.obtain().set(entity, body);
                refs.add(ref);

                body.setUserData(ref);
                entity.colref(ref);
            }

            //save last position
            CollisionRef ref = entity.colref();
            ref.position.set(entity);
        }
    }

    @Override
    public void process(){
        if(physics == null) return;

        collisions.clear();

        Time.mark();

        //get last position vectors before step
        for(CollisionRef ref : refs){
            //force set target position
            ref.body.setPosition(ref.position.x, ref.position.y);

            //write velocity
            ref.body.setLinearVelocity(ref.velocity);
        }

        physics.step(Core.graphics.getDeltaTime(), 2, 2);
    }

    @Override
    public void end(){
        if(physics == null) return;

        //processes anything that happened
        for(int i = 0; i < collisions.size; i += 2){
            Collisionc a = collisions.items[i];
            Collisionc b = collisions.items[i + 1];

            //TODO incorrect
            float cx = (a.x() + b.x())/2f, cy = (a.y() + b.y())/2f;

            a.collision(b, cx, cy);
            b.collision(a, cx, cy);
        }

        //update velocity state based on frame movement
        for(CollisionRef ref : refs){
            ref.velocity.set(ref.entity).sub(ref.position);
        }
    }

    @Override
    public void reset(){
        if(physics != null){
            pool.freeAll(refs);
            refs.clear();
            physics.dispose();
            physics = null;
        }
    }

    @Override
    public void init(){
        reset();

        physics = new Physics(new Vec2(), true);
        physics.setContactListener(this);
        physics.setContactFilter(this);
    }

    @Override
    public void beginContact(Contact contact){
        CollisionRef a = contact.getFixtureA().getBody().getUserData();
        CollisionRef b = contact.getFixtureB().getBody().getUserData();

        //save collision
        collisions.add(a.entity, b.entity);
    }

    @Override
    public void endContact(Contact contact){

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold){

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse){

    }

    @Override
    public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB){
        CollisionRef a = fixtureA.getBody().getUserData();
        CollisionRef b = fixtureB.getBody().getUserData();

        //note that this method is called in a different thread, but for simple collision checks state doesn't matter too much
        return a != b && a.entity.collides(b.entity) && b.entity.collides(a.entity);
    }

    public static class CollisionRef implements Poolable{
        Collisionc entity;
        Body body;
        Vec2 velocity = new Vec2(), position = new Vec2();

        public CollisionRef set(Collisionc entity, Body body){
            this.entity = entity;
            this.body = body;

            position.set(entity);
            return this;
        }

        @Override
        public void reset(){
            entity = null;
            body = null;

            velocity.setZero();
            position.setZero();
        }
    }
}
