package mindustry.world.blocks.payloads;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Generic building that produces other buildings. */
public class PayloadSource extends PayloadBlock{

    public PayloadSource(String name){
        super(name);

        size = 3;
        update = true;
        outputsPayload = true;
        hasPower = false;
        rotate = true;
        configurable = true;
        selectionRows = selectionColumns = 8;
        //make sure to display large units.
        clipSize = 120;
        noUpdateDisabled = true;
        clearOnDoubleTap = true;
        regionRotated1 = 1;
        commandable = true;

        config(Block.class, (PayloadSourceBuild build, Block block) -> {
            if(canProduce(block) && build.configBlock != block){
                build.configBlock = block;
                build.unit = null;
                build.payload = null;
                build.scl = 0f;
            }
        });

        config(UnitType.class, (PayloadSourceBuild build, UnitType unit) -> {
            if(canProduce(unit) && build.unit != unit){
                build.unit = unit;
                build.configBlock = null;
                build.payload = null;
                build.scl = 0f;
            }
        });

        configClear((PayloadSourceBuild build) -> {
            build.configBlock = null;
            build.unit = null;
            build.payload = null;
            build.scl = 0f;
        });
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    public boolean canProduce(Block b){
        return b.isVisible() && b.size < size && !(b instanceof CoreBlock) && !state.rules.isBanned(b) && b.environmentBuildable();
    }

    public boolean canProduce(UnitType t){
        return !t.isHidden() && !t.isBanned() && t.supportsEnv(state.rules.env);
    }

    public class PayloadSourceBuild extends PayloadBlockBuild<Payload>{
        public UnitType unit;
        public Block configBlock;
        public @Nullable Vec2 commandPos;
        public float scl;

        @Override
        public Vec2 getCommandPosition(){
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            commandPos = target;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(PayloadSource.this, table,
                content.blocks().select(PayloadSource.this::canProduce).<UnlockableContent>as()
                .add(content.units().select(PayloadSource.this::canProduce).as()),
            () -> (UnlockableContent)config(), this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Object config(){
            return unit == null ? configBlock : unit;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(payload == null){
                scl = 0f;
                if(unit != null){
                    payload = new UnitPayload(unit.create(team));

                    Unit p = ((UnitPayload)payload).unit;
                    if(commandPos != null && p.isCommandable()){
                        p.command().commandPosition(commandPos);
                    }

                    Events.fire(new UnitCreateEvent(p, this));
                }else if(configBlock != null){
                    payload = new BuildPayload(configBlock, team);
                }
                payVector.setZero();
                payRotation = rotdeg();
            }
            scl = Mathf.lerpDelta(scl, 1f, 0.1f);

            moveOutPayload();
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());
            Draw.rect(topRegion, x, y);

            Draw.scl(scl);
            drawPayload();
            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            write.s(configBlock == null ? -1 : configBlock.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            unit = Vars.content.unit(read.s());
            configBlock = Vars.content.block(read.s());
        }
    }
}
