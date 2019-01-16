package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.entities.Player;

import static io.anuke.mindustry.Vars.control;
import static io.anuke.mindustry.Vars.players;

public class LocalPlayerDialog extends FloatingDialog{

    public LocalPlayerDialog(){
        super("$addplayers");

        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild(){
        float size = 140f;

        cont.clear();

        if(players.length > 1){
            cont.addImageButton("icon-cancel", 14 * 2, () -> {
                control.removePlayer();
                rebuild();
            }).size(50f, size).pad(5).bottom();
        }else{
            cont.add().size(50f, size);
        }

        for(Player player : players){
            Table table = new Table();
            Stack stack = new Stack();

            stack.add(new Image("button"));

            Image img = new Image(Core.atlas.find("icon-chat"));
            img.setScaling(Scaling.fill);

            stack.add(img);

            table.add("Player " + (player.playerIndex + 1)).update(label -> label.setColor(player.color));
            table.row();
            table.add(stack).size(size);

            cont.add(table).pad(5);
        }

        if(players.length < 4){
            cont.addImageButton("icon-add", 14 * 2, () -> {
                control.addPlayer(players.length);
                rebuild();
            }).size(50f, size).pad(5).bottom();
        }
    }
}
