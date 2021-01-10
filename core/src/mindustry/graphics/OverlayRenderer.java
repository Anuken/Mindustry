package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();
    private float buildFade, unitFade;
    private Unit lastSelect;

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

        Unit select = input.selectedUnit();
        if(!Core.input.keyDown(Binding.control)) select = null;
        unitFade = Mathf.lerpDelta(unitFade, Mathf.num(select != null), 0.1f);

        if(select != null) lastSelect = select;
        if(select == null) select = lastSelect;
        if(select != null && select.isAI()){
            Draw.mixcol(Pal.accent, 1f);
            Draw.alpha(unitFade);

            if(select instanceof BlockUnitc){
                //special selection for block "units"
                Fill.square(select.x, select.y, ((BlockUnitc)select).tile().block.size * tilesize/2f);
            }else{
                Draw.rect(select.type.icon(Cicon.full), select.x(), select.y(), select.rotation() - 90);
            }

            Lines.stroke(unitFade);
            Lines.square(select.x, select.y, select.hitSize() * 1.5f, Time.time * 2f);
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
            state.teams.eachEnemyCore(player.team(), core -> {
                float dst = core.dst(player);
                if(dst < state.rules.enemyCoreBuildRadius * 2.2f){
                    Draw.color(Color.darkGray);
                    Lines.circle(core.x, core.y - 2, state.rules.enemyCoreBuildRadius);
                    Draw.color(Pal.accent, core.team.color, 0.5f + Mathf.absin(Time.time, 10f, 0.5f));
                    Lines.circle(core.x, core.y, state.rules.enemyCoreBuildRadius);
                }
            });
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

        if(ui.hudfrag.blockfrag.hover() instanceof Unit unit && unit.controller() instanceof LogicAI ai && ai.controller instanceof Building build && build.isValid()){
            Drawf.square(build.x, build.y, build.block.size * tilesize/2f + 2f);
            if(!unit.within(build, unit.hitSize * 2f)){
                Drawf.arrow(unit.x, unit.y, build.x, build.y, unit.hitSize *2f, 4f);
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            if(player.unit().formation == null){
                Draw.rect(player.unit().item().icon(Cicon.medium), v.x, v.y, size, size);
            }
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time, 5f, 1f));
            Draw.reset();

            Building tile = world.buildWorld(v.x, v.y);
            if(input.canDropItem() && tile != null && tile.interactable(player.team()) && tile.canAcceptItems(player) && player.within(tile, itemTransferRange)){
                Lines.stroke(3f, Pal.gray);
                Lines.square(tile.x, tile.y, tile.block.size * tilesize / 2f + 3 + Mathf.absin(Time.time, 5f, 1f));
                Lines.stroke(1f, Pal.place);
                Lines.square(tile.x, tile.y, tile.block.size * tilesize / 2f + 2 + Mathf.absin(Time.time, 5f, 1f));
                Draw.reset();

            }
        }
    }
}
