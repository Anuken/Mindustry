package mindustry.ui;

import arc.Core;
import arc.scene.style.*;
import arc.util.Strings;
import arc.graphics.Color;
import mindustry.gen.*;
import mindustry.graphics.Pal;

public class Links{
    private static LinkEntry[] links;

    private static void createLinks(){
        links = new LinkEntry[]{
            new LinkEntry("discord", "https://discord.gg/mindustry", Icon.discord, Color.valueOf("7289da")),
            new LinkEntry("changelog", "https://github.com/Anuken/Mindustry/releases", Icon.list, Pal.accent.cpy()),
            new LinkEntry("trello", "https://trello.com/b/aE2tcUwF", Icon.trello, Color.valueOf("026aa7")),
            new LinkEntry("wiki", "https://mindustrygame.github.io/wiki/", Icon.book, Color.valueOf("0f142f")),
            new LinkEntry("suggestions", "https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose/", Icon.add, Color.valueOf("ebebeb")),
            new LinkEntry("reddit", "https://www.reddit.com/r/Mindustry/", Icon.redditAlien, Color.valueOf("ee593b")),
            new LinkEntry("itch.io", "https://anuke.itch.io/mindustry", Icon.itchio, Color.valueOf("fa5c5c")),
            new LinkEntry("google-play", "https://play.google.com/store/apps/details?id=io.anuke.mindustry", Icon.googleplay, Color.valueOf("689f38")),
            new LinkEntry("f-droid", "https://f-droid.org/packages/io.anuke.mindustry/", Icon.android, Color.valueOf("026aa7")),
            new LinkEntry("github", "https://github.com/Anuken/Mindustry/", Icon.github, Color.valueOf("24292e")),
            new LinkEntry("dev-builds", "https://github.com/Anuken/MindustryBuilds", Icon.githubSquare, Color.valueOf("fafbfc"))
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
        public final Drawable icon;

        public LinkEntry(String name, String link, Drawable icon, Color color){
            this.name = name;
            this.color = color;
            this.description = Core.bundle.get("link." + name + ".description", "");
            this.link = link;
            this.icon = icon;
            this.title = Core.bundle.get("link." + name + ".title", Strings.capitalize(name.replace("-", " ")));
        }
    }
}
