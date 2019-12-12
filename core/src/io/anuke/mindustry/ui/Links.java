package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.util.Strings;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.graphics.Pal;

import java.util.ArrayList;
import java.util.List;

import static io.anuke.mindustry.Vars.ui;

public class Links{
    private static LinkEntry[] links;
    static List<String> missingLocales = new ArrayList<String>();

    private static void createLinks(){
        links = new LinkEntry[]{
            new LinkEntry("discord", "https://discord.gg/mindustry", Color.valueOf("7289da")),
            new LinkEntry("changelog", "https://github.com/Anuken/Mindustry/releases", Pal.accent.cpy()),
            new LinkEntry("trello", "https://trello.com/b/aE2tcUwF", Color.valueOf("026aa7")),
            new LinkEntry("wiki", "https://mindustrygame.github.io/wiki/", Color.valueOf("0f142f")),
            new LinkEntry("reddit", "https://www.reddit.com/r/Mindustry/", Color.valueOf("ee593b")),
            new LinkEntry("itch.io", "https://anuke.itch.io/mindustry", Color.valueOf("fa5c5c")),
            new LinkEntry("google-play", "https://play.google.com/store/apps/details?id=io.anuke.mindustry", Color.valueOf("689f38")),
            new LinkEntry("f-droid", "https://f-droid.org/packages/io.anuke.mindustry/", Color.valueOf("026aa7")),
            new LinkEntry("github", "https://github.com/Anuken/Mindustry/", Color.valueOf("24292e")),
            new LinkEntry("dev-builds", "https://github.com/Anuken/MindustryBuilds", Color.valueOf("fafbfc"))
        };
    }

    public static LinkEntry[] getLinks(){
        if(links == null){
            createLinks();
        }
        if(!missingLocales.isEmpty()){
            String key = "missinglocale";
            if(missingLocales.size() == 1){
                key = "missinglocale.single";
            }
            String m = Core.bundle.getOrNull(key);
            if(m==null)m="Locale key(s) missing:\n";
            for(String s : missingLocales){
                m = m+s;
            }
            String finalM = m;
            Time.run(1f, ()->{ui.showErrorMessage(finalM);});
        }

        return links;
    }

    public static class LinkEntry{
        public final String name, title, description, link;
        public final Color color;

        public LinkEntry(String name, String link, Color color){
            this.name = name;
            this.color = color;
            String key = "link." + name + ".description";
            String desc = Core.bundle.getOrNull(key);
            if(desc == null){
                missingLocales.add(key+"\n"); // <- LINE 62
                desc = key;
            }
            this.description = desc;
            this.link = link;

            String title = Core.bundle.getOrNull("link." + name + ".title");
            this.title = title != null ? title : Strings.capitalize(name.replace("-", " "));
        }
    }
}
