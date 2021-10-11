package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();

    private float buildFade, unitFade;
    private Sized lastSelect;
    private Seq<CoreEdge> cedges = new Seq<>();
    private boolean updatedCores;

    public OverlayRenderer(){
        Events.on(WorldLoadEvent.class, e -> {
            updatedCores = true;
        });

        Events.on(CoreChangeEvent.class, e -> {
            updatedCores = true;
        });
    }

    private void updateCoreEdges(){
        if(!updatedCores){
            return;
        }

        updatedCores = false;
        cedges.clear();

        Seq<Vec2> pos = new Seq<>();
        Seq<CoreBuild> teams = new Seq<>();
        for(TeamData team : state.teams.active){
            for(CoreBuild b : team.cores){
                teams.add(b);
                pos.add(new Vec2(b.x, b.y));
            }
        }

        //if this is laggy, it could be shoved in another thread.
        var result = Voronoi.generate(pos.toArray(Vec2.class), 0, world.unitWidth(), 0, world.unitHeight());
        for(var edge : result){
            cedges.add(new CoreEdge(edge.x1, edge.y1, edge.x2, edge.y2, teams.get(edge.site1).team, teams.get(edge.site2).team));
        }
    }

    public void drawBottom(){
        InputHandler input = control.input;

        if(player.dead()) return;

        if(player.isBuilder()){
            player.unit().drawBuildPlans();
        }

        input.drawBottom();
    }

    public void drawTop(){

        if(!player.dead() && ui.hudfrag.shown){
            if(Core.settings.getBool("playerindicators")){
                for(Player player : Groups.player){
                    if(Vars.player != player && Vars.player.team() == player.team()){
                        if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                        .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                            Tmp.v1.set(player).sub(Vars.player).setLength(indicatorLength);

                            Lines.stroke(2f, Vars.player.team().color);
                            Lines.lineAngle(Vars.player.x + Tmp.v1.x, Vars.player.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                            Draw.reset();
                        }
                    }
                }
            }

            if(Core.settings.getBool("indicators")){
                Groups.unit.each(unit -> {
                    if(!unit.isLocal() && unit.team != player.team() && !rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                    .setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x, unit.y)){
                        Tmp.v1.set(unit.x, unit.y).sub(player).setLength(indicatorLength);

                        Lines.stroke(1f, unit.team().color);
                        Lines.lineAngle(player.x + Tmp.v1.x, player.y + Tmp.v1.y, Tmp.v1.angle(), 3f);
                        Draw.reset();
                    }
                });
            }
        }

        if(player.dead()) return; //dead players don't draw

        InputHandler input = control.input;


        Sized select = input.selectedUnit();
        if(select == null) select = input.selectedControlBuild();
        if(!Core.input.keyDown(Binding.control)) select = null;

        unitFade = Mathf.lerpDelta(unitFade, Mathf.num(select != null), 0.1f);

        if(select != null) lastSelect = select;
        if(select == null) select = lastSelect;
        if(select != null && (!(select instanceof Unitc u) || u.isAI())){
            Draw.mixcol(Pal.accent, 1f);
            Draw.alpha(unitFade);
            Building build = (select instanceof BlockUnitc b ? b.tile() : select instanceof Building b ? b : null);

            if(build != null){
                //special selection for block "units"
                Fill.square(build.x, build.y, build.block.size * tilesize/2f);
            }else if(select instanceof Unit u){
                Draw.rect(u.type.fullIcon, u.x, u.y, u.rotation - 90);
            }

            for(int i = 0; i < 4; i++){
                float rot = i * 90f + 45f + (-Time.time) % 360f;
                float length = select.hitSize() * 1.5f + (unitFade * 2.5f);
                Draw.rect("select-arrow", select.getX() + Angles.trnsx(rot, length), select.getY() + Angles.trnsy(rot, length), length / 1.9f, length / 1.9f, rot - 135f);
            }

            Draw.reset();
        }

        //draw config selected block
        if(input.frag.config.isShown()){
            Building tile = input.frag.config.getSelectedTile();
            tile.drawConfigure();
        }

        input.drawTop();

        buildFade = Mathf.lerpDelta(buildFade, input.isPlacing() || input.isUsingSchematic() ? 1f : 0f, 0.06f);

        Draw.reset();
        Lines.stroke(buildFade * 2f);

        if(buildFade > 0.005f){
            if(state.rules.polygonCoreProtection){
                updateCoreEdges();
                Draw.color(Pal.accent);

                for(int i = 0; i < 2; i++){
                    float offset = (i == 0 ? -2f : 0f);
                    for(CoreEdge edge : cedges){
                        Team displayed = edge.displayed();
                        if(displayed != null){
                            Draw.color(i == 0 ? Color.darkGray : Tmp.c1.set(displayed.color).lerp(Pal.accent, Mathf.absin(Time.time, 10f, 0.2f)));
                            Lines.line(edge.x1, edge.y1 + offset, edge.x2, edge.y2 + offset);
                        }
                    }
                }

                Draw.color();
            }else{
                state.teams.eachEnemyCore(player.team(), core -> {
                    if(Core.camera.bounds(Tmp.r1).overlaps(Tmp.r2.setCentered(core.x, core.y, state.rules.enemyCoreBuildRadius * 2f))){
                        Draw.color(Color.darkGray);
                        Lines.circle(core.x, core.y - 2, state.rules.enemyCoreBuildRadius);
                        Draw.color(Pal.accent, core.team.color, 0.5f + Mathf.absin(Time.time, 10f, 0.5f));
                        Lines.circle(core.x, core.y, state.rules.enemyCoreBuildRadius);
                    }
                });
            }
        }

        Lines.stroke(2f);
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));

        if(state.hasSpawns()){
            for(Tile tile : spawner.getSpawns()){
                if(tile.within(player.x, player.y, state.rules.dropZoneRadius + spawnerMargin)){
                    Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                    Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                }
            }
        }

        Draw.reset();

        //draw selected block
        if(input.block == null && !Core.scene.hasMouse()){
            Vec2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            Building build = world.buildWorld(vec.x, vec.y);

            if(build != null && build.team == player.team()){
                build.drawSelect();
                if(!build.enabled && build.block.drawDisabled){
                   build.drawDisabled();
                }

                if(Core.input.keyDown(Binding.rotateplaced) && build.block.rotate && build.block.quickRotate && build.interactable(player.team())){
                    control.input.drawArrow(build.block, build.tileX(), build.tileY(), build.rotation, true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(build.x, build.y, build.block.size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        input.drawOverSelect();

        if(ui.hudfrag.blockfrag.hover() instanceof Unit unit && unit.controller() instanceof LogicAI ai && ai.controller != null && ai.controller.isValid()){
            var build = ai.controller;
            Drawf.square(build.x, build.y, build.block.size * tilesize/2f + 2f);
            if(!unit.within(build, unit.hitSize * 2f)){
                Drawf.arrow(unit.x, unit.y, build.x, build.y, unit.hitSize *2f, 4f);
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            Draw.rect(player.unit().item().fullIcon, v.x, v.y, size, size);
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time, 5f, 1f));
            Draw.reset();

            Building tile = world.buildWorld(v.x, v.y);
            if(input.canDropItem() && tile != null && tile.interactable(player.team()) && tile.acceptStack(player.unit().item(), player.unit().stack.amount, player.unit()) > 0 && player.within(tile, itemTransferRange)){
                Lines.stroke(3f, Pal.gray);
                Lines.square(tile.x, tile.y, tile.block.size * tilesize / 2f + 3 + Mathf.absin(Time.time, 5f, 1f));
                Lines.stroke(1f, Pal.place);
                Lines.square(tile.x, tile.y, tile.block.size * tilesize / 2f + 2 + Mathf.absin(Time.time, 5f, 1f));
                Draw.reset();

            }
        }
    }

    private static class CoreEdge{
        float x1, y1, x2, y2;
        Team t1, t2;

        public CoreEdge(float x1, float y1, float x2, float y2, Team t1, Team t2){
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.t1 = t1;
            this.t2 = t2;
        }

        @Nullable
        Team displayed(){
            return
                t1 == t2 ? null :
                t1 == player.team() ? t2 :
                t2 == player.team() ? t1 :
                t2.id == 0 ? t1 :
                t1.id < t2.id && t1.id != 0 ? t1 : t2;
        }
    }
}
