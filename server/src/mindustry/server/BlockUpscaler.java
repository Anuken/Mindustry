package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class BlockUpscaler implements ApplicationListener{
    private Interval timer = new Interval();
    private Array<Tile> tmp = new Array<>();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;

        if(!timer.get(0, 20)) return;

        for(Team team : Team.base()){
            indexer.getAllied(team, BlockFlag.scalable).each(tile -> {

                if(tile == null) return;
                if(!tile.block().flags.contains(BlockFlag.scalable)) return;

                tile.getLinkedTilesAs(tile.block().upscale.get(), tmp);
                if(tmp.select(t -> t.block() != tile.block()).size > 0) return;

                Call.onConstructFinish(tile, tile.block().upscale.get(), -1, tile.rotation(), tile.getTeam(), true);
            });
        }
    }

    @Override
    public void init(){
        try(Scanner scan = new Scanner(Core.files.internal("icons/icons.properties").read(512))){
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                String[] split = line.split("=");
                String[] nametex = split[1].split("\\|");
                String character = split[0], texture = nametex[1];
                int ch = Integer.parseInt(character);

                Fonts.unicodeIcons.put(nametex[0], ch);
            }
        }

        netServer.admins.addChatFilter((player, text) -> {
            for(String word : text.split("\\s+")){
                if(Fonts.getUnicode(word.toLowerCase()) != 0){
                    text = text.replaceAll("(?i)" + word, (char) Fonts.getUnicode(word.toLowerCase()) + "");
                }
            }

            return text;
        });
    }
}
