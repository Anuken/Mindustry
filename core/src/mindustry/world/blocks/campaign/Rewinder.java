package mindustry.world.blocks.campaign;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.Rewind;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

public class Rewinder extends Block {
    public Rewinder(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = false;
        configurable = false;
    }
    //TODO make rewind, use Rewind.main(null? or maybe 145.1, idrk)
}