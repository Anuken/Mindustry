package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import mindustry.ui.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;

import static mindustry.Vars.*;

public class TraceDialog extends BaseDialog{

    public TraceDialog(){
        super("@trace");

        addCloseButton();
        setFillParent(false);
    }

    public void show(Player player, TraceInfo info){
        cont.clear();

        ImageButtonStyle style = new ImageButtonStyle(){{
            down = Styles.flatDown;
            up = Styles.none;
            over = Styles.flatOver;
        }};

        Table table = new Table(Tex.clear);
        table.margin(14);
        table.defaults().pad(1);

        table.defaults().left();
        table.table(stack -> {
            stack.button(Icon.copy, style, 24f, () -> copy(player.name)).padRight(4f);
            stack.add(Core.bundle.format("trace.playername", player.name));
        }).row();
        table.table(stack -> {
            stack.button(Icon.copy, style, 24f, () -> copy(info.ip)).padRight(4f);
            stack.add(Core.bundle.format("trace.ip", info.ip));
        }).row();
        table.table(stack -> {
            stack.button(Icon.copy, style, 24f, () -> copy(info.uuid)).padRight(4f);
            stack.add(Core.bundle.format("trace.id", info.uuid));
        }).row();

        table.add(Core.bundle.format("trace.modclient", info.modded));
        table.row();
        table.add(Core.bundle.format("trace.mobile", info.mobile));
        table.row();
        table.add(Core.bundle.format("trace.times.joined", info.timesJoined));
        table.row();
        table.add(Core.bundle.format("trace.times.kicked", info.timesKicked));
        table.row();

        table.add().pad(5);
        table.row();

        cont.add(table);

        show();
    }

    private void copy(String content){
        Core.app.setClipboardText(content);
        ui.showInfoFade("@copied");
    }
}
