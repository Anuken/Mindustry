package mindustry.entities.type.base;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;

public class CraterUnit extends GroundUnit{

    public final Effect io = Fx.plasticburn;
    public int inactivity = 0;

    public final UnitState

    load = new UnitState(){
        public void update(){
            if(item().amount >= getItemCapacity() || !velocity.isZero(1f) || inactivity++ > 120) state.set(move);
        }
    },
    move = new UnitState(){
        public void update(){
            velocity.add(vec.trnsExact(angleTo(on().front()), type.speed * Time.delta()));
            rotation = Mathf.slerpDelta(rotation, baseRotation, type.rotatespeed);

            if(dst(on()) < 2.5f && on().block() instanceof CompressedConveyor && ((CompressedConveyor) on().block()).end(on())){
                state.set(unload);
            }
        }
    },
    unload = new UnitState(){
        public void update(){

            if(on().block() instanceof CompressedConveyor && !((CompressedConveyor)on().block()).end(on())){
                state.set(move);
                return;
            }

            if(item().amount-- > 0){
                int rot = on().rotation();
                on().block().offloadNear(on(), item().item);
                on().rotation(rot);
            }
        }
    };

    @Override
    public UnitState getStartState(){
        return load;
    }

    @Override
    public void drawStats(){
        if(item.amount > 0) drawBackItems();
        drawLight();
    }

    @Override
    public void update(){
        super.update();

        if(on() == null || !on().block().compressable || item.amount == 0){
            Effects.effect(io, x, y);
            kill();
        }
    }

    @Override
    public void added(){
        super.added();
        Effects.effect(io, x, y);
        baseRotation = rotation;
    }

    @Override
    public void onDeath(){
        Events.fire(new UnitDestroyEvent(this));
    }

    @Override
    public boolean isCommanded(){
        return false;
    }

    public Tile on(){
        return world.ltileWorld(x, y);
    }

    private void drawBackItems(){
        float itemtime = 0.5f;
        float backTrns = 0f;

        float size = itemSize / 1.5f;

        Draw.rect(item.item.icon(Cicon.medium),
        x + Angles.trnsx(rotation + 180f, backTrns),
        y + Angles.trnsy(rotation + 180f, backTrns),
        size, size, rotation);

        Fonts.outline.draw(item.amount + "",
        x + Angles.trnsx(rotation + 180f, backTrns),
        y + Angles.trnsy(rotation + 180f, backTrns) - 1,
        Pal.accent, 0.25f * itemtime / Scl.scl(1f), false, Align.center);

        Draw.reset();
    }

    public boolean loading(){
        return state.is(load);
    }

    /**
     * Since normal conveyors get faster when boosted,
     * this piece of code changes their capacity,
     * make sure capacity is dividable by 4,
     * for the best user experience.
     */
    @Override
    public int getItemCapacity(){

        if(on() == null || on().entity == null) return type.itemCapacity;

        return Mathf.round(type.itemCapacity * on().entity.timeScale);
    }
}
