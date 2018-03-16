package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.util.Bundles;

public class Links {
    private static final LinkEntry[] links = {
        new LinkEntry("discord", "https://discord.gg/BKADYds", Color.valueOf("7289da")),
        new LinkEntry("trello", "https://trello.com/b/aE2tcUwF", Color.valueOf("026aa7")),
        new LinkEntry("wiki", "http://mindustry.wikia.com/wiki/Mindustry_Wiki", Color.valueOf("0f142f")),
        new LinkEntry("itch.io", "https://anuke.itch.io/mindustry", Color.valueOf("fa5c5c")),
        new LinkEntry("google-play", "https://play.google.com/store/apps/details?id=io.anuke.mindustry", Color.valueOf("689f38")),
        new LinkEntry("github", "https://github.com/Anuken/Mindustry/", Color.valueOf("24292e")),
        new LinkEntry("dev-builds", "https://github.com/Anuken/Mindustry/wiki", Color.valueOf("fafbfc")),
    };

    public static LinkEntry[] getLinks(){
        return links;
    }

    public static class LinkEntry{
        public final String name, description, link;
        public final Color color;

        public LinkEntry(String name, String link, Color color) {
            this.name = name;
            this.color = color;
            this.description = Bundles.getNotNull("text.link." + name +".description");
            this.link = link;
        }
    }
}
