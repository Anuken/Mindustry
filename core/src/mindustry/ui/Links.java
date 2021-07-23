package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;

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
        new LinkEntry("dev-builds", "https://github.com/Anuken/MindustryBuilds", Icon.githubSquare, Color.valueOf("fafbfc")),
        new LinkEntry("bug", report(), Icon.wrench, Color.valueOf("cbd97f"))
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

    private static String report(){
        return "https://github.com/Anuken/Mindustry/issues/new?assignees=&labels=bug&body=" +
        Strings.encode(Strings.format(
        """
        **Platform**: `@`
                    
        **Build**: `@`
                    
        **Issue**: *Explain your issue in detail.*
                    
        **Steps to reproduce**: *How you happened across the issue, and what exactly you did to make the bug happen.*
                    
        **Link(s) to mod(s) used**: `@`
                    
        **Save file**: *The (zipped) save file you were playing on when the bug happened. THIS IS REQUIRED FOR ANY ISSUE HAPPENING IN-GAME, REGARDLESS OF WHETHER YOU THINK IT HAPPENS EVERYWHERE. DO NOT DELETE OR OMIT THIS LINE UNLESS YOU ARE SURE THAT THE ISSUE DOES NOT HAPPEN IN-GAME.*
                    
        **Crash report**: *The contents of relevant crash report files. REQUIRED if you are reporting a crash.*
                    
        ---
                    
        *Place an X (no spaces) between the brackets to confirm that you have read the line below.*
        - [ ] **I have updated to the latest release (https://github.com/Anuken/Mindustry/releases) to make sure my issue has not been fixed.**
        - [ ] **I have searched the closed and open issues to make sure that this problem has not already been reported.**
        """,
        OS.isAndroid ? "Android " + Core.app.getVersion() : (OS.osName + " x" + OS.osArchBits),
        Version.combined(),
        Vars.mods.list().any() ? Vars.mods.list().select(LoadedMod::enabled).map(l -> l.meta.author + "/" + l.name + ":" + l.meta.version) : "none"));
    }
}