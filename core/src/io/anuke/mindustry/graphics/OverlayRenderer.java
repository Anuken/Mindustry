package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.CapStyle;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.Vars.playerGroup;

public class OverlayRenderer {

    public void draw(){

        for(Player player : players) {

            InputHandler input = control.input(player.playerIndex);

            //draw config selected block
            if(input.frag.config.isShown()){
                Tile tile = input.frag.config.getSelectedTile();
                tile.block().drawConfigure(tile);
            }

            int tilex = input.getBlockX();
            int tiley = input.getBlockY();

            //draw placement box
            if ((input.recipe != null && state.inventory.hasItems(input.recipe.requirements) && (!ui.hasMouse() || mobile)
                    && input.drawPlace())) {

                input.placeMode.draw(input, input.getBlockX(),
                        input.getBlockY(), input.getBlockEndX(), input.getBlockEndY());

                if (input.breakMode == PlaceMode.holdDelete)
                    input.breakMode.draw(input, tilex, tiley, 0, 0);

            } else if (input.breakMode.delete && input.drawPlace()
                    && (input.recipe == null || !state.inventory.hasItems(input.recipe.requirements))
                    && (input.placeMode.delete || input.breakMode.both || !mobile)) {

                if (input.breakMode == PlaceMode.holdDelete)
                    input.breakMode.draw(input, tilex, tiley, 0, 0);
                else
                    input.breakMode.draw(input, input.getBlockX(),
                            input.getBlockY(), input.getBlockEndX(), input.getBlockEndY());
            }

            if (input.frag.tool.confirming) {
                ToolFragment t = input.frag.tool;
                PlaceMode.areaDelete.draw(input, t.px, t.py, t.px2, t.py2);
            }

            Draw.reset();

            //draw selected block bars and info
            if (input.recipe == null && !ui.hasMouse() && !input.frag.config.isShown()) {
                Vector2 vec = Graphics.world(input.getMouseX(), input.getMouseY());
                Tile tile = world.tileWorld(vec.x, vec.y);

                if (tile != null && tile.block() != Blocks.air) {
                    Tile target = tile;
                    if (tile.isLinked())
                        target = tile.getLinked();

                    if (showBlockDebug && target.entity != null) {
                        Draw.color(Color.RED);
                        Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
                        Vector2 v = new Vector2();

                        Draw.tcolor(Color.YELLOW);
                        Draw.tscl(0.25f);
                        Array<Object> arr = target.block().getDebugInfo(target);
                        StringBuilder result = new StringBuilder();
                        for (int i = 0; i < arr.size / 2; i++) {
                            result.append(arr.get(i * 2));
                            result.append(": ");
                            result.append(arr.get(i * 2 + 1));
                            result.append("\n");
                        }
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.color(0f, 0f, 0f, 0.5f);
                        Fill.rect(target.drawx(), target.drawy(), v.x, v.y);
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.tscl(fontscale);
                        Draw.reset();
                    }

                    if (Inputs.keyDown("block_info") && target.block().isAccessible()) {
                        Draw.color(Colors.get("accent"));
                        Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
                        Draw.color();
                    }

                    if (target.entity != null) {
                        int bot = 0, top = 0;
                        for (BlockBar bar : target.block().bars.list()) {
                            float offset = Mathf.sign(bar.top) * (target.block().size / 2f * tilesize + 3f + 4f * ((bar.top ? top : bot))) +
                                    (bar.top ? -1f : 0f);

                            float value = bar.value.get(target);

                            if (MathUtils.isEqual(value, -1f)) continue;

                            drawBar(bar.type.color, target.drawx(), target.drawy() + offset, value);

                            if (bar.top)
                                top++;
                            else
                                bot++;
                        }
                    }

                    target.block().drawSelect(target);
                }
            }

            if (input.isDroppingItem()) {
                Vector2 v = Graphics.world(input.getMouseX(), input.getMouseY());
                float size = 8;
                Draw.rect(player.inventory.getItem().item.region, v.x, v.y, size, size);
                Draw.color("accent");
                Lines.circle(v.x, v.y, 6 + Mathf.absin(Timers.time(), 5f, 1f));
                Draw.reset();

                Tile tile = world.tileWorld(v.x, v.y);
                if (tile != null) tile = tile.target();
                if (tile != null && tile.block().acceptStack(player.inventory.getItem().item, player.inventory.getItem().amount, tile, player) > 0) {
                    Draw.color("place");
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 1 + Mathf.absin(Timers.time(), 5f, 1f));
                    Draw.color();
                }
            }

        }

        if((!debug || showUI) && Settings.getBool("healthbars")){
            for(TeamData ally : (debug ? state.teams.getTeams() : state.teams.getTeams(true))){
                for(Unit e : unitGroups[ally.team.ordinal()].all()){
                    drawStats(e);
                }
            }

            for(Unit e : playerGroup.all()){
                drawStats(e);
            }
        }
    }

    void drawStats(Unit unit){
        if(unit.isDead()) return;

        float x = unit.getDrawPosition().x;
        float y = unit.getDrawPosition().y;

        if(unit == players[0] && players.length == 1 && snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate")) {
            x = (int)x;
            y = (int)y;
        }

        drawEncloser(x, y - 8f, 2f);
        drawBar(Color.SCARLET, x, y - 8f, unit.health / unit.maxhealth);
        drawBar(Color.valueOf("32cf6d"), x, y - 9f, unit.inventory.totalAmmo() / (float) unit.inventory.ammoCapacity());
    }

    public void drawBar(Color color, float x, float y, float finion){
        finion = Mathf.clamp(finion);

        if(finion > 0) finion = Mathf.clamp(finion + 0.2f, 0.24f, 1f);

        float len = 3;

        float w = (int) (len * 2 * finion) + 0.5f;

        x -= 0.5f;
        y += 0.5f;

        Draw.color(Color.BLACK);
        Lines.line(x - len + 1, y, x + len + 0.5f, y);
        Draw.color(color);
        if(w >= 1)
            Lines.line(x - len + 1, y, x - len + w, y);
        Draw.reset();
    }

    public void drawEncloser(float x, float y, float height){
        x -= 0.5f;
        y += 0.5f - (height-1f)/2f;

        float len = 3;

        Lines.stroke(2f + height);
        Draw.color(Color.SLATE);
        Lines.line(x - len - 0.5f, y, x + len + 1.5f, y, CapStyle.none);

        Draw.reset();
    }
}
