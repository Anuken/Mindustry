package mindustry.entities.type;

import arc.graphics.Color;
import arc.util.pooling.Pool.Poolable;
import arc.util.pooling.Pools;
import mindustry.entities.Effects;
import mindustry.entities.Effects.Effect;
import mindustry.entities.EntityGroup;
import mindustry.entities.traits.DrawTrait;
import mindustry.entities.traits.Entity;

import static mindustry.Vars.effectGroup;

public class EffectEntity extends TimedEntity implements Poolable, DrawTrait{
    public Effect effect;
    public Color color = new Color(Color.white);
    public Object data;
    public float rotation = 0f;

    public Entity parent;
    public float poffsetx, poffsety;

    /** For pooling use only! */
    public EffectEntity(){
    }

    public void setParent(Entity parent){
        this.parent = parent;
        this.poffsetx = x - parent.getX();
        this.poffsety = y - parent.getY();
    }

    @Override
    public EntityGroup targetGroup(){
        //this should never actually be called
        return effectGroup;
    }

    @Override
    public float lifetime(){
        return effect.lifetime;
    }

    @Override
    public float drawSize(){
        return effect.size;
    }

    @Override
    public void update(){
        if(effect == null){
            remove();
            return;
        }

        super.update();
        if(parent != null){
            x = parent.getX() + poffsetx;
            y = parent.getY() + poffsety;
        }
    }

    @Override
    public void reset(){
        effect = null;
        color.set(Color.white);
        rotation = time = poffsetx = poffsety = 0f;
        parent = null;
        data = null;
    }

    @Override
    public void draw(){
        Effects.renderEffect(id, effect, color, time, rotation, x, y, data);
    }

    @Override
    public void removed(){
        Pools.free(this);
    }
}
