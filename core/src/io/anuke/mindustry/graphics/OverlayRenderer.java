package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final Rectangle rect = new Rectangle();
    private float buildFadeTime;

    public void drawBottom(){
        for(Player player : players){
            InputHandler input = control.input(player.playerIndex);

            if(!input.isDrawing() || player.isDead()) continue;

            Shaders.outline.color.set(Pal.accent);
            //TODO draw outlined version
            //Graphics.beginShaders(Shaders.outline);

            input.drawOutlined();

            //Graphics.endShaders();
        }
    }

    public void drawTop(){

        for(Player player : playerGroup.all()){
            if(Core.settings.getBool("indicators") && player != players[0] && player.getTeam() == players[0].getTeam()){
                if(!rect.setSize(Core.camera.width  * 0.9f, Core.camera.height  * 0.9f)
                .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                    Tmp.v1.set(player.x, player.y).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                    Lines.stroke(2f, player.getTeam().color);
                    Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                    Draw.reset();
                }
            }
        }

        for(Player player : players){
            if(player.isDead()) continue; //dead players don't draw

            InputHandler input = control.input(player.playerIndex);

            //draw config selected block
            if(input.frag.config.isShown()){
                Tile tile = input.frag.config.getSelectedTile();
                tile.block().drawConfigure(tile);
            }

            input.drawTop();

            buildFadeTime = Mathf.lerpDelta(buildFadeTime, input.isPlacing() ? 1f : 0f, 0.06f);

            Draw.reset();
            Lines.stroke(buildFadeTime*2f);

            if(buildFadeTime > 0.005f){
                for(Team enemy : state.teams.enemiesOf(player.getTeam())){
                    for(Tile core : state.teams.get(enemy).cores){
                        float dst = Mathf.dst(player.x, player.y, core.drawx(), core.drawy());
                        if(dst < state.rules.enemyCoreBuildRadius * 1.5f){
                            Draw.color(Color.DARK_GRAY);
                            Lines.poly(core.drawx(), core.drawy() - 2, 200, state.rules.enemyCoreBuildRadius);
                            Draw.color(Pal.accent, enemy.color, 0.5f + Mathf.absin(Time.time(), 10f, 0.5f));
                            Lines.poly(core.drawx(), core.drawy(), 200, state.rules.enemyCoreBuildRadius);
                        }
                    }
                }
            }

            Draw.reset();

            //draw selected block bars and info
            if(input.block == null && !Core.scene.hasMouse()){
                Vector2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
                Tile tile = world.tileWorld(vec.x, vec.y);

                if(tile != null && tile.block() != Blocks.air && tile.target().getTeam() == players[0].getTeam()){
                    Tile target = tile.target();
                    target.block().drawSelect(target);
                }
            }

            if(input.isDroppingItem()){
                Vector2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
                float size = 8;
                Draw.rect(player.item().item.region, v.x, v.y, size, size);
                Draw.color(Pal.accent);
                Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time(), 5f, 1f));
                Draw.reset();

                Tile tile = world.tileWorld(v.x, v.y);
                if(tile != null) tile = tile.target();
                if(tile != null && tile.getTeam() == player.getTeam() && tile.block().acceptStack(player.item().item, player.item().amount, tile, player) > 0){
                    Draw.color(Pal.place);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 1 + Mathf.absin(Time.time(), 5f, 1f));
                    Draw.color();
                }
            }
        }
    }
}
