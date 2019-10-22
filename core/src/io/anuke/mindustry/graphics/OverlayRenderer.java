package io.anuke.mindustry.graphics;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rectangle rect = new Rectangle();
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
            for(Team enemy : state.teams.enemiesOf(player.getTeam())){
                for(Tile core : state.teams.get(enemy).cores){
                    float dst = Mathf.dst(player.x, player.y, core.drawx(), core.drawy());
                    if(dst < state.rules.enemyCoreBuildRadius * 1.5f){
                        Draw.color(Color.darkGray);
                        Lines.circle(core.drawx(), core.drawy() - 2, state.rules.enemyCoreBuildRadius);
                        Draw.color(Pal.accent, enemy.color, 0.5f + Mathf.absin(Time.time(), 10f, 0.5f));
                        Lines.circle(core.drawx(), core.drawy(), state.rules.enemyCoreBuildRadius);
                    }
                }
            }
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
            Vector2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            Tile tile = world.ltileWorld(vec.x, vec.y);

            if(tile != null && tile.block() != Blocks.air && tile.getTeam() == player.getTeam()){
                tile.block().drawSelect(tile);

                if(Core.input.keyDown(Binding.rotateplaced) && tile.block().rotate){
                    control.input.drawArrow(tile.block(), tile.x, tile.y, tile.rotation(), true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vector2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
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
