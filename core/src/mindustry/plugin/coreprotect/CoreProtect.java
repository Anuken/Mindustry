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
import mindustry.net.Administration.*;
import mindustry.plugin.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;

public class CoreProtect extends Plugin implements ApplicationListener{

    private ObjectMap<Player, Stick> sticks = new ObjectMap<>();
    private Array<Tile> cuboid = new Array<>();

    private int timers = 0;
    private int spark = timers++;
    private Interval timer = new Interval(timers);

    private Color red = Pal.remove, blue = Pal.copy;

    private IntMap<Array<Edit>> edits = new IntMap<>();

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.register("/", "Coreprotect wand.", (args, player) -> {
            Stick stick = sticks.getOr((Player)player, Stick::new);

            if(stick.enabled = !stick.enabled){
                message((Player)player, Strings.format("Selection [accent]enabled[] {0}", Iconc.lockOpen));
            }else{
                message((Player)player, Strings.format("Selection [accent]disabled[] {0}", Iconc.lock));
                stick.xyi = 0;
                stick.xy1 = Pos.invalid;
                stick.xy2 = Pos.invalid;
            }
        });

        handler.register("lookup", "Coreprotect lookup.", (args, player) -> {
            Stick stick = sticks.getOr((Player)player, Stick::new);

            if(!stick.enabled){
                message((Player)player, Strings.format("Selection [accent]required[] {0}", Iconc.block));
                return;
            }

            tiles(stick).each(t -> {
                if(!edits.containsKey(t.pos())) return;
                edits.get(t.pos()).each(edit -> {
                    message((Player)player, "- " + edit);
                });
            });
        });
    }

    public void onTileTapped(Player player, Tile tile){
        Stick stick = sticks.getOr(player, Stick::new);

        if(!stick.enabled) return;

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

    public void allowedAction(PlayerAction pac){
        if(!edits.containsKey(pac.tile.pos())) edits.put(pac.tile.pos(), new Array<>());

        if(pac.type == ActionType.tapTile) return; // ignore taps

        Edit edit = new Edit();
        edit.player = Strings.stripColors(pac.player.name);
        edit.action = pac.type.name();

        edit.visual = null;
        if(pac.block != null) edit.visual = "" + (char)Fonts.getUnicode(pac.block.name);

        if(pac.type == ActionType.configure){
            if(pac.tile.block instanceof Sorter) edit.visual = "" + (char)Fonts.getUnicode(content.item(pac.config).name);
        }

        if(edit.visual == null) edit.visual = "" + (char)Fonts.getUnicode("dark-metal");

        edits.get(pac.tile.pos()).add(edit);
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

        boolean enabled = false;
    }

    class Edit{
        String player, visual, action;

        @Override
        public String toString(){
            return Strings.format("[accent]{0} [white]{1} [accent]{2}", action, visual, player);
        }
    }
}
