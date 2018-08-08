package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class OverlayRenderer{

    public void drawBottom(){
        for(Player player : players){
            InputHandler input = control.input(player.playerIndex);

            if(!input.isDrawing() || player.isDead()) continue;

            Shaders.outline.color.set(Palette.accent);
            Graphics.beginShaders(Shaders.outline);

            input.drawOutlined();

            Graphics.endShaders();
        }
    }

    public void drawTop(){

        for(Player player : players){
            if(player.isDead()) continue; //dead player don't draw

            InputHandler input = control.input(player.playerIndex);

            //draw config selected block
            if(input.frag.config.isShown()){
                Tile tile = input.frag.config.getSelectedTile();

                synchronized(Tile.tileSetLock){
                    tile.block().drawConfigure(tile);
                }
            }

            input.drawTop();

            Draw.reset();

            //draw selected block bars and info
            if(input.recipe == null && !ui.hasMouse() && !input.frag.config.isShown()){
                Vector2 vec = Graphics.world(input.getMouseX(), input.getMouseY());
                Tile tile = world.tileWorld(vec.x, vec.y);

                if(tile != null && tile.block() != Blocks.air && tile.target().getTeam() == players[0].getTeam()){
                    Tile target = tile.target();

                    if(showBlockDebug && target.entity != null){
                        Draw.color(Color.RED);
                        Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
                        Vector2 v = new Vector2();

                        Draw.tcolor(Color.YELLOW);
                        Draw.tscl(0.25f);
                        Array<Object> arr = target.block().getDebugInfo(target);
                        StringBuilder result = new StringBuilder();
                        for(int i = 0; i < arr.size / 2; i++){
                            result.append(arr.get(i * 2));
                            result.append(": ");
                            result.append(arr.get(i * 2 + 1));
                            result.append("\n");
                        }
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.color(0f, 0f, 0f, 0.5f);
                        Fill.rect(target.drawx(), target.drawy(), v.x, v.y);
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.tscl(fontScale);
                        Draw.reset();
                    }

                    synchronized(Tile.tileSetLock){
                        Block block = target.block();
                        TileEntity entity = target.entity;

                        if(entity != null){
                            int[] values = {0, 0};
                            boolean[] doDraw = {false};

                            Runnable drawbars = () -> {
                                for(BlockBar bar : block.bars.list()){
                                    float offset = Mathf.sign(bar.top) * (block.size / 2f * tilesize + 2f + (bar.top ? values[0] : values[1]));

                                    float value = bar.value.get(target);

                                    if(MathUtils.isEqual(value, -1f)) continue;

                                    if(doDraw[0]){
                                        drawBar(bar.type.color, target.drawx(), target.drawy() + offset, value);
                                    }

                                    if(bar.top)
                                        values[0]++;
                                    else
                                        values[1]++;
                                }
                            };

                            drawbars.run();

                            if(values[0] > 0){
                                drawEncloser(target.drawx(), target.drawy() + block.size * tilesize / 2f + 2f, values[0]);
                            }

                            if(values[1] > 0){
                                drawEncloser(target.drawx(), target.drawy() - block.size * tilesize / 2f - 2f - values[1], values[1]);
                            }

                            doDraw[0] = true;
                            values[0] = 0;
                            values[1] = 1;

                            drawbars.run();
                        }


                        target.block().drawSelect(target);
                    }
                }
            }

            if(input.isDroppingItem()){
                Vector2 v = Graphics.world(input.getMouseX(), input.getMouseY());
                float size = 8;
                Draw.rect(player.inventory.getItem().item.region, v.x, v.y, size, size);
                Draw.color(Palette.accent);
                Lines.circle(v.x, v.y, 6 + Mathf.absin(Timers.time(), 5f, 1f));
                Draw.reset();

                Tile tile = world.tileWorld(v.x, v.y);
                if(tile != null) tile = tile.target();
                if(tile != null && tile.block().acceptStack(player.inventory.getItem().item, player.inventory.getItem().amount, tile, player) > 0){
                    Draw.color(Palette.place);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 1 + Mathf.absin(Timers.time(), 5f, 1f));
                    Draw.color();
                }
            }

        }

        if((!debug || showUI) && Settings.getBool("healthbars")){
            for(TeamData ally : (debug ? state.teams.getTeams() : state.teams.getTeams(true))){
                renderer.drawAndInterpolate(unitGroups[ally.team.ordinal()], u -> !u.isDead(), this::drawStats);
            }

            renderer.drawAndInterpolate(playerGroup, u -> !u.isDead(), this::drawStats);
        }
    }

    void drawStats(Unit unit){
        if(unit.isDead()) return;

        float x = unit.x;
        float y = unit.y;

        if(unit == players[0] && players.length == 1 && snapCamera){
            x = (int) (x + 0.0001f);
            y = (int) (y + 0.0001f);
        }

        drawEncloser(x, y - 9f, 2f);
        drawBar(Palette.healthstats, x, y - 8f, unit.healthf());
        drawBar(Palette.ammo, x, y - 9f, unit.getAmmoFraction());
    }

    void drawBar(Color color, float x, float y, float finion){
        finion = Mathf.clamp(finion);

        if(finion > 0) finion = Mathf.clamp(finion + 0.2f, 0.24f, 1f);

        float len = 3;

        float w = (int) (len * 2 * finion);

        Draw.color(Color.BLACK);
        Fill.crect(x - len, y, len * 2f, 1);
        if(finion > 0){
            Draw.color(color);
            Fill.crect(x - len, y, Math.max(1, w), 1);
        }
        Draw.color();
    }

    void drawEncloser(float x, float y, float height){

        float len = 4;

        Draw.color(Palette.bar);
        Fill.crect(x - len, y - 1, len * 2f, height + 2f);
        Draw.color();
    }
}
