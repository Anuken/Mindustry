package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.util.Strings;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.graphics.Pal;

public class Links{
    private static LinkEntry[] links;

    private static void createLinks(){
        links = new LinkEntry[]{
            new LinkEntry("discord", "https://discord.gg/mindustry", Color.valueOf("7289da")),
            new LinkEntry("changelog", "https://github.com/Anuken/Mindustry/releases", Pal.accent.cpy()),
            new LinkEntry("trello", "https://trello.com/b/aE2tcUwF", Color.valueOf("026aa7")),
            new LinkEntry("wiki", "https://mindustrygame.github.io/wiki/", Color.valueOf("0f142f")),
            new LinkEntry("reddit", "https://www.reddit.com/r/Mindustry/", Color.valueOf("ee593b")),
            new LinkEntry("itch.io", "https://anuke.itch.io/mindustry", Color.valueOf("fa5c5c")),
            new LinkEntry("google-play", "https://play.google.com/store/apps/details?id=io.anuke.mindustry", Color.valueOf("689f38")),
            new LinkEntry("github", "https://github.com/Anuken/Mindustry/", Color.valueOf("24292e")),
            new LinkEntry("dev-builds", "https://github.com/Anuken/MindustryBuilds", Color.valueOf("fafbfc"))
        };
    }

    public static LinkEntry[] getLinks(){
        if(links == null){
            createLinks();
        }

        return links;
    }

    public static class LinkEntry{
        public final String name, title, description, link;
        public final Color color;

        public LinkEntry(String name, String link, Color color){
            this.name = name;
            this.color = color;
            this.description = Core.bundle.getNotNull("link." + name + ".description");
            this.link = link;

            String title = Core.bundle.getOrNull("link." + name + ".title");
            this.title = title != null ? title : Strings.capitalize(name.replace("-", " "));
        }
    }
}
