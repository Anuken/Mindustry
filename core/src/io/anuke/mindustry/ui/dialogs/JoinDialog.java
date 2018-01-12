package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Address;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter.DigitsOnlyFilter;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

public class JoinDialog extends FloatingDialog {
    Dialog join;
    Table hosts = new Table();
    float w = 400;

    public JoinDialog(){
        super("$text.joingame");

        addCloseButton();

        join = new FloatingDialog("$text.joingame.title");
        join.content().add("$text.joingame.ip").left();
        Mindustry.platforms.addDialog(join.content().addField(Settings.getString("ip"),text ->{
            Settings.putString("ip", text);
            Settings.save();
        }).size(180f, 54f).get(), 100);

        join.content().row();
        join.content().add("$text.server.port").left();
        Mindustry.platforms.addDialog(join.content()
                .addField(Settings.getString("port"), new DigitsOnlyFilter(), text ->{
                    Settings.putString("port", text);
                    Settings.save();
                })
                .size(180f, 54f).get());
        join.buttons().defaults().size(140f, 60f).pad(4f);
        join.buttons().addButton("$text.cancel", join::hide);
        join.buttons().addButton("$text.ok", () ->
            connect(Settings.getString("ip"), Strings.parseInt(Settings.getString("port")))
        ).disabled(b -> Settings.getString("ip").isEmpty() || Strings.parseInt(Settings.getString("port")) == Integer.MIN_VALUE || Net.active());

        setup();

        shown(this::refresh);
    }

    void refresh(){
        hosts.clear();
        hosts.background("button");
        hosts.label(() -> "[accent]" + Bundles.get("text.hosts.discovering") + new String(new char[(int)(Timers.time() / 10) % 4]).replace("\0", ".")).pad(10f);
        Net.discoverServers(this::addHosts);
    }

    void setup(){
        hosts.background("button");

        ScrollPane pane = new ScrollPane(hosts, "clear");
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        content().clear();
        content().table(t -> {
            t.add("$text.name").padRight(10);
            t.addField(Settings.getString("name"), text -> {
                if(text.isEmpty()) return;
                Vars.player.name = text;
                Settings.put("name", text);
                Settings.save();
            }).grow().pad(8);
        }).width(w).height(70f).pad(4);
        content().row();
        content().add(pane).width(w).pad(0);
        content().row();
        content().addButton("$text.joingame.byip", "clear", join::show).width(w).height(80f);
    }

    void addHosts(Array<Address> array){
        hosts.clear();

        if(array.size == 0){
            hosts.add("$text.hosts.none").pad(10f);
            hosts.add().growX();
            hosts.addImageButton("icon-loading", 16*2f, this::refresh).pad(-10f).padLeft(0).padTop(-6).size(70f, 74f);
        }else {
            for (Address a : array) {
                TextButton button = hosts.addButton("[accent]"+a.name, "clear", () -> {
                    connect(a.address, Vars.port);
                }).width(w).height(80f).pad(4f).get();
                button.left();
                button.row();
                button.add("[lightgray]" + a.address + " / " + Vars.port).pad(4).left();

                hosts.row();
                hosts.background((Drawable) null);
            }
        }
    }

    void connect(String ip, int port){
        Vars.ui.loadfrag.show("$text.connecting");

        Timers.runTask(2f, () -> {
            try{
                Net.connect(ip, port);
                hide();
                join.hide();
            }catch (Exception e) {
                Throwable t = e;
                while(t.getCause() != null){
                    t = t.getCause();
                }
                //TODO localize
                String error = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
                if(error.contains("connection refused")) {
                    error = "connection refused";
                }else if(error.contains("port out of range")){
                    error = "invalid port!";
                }else if(error.contains("invalid argument")) {
                    error = "invalid IP or port!";
                }else if(t.getClass().toString().toLowerCase().contains("sockettimeout")){
                    error = "timed out!\nmake sure the host has port forwarding set up,\nand that the address is correct!";
                }else{
                    error = Strings.parseException(e, false);
                }
                Vars.ui.showError(Bundles.format("text.connectfail", error));
                Vars.ui.loadfrag.hide();

                e.printStackTrace();
            }
        });
    }
}
