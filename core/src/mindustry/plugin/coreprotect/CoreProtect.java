package mindustry.plugin.coreprotect;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
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
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class CoreProtect extends Plugin implements ApplicationListener{

    private ObjectMap<Player, Stick> sticks = new ObjectMap<>();
    private Array<Tile> cuboid = new Array<>();
    private Array<Edit> lookup = new Array<>();

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
                stick.disable();
            }
        });

        handler.register("lookup", "[size]", "Coreprotect lookup.", (args, player) -> {
            Stick stick = sticks.getOr((Player)player, Stick::new);

            if(!stick.enabled){
                message((Player)player, Strings.format("Selection [accent]required[] {0}", Iconc.block));
                return;
            }

            lookup((Player)player, args.length > 0 ? Strings.parseInt(args[0]) : 5);
        });
    }

    private void lookup(Player player, int max){
        Stick stick = sticks.getOr(player, Stick::new);

        lookup.clear();
        tiles(stick).each(t -> {
            if(!edits.containsKey(t.pos())) return;
            edits.get(t.pos()).each(edit -> lookup.add(edit));
        });
        lookup.sort(edit -> -edit.frame);

        message(player, Strings.format("-- showing [accent]{0}[] of [accent]{1}[] results", Mathf.clamp(max, 0, lookup.size), lookup.size));

        int i = 0;
        for(Edit edit : lookup){
            if(i++ > max) break;
            message(player, "- " + edit);
        }
    }

    public void onTileTapped(Player player, Tile tile){
        Stick stick = sticks.getOr(player, Stick::new);

        if(!stick.enabled) return;

        switch(stick.xyi++ % 2){
            case 0:
                stick.xy1 = tile.pos();
                break;
            case 1:
                stick.xy2 = tile.pos();
                break;
        }

        if(stick.xy1 == stick.xy2){
            message(player, Strings.format("Selection [accent]disabled[] {0}", Iconc.lock));
            stick.disable();
            return;
        }

        if(tiles(stick).isEmpty()) return;

        lookup(player, 5);
    }

    @Override
    public void init(){
        Events.on(Trigger.update, () -> {
            if(!Vars.state.is(State.playing)) return;

            if(timer.get(spark, 10)){
                sticks.each((player, stick) -> {
                    if(stick.xy1 != Pos.invalid) spark(player, stick.xy1, blue);
                    if(stick.xy2 != Pos.invalid) spark(player, stick.xy2, red);
                });
            }
        });
    }

    public void allowedAction(PlayerAction pac){
        if(!edits.containsKey(pac.tile.pos())) edits.put(pac.tile.pos(), new Array<>());

        if(pac.type == ActionType.tapTile) return; // ignore taps
        if(pac.player == null) return; // sometimes configure has no player

        Edit edit = new Edit();
        edit.player = Strings.stripColors(pac.player.name);
        edit.action = pac.type.human;
        edit.x = pac.tile.x;
        edit.y = pac.tile.y;

        // block
        if(pac.type == ActionType.placeBlock || pac.type == ActionType.breakBlock){
            if(pac.tile.block instanceof BlockPart) return; // skip block parts
            edit.icon = (char)Fonts.getUnicode(pac.block.name);
        }

        // item
        if(pac.type == ActionType.configure && (pac.tile.block instanceof Sorter || pac.tile.block instanceof Unloader || pac.tile.block instanceof ItemSource)){
            edit.icon = (char)Fonts.getUnicode(content.item(pac.config).name);
        }

        // liquid
        if(pac.type == ActionType.configure && (pac.tile.block instanceof LiquidSource)){
            edit.icon = (char)Fonts.getUnicode(content.liquid(pac.config).name);
        }

        // banking
        if(pac.type == ActionType.withdrawItem || pac.type == ActionType.depositItem){
            edit.icon = (char)Fonts.getUnicode(content.item(pac.config).name);
        }

        // merry go round
        if(pac.type == ActionType.rotate){
            switch(pac.rotation){
                case 0: edit.icon = Iconc.right; break;
                case 1: edit.icon = Iconc.up;    break;
                case 2: edit.icon = Iconc.left;  break;
                case 3: edit.icon = Iconc.down;  break;
            }
        }

        // prevent chain of (de)construction messages
        if(edits.get(pac.tile.pos()).size > 0){
            Edit last = edits.get(pac.tile.pos()).get(edits.get(pac.tile.pos()).size -1);
            if(edit.similar(last)) return;
        }

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
                cuboid.add(world.tile(x, y));
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

        void disable(){
            xyi = 0;
            xy1 = Pos.invalid;
            xy2 = Pos.invalid;
            enabled = false;
        }
    }

    class Edit{
        int x, y;
        String player, action;
        char icon = (char)Fonts.getUnicode("dark-metal");
        long frame = Core.graphics.getFrameId();

        public boolean similar(Edit edit){
            return player.equals(edit.player) && action.equals(edit.action);
        }

        @Override
        public String toString(){
            return Strings.format("[white]{0}[] [accent]{1}[] [white]{2} {3} seconds ago[] {4}, {5}", player, action, icon, (Core.graphics.getFrameId() - frame) / 60, x, y);
        }
    }
}
