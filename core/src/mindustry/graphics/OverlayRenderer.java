package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();
    private float buildFadeTime;

    public void drawBottom(){
        InputHandler input = control.input;

        if(player.isDead()) return;

        input.drawBottom();
    }

    public void drawTop(){

        if(Core.settings.getBool("indicators")){
            for(Player player : playerGroup.all()){
                if(Vars.player != player && Vars.player.getTeam() == player.getTeam()){
                    if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                    .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                        Tmp.v1.set(player.x, player.y).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                        Lines.stroke(2f, player.getTeam().color);
                        Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                        Draw.reset();
                    }
                }
            }

            Units.all(unit -> {
                if(unit != player && unit.getTeam() != player.getTeam() && !rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f).setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x, unit.y)){
                    Tmp.v1.set(unit.x, unit.y).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                    Lines.stroke(1f, unit.getTeam().color);
                    Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 3f);
                    Draw.reset();
                }
            });

            if(ui.hudfrag.blockfrag.currentCategory == Category.upgrade){
                for(Tile mechpad : indexer.getAllied(player.getTeam(), BlockFlag.mechPad)){
                    if(!(mechpad.block() instanceof MechPad)) continue;
                    if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                            .setCenter(Core.camera.position.x, Core.camera.position.y).contains(mechpad.drawx(), mechpad.drawy())){

                        Tmp.v1.set(mechpad.drawx(), mechpad.drawy()).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                        Lines.stroke(2f, ((MechPad) mechpad.block()).mech.engineColor);
                        Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 0.5f);
                        Draw.reset();
                    }
                }
            }
        }

        if(player.isDead()) return; //dead players don't draw

        InputHandler input = control.input;

        //draw config selected block
        if(input.frag.config.isShown()){
            Tile tile = input.frag.config.getSelectedTile();
            tile.block().drawConfigure(tile);
        }

        input.drawTop();

        buildFadeTime = Mathf.lerpDelta(buildFadeTime, input.isPlacing() ? 1f : 0f, 0.06f);

        Draw.reset();
        Lines.stroke(buildFadeTime * 2f);

        if(buildFadeTime > 0.005f){
            state.teams.eachEnemyCore(player.getTeam(), core -> {
                float dst = core.dst(player);
                if(dst < state.rules.enemyCoreBuildRadius * 2.2f){
                    Draw.color(Color.darkGray);
                    Lines.circle(core.x, core.y - 2, state.rules.enemyCoreBuildRadius);
                    Draw.color(Pal.accent, core.getTeam().color, 0.5f + Mathf.absin(Time.time(), 10f, 0.5f));
                    Lines.circle(core.x, core.y, state.rules.enemyCoreBuildRadius);
                }
            });
        }

        Lines.stroke(2f);
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time(), 8f, 1f));

        for(Tile tile : spawner.getGroundSpawns()){
            if(tile.withinDst(player.x, player.y, state.rules.dropZoneRadius + spawnerMargin)){
                Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
            }
        }

        Draw.reset();

        //draw selected block
        if(input.block == null && !Core.scene.hasMouse()){
            Vec2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            Tile tile = world.ltileWorld(vec.x, vec.y);

            if(tile != null && tile.block() != Blocks.air && tile.getTeam() == player.getTeam()){
                tile.block().drawSelect(tile);

                if(Core.input.keyDown(Binding.rotateplaced) && tile.block().rotate && tile.interactable(player.getTeam())){
                    control.input.drawArrow(tile.block(), tile.x, tile.y, tile.rotation(), true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            Draw.rect(player.item().item.icon(Cicon.medium), v.x, v.y, size, size);
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time(), 5f, 1f));
            Draw.reset();

            Tile tile = world.ltileWorld(v.x, v.y);
            if(tile != null && tile.interactable(player.getTeam()) && tile.block().acceptStack(player.item().item, player.item().amount, tile, player) > 0){
                Lines.stroke(3f, Pal.gray);
                Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 3 + Mathf.absin(Time.time(), 5f, 1f));
                Lines.stroke(1f, Pal.place);
                Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 2 + Mathf.absin(Time.time(), 5f, 1f));
                Draw.reset();

            }
        }
    }
}
