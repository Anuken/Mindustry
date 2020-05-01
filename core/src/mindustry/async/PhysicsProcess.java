package mindustry.async;

import arc.*;
import arc.box2d.*;
import arc.box2d.BodyDef.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class PhysicsProcess implements AsyncProcess{
    private Physics physics;
    private Array<PhysicRef> refs = new Array<>(false);
    private BodyDef def;

    private EntityGroup<Unitc> group;
    private Filter flying = new Filter(){{
        maskBits = categoryBits = 2;
    }}, ground = new Filter(){{
        maskBits = categoryBits = 1;
    }};

    public PhysicsProcess(){
        def = new BodyDef();
        def.allowSleep = true;
        def.bullet = false;
        def.type = BodyType.DynamicBody;

        //currently only enabled for units
        group = Groups.unit;
    }

    @Override
    public void begin(){
        if(physics == null) return;

        //remove stale entities
        refs.removeAll(ref -> {
            if(!ref.entity.isAdded()){
                physics.destroyBody(ref.body);
                return true;
            }
            return false;
        });

        //find entities without bodies and assign them
        for(Unitc entity : group){
            boolean grounded = entity.isGrounded();

            if(entity.body() == null){
                //add bodies to entities that have none
                CircleShape shape = new CircleShape();
                shape.setRadius(entity.hitSize() / 2f);

                FixtureDef fd = new FixtureDef();
                fd.shape = shape;
                fd.density = 5.0f * entity.mass();
                fd.restitution = 0.05f;
                fd.filter.maskBits = fd.filter.categoryBits = (grounded ? ground : flying).maskBits;

                def.position.set(entity);

                Body body = physics.createBody(def);
                body.createFixture(fd);
                body.setUserData(entity);

                PhysicRef ref = new PhysicRef(entity, body);
                refs.add(ref);

                entity.body(ref);
            }

            //save last position
            PhysicRef ref = entity.body();

            if(ref.wasGround != grounded){
                //set correct filter
                ref.body.getFixtureList().first().setFilterData(grounded ? ground : flying);
                ref.wasGround = grounded;
            }

            ref.velocity.set(entity.deltaX(), entity.deltaY());
            ref.position.set(entity);
        }
    }

    @Override
    public void process(){
        if(physics == null) return;

        //get last position vectors before step
        for(PhysicRef ref : refs){
            //force set target position
            ref.body.setPosition(ref.position.x, ref.position.y);

            //save last position for delta
            ref.lastPosition.set(ref.body.getPosition());

            //write velocity
            ref.body.setLinearVelocity(ref.velocity);
        }

        physics.step(Core.graphics.getDeltaTime(), 8, 8);

        //get delta vectors
        for(PhysicRef ref : refs){
            //get delta vector
            ref.delta.set(ref.body.getPosition()).sub(ref.lastPosition);
        }
    }

    @Override
    public void end(){
        if(physics == null) return;

        //move entities
        for(PhysicRef ref : refs){
            Hitboxc entity = ref.entity;

            if(entity instanceof Velc){
                //move using velocity component move method TODO hack
                ((Velc)entity).move(ref.delta.x, ref.delta.y);
            }else{
                //move directly
                entity.trns(ref.delta.x, ref.delta.y);
            }

            //save last position
            ref.position.set(entity);
        }
    }

    @Override
    public void reset(){
        if(physics != null){
            refs.clear();
            physics.dispose();
            physics = null;
        }
    }

    @Override
    public void init(){
        reset();

        physics = new Physics(new Vec2(), true);
    }

    public static class PhysicRef{
        Hitboxc entity;
        Body body;
        boolean wasGround = true;
        Vec2 lastPosition = new Vec2(), delta = new Vec2(), velocity = new Vec2(), position = new Vec2();

        public PhysicRef(Hitboxc entity, Body body){
            this.entity = entity;
            this.body = body;
        }
    }
}
