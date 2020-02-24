package mindustry.plugin.coreprotect;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.entities.effect.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.input.Placement.*;
import mindustry.plugin.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class CoreProtect extends Plugin implements ApplicationListener{

    private ObjectMap<Player, Stick> sticks = new ObjectMap<>();
    private Array<Tile> cuboid = new Array<>();

    private int timers = 0;
    private int spark = timers++;
    private Interval timer = new Interval(timers);

    private Color red = Pal.remove, blue = Pal.copy;

    public void onTileTapped(Player player, Tile tile){
        Stick stick = sticks.getOr(player, Stick::new);

        switch(stick.xyi++ % 3){
            case 0:
                stick.xy1 = tile.pos();
                message(player, Strings.format("Selected [accent]{0}, {1}[] as point 1 {2}", tile.x, tile.y, Iconc.pipette));
                break;
            case 1:
                stick.xy2 = tile.pos();
                message(player, Strings.format("Selected [accent]{0}, {1}[] as point 2 {2}", tile.x, tile.y, Iconc.pipette));
                break;
            case 2:
                stick.xy1 = Pos.invalid;
                stick.xy2 = Pos.invalid;
                message(player, Strings.format("Selection [accent]cleared[] {0}", Iconc.trash));
                break;
        }

        if(tiles(stick).isEmpty()) return;

        message(player, Strings.format("Selected [accent]{0}[] tiles in total {1}", cuboid.size, Iconc.play));
    }

    @Override
    public void init(){
        Events.on(Trigger.update, () -> {
            if(!Vars.state.is(State.playing)) return;

            if(timer.get(spark, 10)){
                sticks.each((player, stick) -> {
                    if(stick.xy1 != Pos.invalid) spark(player, stick.xy1, blue);
                    if(stick.xy2 != Pos.invalid) spark(player, stick.xy2, red);

//                    tiles(stick).each(Fire::create);
                });
            }
        });
    }

    private void spark(Player player, int pos, Color color){
        Call.createLighting(player.con, 0, player.getTeam(), color, 0, Pos.x(pos) * tilesize, Pos.y(pos) * tilesize, 0, 2);
    }

    private Array<Tile> tiles(Stick stick){
        cuboid.clear();
        if(stick.xy1 == Pos.invalid || stick.xy2 == Pos.invalid) return cuboid;

        NormalizeResult result = Placement.normalizeArea(Pos.x(stick.xy1), Pos.y(stick.xy1), Pos.x(stick.xy2), Pos.y(stick.xy2), 0, false, 1024);

        for(int x = result.x; x <= result.x2; x++){
            for(int y = result.y; y <= result.y2; y++){
                cuboid.add(world.ltile(x, y));
            }
        }

        return cuboid;
    }

    private void message(Player player, String message){
        player.sendMessage(String.format("[royal]%s %s", Iconc.eye, message));
    }

    class Stick{
        int xyi = 0;
        int xy1 = Pos.invalid;
        int xy2 = Pos.invalid;
    }
}
