package mindustry.server;

import arc.*;
import mindustry.ui.*;

import java.util.*;

import static mindustry.Vars.netServer;

public class EmojiFilter implements ApplicationListener{

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
