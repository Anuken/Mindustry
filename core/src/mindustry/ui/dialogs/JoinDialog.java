package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Timer.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.versions.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class JoinDialog extends BaseDialog{
    Seq<Server> servers = new Seq<>();
    Dialog add;
    Server renaming;
    Table local = new Table();
    Table remote = new Table();
    Table global = new Table();
    Table hosts = new Table();
    int totalHosts;
    int refreshes;
    boolean showHidden;
    TextButtonStyle style;

    String lastIp;
    int lastPort, lastColumns = -1;
    Task ping;

    String serverSearch = "";

    public JoinDialog(){
        super("@joingame");

        style = new TextButtonStyle(){{
            over = Styles.flatOver;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            down = Styles.flatOver;
            up = Styles.black5;
        }};

        loadServers();

        if(!steam) buttons.add().width(60f);
        buttons.add().growX().width(-1);

        addCloseButton();

        buttons.add().growX().width(-1);
        if(!steam) buttons.button("?", () -> ui.showInfo("@join.info")).size(60f, 64f);

        add = new BaseDialog("@joingame.title");
        add.cont.add("@joingame.ip").padRight(5f).left();

        TextField field = add.cont.field(Core.settings.getString("ip"), text -> {
            Core.settings.put("ip", text);
        }).size(320f, 54f).maxTextLength(100).get();

        add.cont.row();
        add.buttons.defaults().size(140f, 60f).pad(4f);
        add.buttons.button("@cancel", add::hide);
        add.buttons.button("@ok", () -> {
            if(renaming == null){
                Server server = new Server();
                server.setIP(Core.settings.getString("ip"));
                servers.add(server);
            }else{
                renaming.setIP(Core.settings.getString("ip"));
            }
            saveServers();
            setupRemote();
            refreshRemote();
            add.hide();
        }).disabled(b -> Core.settings.getString("ip").isEmpty() || net.active());

        add.shown(() -> {
            add.title.setText(renaming != null ? "@server.edit" : "@server.add");
            if(renaming != null){
                field.setText(renaming.displayIP());
            }
        });

        keyDown(KeyCode.f5, this::refreshAll);

        shown(() -> {
            setup();
            refreshAll();

            if(!steam){
                Core.app.post(() -> Core.settings.getBoolOnce("joininfo", () -> ui.showInfo("@join.info")));
            }
        });

        onResize(() -> {


            //only refresh on resize when the minimum dimension is smaller than the maximum preferred width
            //this means that refreshes on resize will only happen for small phones that need the list to fit in portrait mode
            //also resize if number of cols changes
            if(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / Scl.scl() * 0.9f < 500f || lastColumns != columns()){
                setup();
                refreshAll();
            }

            lastColumns = columns();
        });
    }

    void refreshAll(){
        refreshes ++;

        refreshLocal();
        refreshRemote();
        refreshCommunity();
    }

    void setupRemote(){
        remote.clear();

        for(Server server : servers){
            //why are java lambdas this bad
            Button[] buttons = {null};

            Button button = buttons[0] = remote.button(b -> {}, style, () -> {
                if(!buttons[0].childrenPressed()){
                    if(server.lastHost != null){
                        Events.fire(new ClientPreConnectEvent(server.lastHost));
                        safeConnect(server.lastHost.address, server.lastHost.port, server.lastHost.version);
                    }else{
                        connect(server.ip, server.port);
                    }
                }
            }).width(targetWidth()).growY().top().left().pad(4f).get();

            if(remote.getChildren().size % columns() == 0){
                remote.row();
            }

            Table inner = new Table(Tex.whiteui);
            inner.setColor(Pal.gray);

            button.clearChildren();
            button.add(inner).growX();

            inner.add("[accent]" + server.displayIP()).left().padLeft(10f).wrap().style(Styles.outlineLabel).growX();

            inner.button(Icon.upOpen, Styles.emptyi, () -> {
                moveRemote(server, -1);

            }).margin(3f).padTop(6f).top().right();

            inner.button(Icon.downOpen, Styles.emptyi, () -> {
                moveRemote(server, +1);

            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.refresh, Styles.emptyi, () -> {
                refreshServer(server);
            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.pencil, Styles.emptyi, () -> {
                renaming = server;
                add.show();
            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.trash, Styles.emptyi, () -> {
                ui.showConfirm("@confirm", "@server.delete", () -> {
                    servers.remove(server, true);
                    saveServers();
                    setupRemote();
                    refreshRemote();
                });
            }).margin(3f).pad(2).pad(6).top().right();

            button.row();

            server.content = button.table(t -> {}).grow().get();
        }
    }

    void moveRemote(Server server, int sign){
        int index = servers.indexOf(server);

        if(index + sign < 0) return;
        if(index + sign > servers.size - 1) return;

        servers.remove(index);
        servers.insert(index + sign, server);

        saveServers();
        setupRemote();
        for(Server other : servers){
            if(other.lastHost != null){
                setupServer(other, other.lastHost);
            }else{
                refreshServer(other);
            }
        }
    }

    void refreshRemote(){
        for(Server server : servers){
            refreshServer(server);
        }
    }

    void refreshServer(Server server){
        server.content.clear();

        server.content.background(Tex.whitePane).setColor(Pal.gray);

        server.content.label(() -> Core.bundle.get("server.refreshing") + Strings.animated(Time.time, 4, 11, ".")).padBottom(4);

        net.pingHost(server.ip, server.port, host -> setupServer(server, host), e -> {
            server.content.clear();

            server.content.background(Tex.whitePane).setColor(Pal.gray);
            server.content.add("@host.invalid");
        });
    }

    void setupServer(Server server, Host host){
        server.lastHost = host;
        server.content.clear();
        buildServer(host, server.content);
    }

    void buildServer(Host host, Table content){
        content.top().left();
        String versionString;

        if(host.version == -1){
            versionString = Core.bundle.format("server.version", Core.bundle.get("server.custombuild"), "");
        }else if(host.version == 0){
            versionString = Core.bundle.get("server.outdated");
        }else if(host.version < Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version > Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated.client") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version == Version.build && Version.type.equals(host.versionType)){
            //not important
            versionString = "";
        }else{
            versionString = Core.bundle.format("server.version", host.version, host.versionType);
        }

        float twidth = targetWidth() - 40f;

        content.background(null);

        Color color = Pal.gray;

        content.table(Tex.whiteui, t -> {
            t.left();
            t.setColor(color);

            t.add(host.name + "   " + versionString).style(Styles.outlineLabel).padLeft(10f).width(twidth).left().ellipsis(true);
        }).growX().height(36f).row();

        content.table(Tex.whitePane, t -> {
            t.top().left();
            t.setColor(color);
            t.left();

            if(!host.description.isEmpty()){
                //limit newlines.
                int count = 0;
                StringBuilder result = new StringBuilder(host.description.length());
                for(int i = 0; i < host.description.length(); i++){
                    char c = host.description.charAt(i);
                    if(c == '\n'){
                        count ++;
                        if(count < 3) result.append(c);
                    }else{
                        result.append(c);
                    }
                }
                t.add("[gray]" + result).width(twidth).left().wrap();
                t.row();
            }

            t.add("[lightgray]" + (Core.bundle.format("players" + (host.players == 1 && host.playerLimit <= 0 ? ".single" : ""),
                (host.players == 0 ? "[lightgray]" : "[accent]") + host.players + (host.playerLimit > 0 ? "[lightgray]/[accent]" + host.playerLimit : "")+ "[lightgray]"))).left().row();

            t.add("[lightgray]" + Core.bundle.format("save.map", host.mapname) + "[lightgray] / " + (host.modeName == null ? host.mode.toString() : host.modeName)).width(twidth).left().ellipsis(true).row();

            if(host.ping > 0){
                t.add(Iconc.chartBar + " " + host.ping + "ms").style(Styles.outlineLabel).color(Pal.gray).left();
            }
        }).growY().growX().left().bottom();
    }

    void setup(){
        local.clear();
        remote.clear();
        global.clear();
        float w = targetWidth();

        hosts.clear();

        section(steam ? "@servers.local.steam" : "@servers.local", local, false);
        section("@servers.remote", remote, false);
        section("@servers.global", global, true);

        ScrollPane pane = new ScrollPane(hosts);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        setupRemote();

        cont.clear();
        cont.table(t -> {
            t.add("@name").padRight(10);
            t.field(Core.settings.getString("name"), text -> {
                player.name(text);
                Core.settings.put("name", text);
            }).grow().pad(8).maxTextLength(maxNameLength);

            ImageButton button = t.button(Tex.whiteui, Styles.squarei, 40, () -> {
                new PaletteDialog().show(color -> {
                    player.color().set(color);
                    Core.settings.put("color-0", color.rgba8888());
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color());
        }).width(w).height(70f).pad(4);
        cont.row();
        cont.add(pane).width((w + 5) * columns() + 33).pad(0);
        cont.row();
        cont.buttonCenter("@server.add", Icon.add, () -> {
            renaming = null;
            add.show();
        }).marginLeft(10).width(w).height(80f).update(button -> {
            float pw = w;
            float pad = 0f;
            if(pane.getChildren().first().getPrefHeight() > pane.getHeight()){
                pw = w + 30;
                pad = 6;
            }

            var cell = ((Table)pane.parent).getCell(button);

            if(!Mathf.equal(cell.minWidth(), pw)){
                cell.width(pw);
                cell.padLeft(pad);
                pane.parent.invalidateHierarchy();
            }
        });
    }

    void section(String label, Table servers, boolean eye){
        Collapser coll = new Collapser(servers, Core.settings.getBool("collapsed-" + label, false));
        coll.setDuration(0.1f);

        hosts.table(name -> {
            name.add(label).pad(10).growX().left().color(Pal.accent);

            if(eye){
                name.button(Icon.eyeSmall, Styles.emptyi, () -> {
                    showHidden = !showHidden;
                    refreshCommunity();
                }).update(i -> i.getStyle().imageUp = (showHidden ? Icon.eyeSmall : Icon.eyeOffSmall))
                    .size(40f).right().padRight(3).tooltip("@servers.showhidden");
            }

            name.button(Icon.downOpen, Styles.emptyi, () -> {
                coll.toggle(false);
                Core.settings.put("collapsed-" + label, coll.isCollapsed());
            }).update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen)).size(40f).right().padRight(10f);
        }).growX();
        hosts.row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent);
        hosts.row();
        hosts.add(coll).width((targetWidth() + 5f) * columns());
        hosts.row();
    }

    void refreshLocal(){
        totalHosts = 0;

        local.clear();
        local.background(null);
        local.table(Tex.button, t -> t.label(() -> "[accent]" + Core.bundle.get("hosts.discovering.any") + Strings.animated(Time.time, 4, 10f, ".")).pad(10f)).growX();
        net.discoverServers(this::addLocalHost, this::finishLocalHosts);
    }

    void refreshCommunity(){
        int cur = refreshes;

        global.clear();
        global.background(null);

        global.table(t -> {
            t.add("@search").padRight(10);
            t.field(serverSearch, text ->
                serverSearch = text.trim().replaceAll(" +", " ").toLowerCase()
            ).grow().pad(8).get().keyDown(KeyCode.enter, this::refreshCommunity);
            t.button(Icon.zoom, Styles.emptyi, this::refreshCommunity).size(54f);
        }).width((targetWidth() + 5f) * columns()).height(70f).pad(4).row();

        for(int i = 0; i < defaultServers.size; i ++){
            ServerGroup group = defaultServers.get((i + defaultServers.size/2) % defaultServers.size);
            boolean hidden = group.hidden();
            if(hidden && !showHidden){
                continue;
            }

            Table[] groupTable = {null, null};

            //table containing all groups
            for(String address : group.addresses){
                String resaddress = address.contains(":") ? address.split(":")[0] : address;
                int resport = address.contains(":") ? Strings.parseInt(address.split(":")[1]) : port;
                net.pingHost(resaddress, resport, res -> {
                    if(refreshes != cur) return;

                    if(!serverSearch.isEmpty() && !(group.name.toLowerCase().contains(serverSearch)
                        || res.name.toLowerCase().contains(serverSearch)
                        || res.description.toLowerCase().contains(serverSearch)
                        || res.mapname.toLowerCase().contains(serverSearch)
                        || (res.modeName != null && res.modeName.toLowerCase().contains(serverSearch)))) return;

                    //add header
                    if(groupTable[0] == null){
                        global.table(t -> groupTable[0] = t).fillX().left().row();

                        groupTable[0].table(head -> {
                            if(!group.name.isEmpty()){
                                head.add(group.name).color(Color.lightGray).padRight(4);
                            }
                            head.image().height(3f).growX().color(Color.lightGray);

                            //button for showing/hiding servers
                            ImageButton[] image = {null};
                            image[0] = head.button(hidden ? Icon.eyeOffSmall : Icon.eyeSmall, Styles.grayi, () -> {
                               group.setHidden(!group.hidden());
                               image[0].getStyle().imageUp = group.hidden() ? Icon.eyeOffSmall : Icon.eyeSmall;
                               if(group.hidden() && !showHidden){
                                   groupTable[0].remove();
                               }
                            }).size(40f).get();
                            image[0].addListener(new Tooltip(t -> t.background(Styles.black6).margin(4).label(() -> !group.hidden() ? "@server.shown" : "@server.hidden")));
                        }).width(targetWidth() * columns()).padBottom(-2).row();

                        groupTable[1] = groupTable[0].row().table().top().left().grow().get();
                    }

                    addCommunityHost(res, groupTable[1]);

                    groupTable[0].margin(5f);
                    groupTable[0].pack();
                }, e -> {});
            }
        }
    }

    int columns(){
        return Mathf.clamp((int)((Core.graphics.getWidth() / Scl.scl() * 0.85f) / targetWidth()), 1, 3);
    }

    void addCommunityHost(Host host, Table container){
        global.background(null);
        float w = targetWidth();

        container.left().top();

        container.button(b -> buildServer(host, b), style, () -> {
            Events.fire(new ClientPreConnectEvent(host));
            if(!Core.settings.getBool("server-disclaimer", false)){
                ui.showCustomConfirm("@warning", "@servers.disclaimer", "@ok", "@back", () -> {
                    Core.settings.put("server-disclaimer", true);
                    safeConnect(host.address, host.port, host.version);
                }, () -> {
                    Core.settings.put("server-disclaimer", false);
                });
            }else{
                safeConnect(host.address, host.port, host.version);
            }
        }).width(w).padBottom(7).padRight(4f).top().left().growY().uniformY();

        if((container.getChildren().size) % columns() == 0){
            container.row();
        }
    }

    void finishLocalHosts(){
        if(totalHosts == 0){
            local.clear();
            local.background(Tex.button);
            local.add("@hosts.none").pad(10f);
            local.add().growX();
            local.button(Icon.refresh, this::refreshLocal).pad(-12f).padLeft(0).size(70f);
        }else{
            local.background(null);
        }
    }

    void addLocalHost(Host host){
        if(totalHosts == 0){
            local.clear();
        }
        local.background(null);
        totalHosts++;
        float w = targetWidth();

        if((local.getChildren().size) % columns() == 0){
            local.row();
        }

        local.button(b -> buildServer(host, b), style, () -> {
            Events.fire(new ClientPreConnectEvent(host));
            safeConnect(host.address, host.port, host.version);
        }).width(w).top().left().growY();
    }

    public void connect(String ip, int port){
        if(player.name.trim().isEmpty()){
            ui.showInfo("@noname");
            return;
        }

        ui.loadfrag.show("@connecting");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        Time.runTask(2f, () -> {
            logic.reset();
            net.reset();
            Vars.netClient.beginConnecting();
            net.connect(lastIp = ip, lastPort = port, () -> {
                if(net.client()){
                    hide();
                    add.hide();
                }
            });
        });
    }

    public void reconnect(){
        if(lastIp == null || lastIp.isEmpty()) return;
        ui.loadfrag.show("@reconnecting");

        ping = Timer.schedule(() -> {
            net.pingHost(lastIp, lastPort, host -> {
                if(ping == null) return;
                ping.cancel();
                ping = null;
                connect(lastIp, lastPort);
            }, exception -> {});
        }, 1, 1);
        
        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            if(ping == null) return;
            ping.cancel();
            ping = null;
        });
    }

    void safeConnect(String ip, int port, int version){
        if(version != Version.build && Version.build != -1 && version != -1){
            ui.showInfo("[scarlet]" + (version > Version.build ? KickReason.clientOutdated : KickReason.serverOutdated) + "\n[]" +
                Core.bundle.format("server.versions", Version.build, version));
        }else{
            connect(ip, port);
        }
    }

    float targetWidth(){
        return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 550f);
    }

    @SuppressWarnings("unchecked")
    private void loadServers(){
        servers = Core.settings.getJson("servers", Seq.class, Server.class, Seq::new);

        //load imported legacy data
        if(Core.settings.has("server-list")){
            servers = LegacyIO.readServers();
            Core.settings.remove("server-list");
        }

        var url = becontrol.active() ? serverJsonBeURL : serverJsonURL;
        Log.info("Fetching community servers at @", url);

        //get servers
        Http.get(url)
        .error(t -> Log.err("Failed to fetch community servers", t))
        .submit(result -> {
            Jval val = Jval.read(result.getResultAsString());
            Seq<ServerGroup> servers = new Seq<>();
            val.asArray().each(child -> {
                String name = child.getString("name", "");
                String[] addresses;
                if(child.has("addresses") || (child.has("address") && child.get("address").isArray())){
                    addresses = (child.has("addresses") ? child.get("addresses") : child.get("address")).asArray().map(Jval::asString).toArray(String.class);
                }else{
                    addresses = new String[]{child.getString("address", "<invalid>")};
                }
                servers.add(new ServerGroup(name, addresses));
            });
            //modify default servers on main thread
            Core.app.post(() -> {
                servers.sort(s -> s.name == null ? Integer.MAX_VALUE : s.name.hashCode());
                defaultServers.addAll(servers);
                Log.info("Fetched @ community servers.", defaultServers.size);
            });
        });
    }

    private void saveServers(){
        Core.settings.putJson("servers", Server.class, servers);
    }

    public static class Server{
        public String ip;
        public int port;

        transient Table content;
        transient Host lastHost;

        void setIP(String ip){
            try{
                boolean isIpv6 = Strings.count(ip, ':') > 1;
                if(isIpv6 && ip.lastIndexOf("]:") != -1 && ip.lastIndexOf("]:") != ip.length() - 1){
                    int idx = ip.indexOf("]:");
                    this.ip = ip.substring(1, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 2));
                }else if(!isIpv6 && ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1){
                    int idx = ip.lastIndexOf(':');
                    this.ip = ip.substring(0, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 1));
                }else{
                    this.ip = ip;
                    this.port = Vars.port;
                }
            }catch(Exception e){
                this.ip = ip;
                this.port = Vars.port;
            }
        }

        String displayIP(){
            if(Strings.count(ip, ':') > 1){
                return port != Vars.port ? "[" + ip + "]:" + port : ip;
            }else{
                return ip + (port != Vars.port ? ":" + port : "");
            }
        }

        public Server(){
        }
    }
}
