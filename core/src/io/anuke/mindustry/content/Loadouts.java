package io.anuke.mindustry.content;

import io.anuke.mindustry.ctype.ContentList;
import io.anuke.mindustry.type.Loadout;

public class Loadouts implements ContentList{
    public static Loadout
    basicShard,
    advancedShard,
    basicFoundation,
    basicNucleus;

    @Override
    public void load(){
        basicShard = new Loadout(
        "  ###  ",
        "  #1#  ",
        "  ###  ",
        "  ^ ^  ",
        " ## ## ",
        " C# C# "
        );

        advancedShard = new Loadout(
        "  ###  ",
        "  #1#  ",
        "#######",
        "C#^ ^C#",
        " ## ## ",
        " C# C# "
        );

        basicFoundation = new Loadout(
        "  ####  ",
        "  ####  ",
        "  #2##  ",
        "  ####  ",
        "  ^^^^  ",
        " ###### ",
        " C#C#C# "
        );

        basicNucleus = new Loadout(
        "  #####  ",
        "  #####  ",
        "  ##3##  ",
        "  #####  ",
        " >#####< ",
        " ^ ^ ^ ^ ",
        "#### ####",
        "C#C# C#C#"
        );
    }
}
