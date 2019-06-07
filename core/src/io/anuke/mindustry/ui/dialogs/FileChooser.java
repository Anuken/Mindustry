package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.g2d.GlyphLayout;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;

import java.util.Arrays;

public class FileChooser extends FloatingDialog{
    private static final FileHandle homeDirectory = Core.files.absolute(OS.isMac ? OS.getProperty("user.home") + "/Downloads/" : Core.files.getExternalStoragePath());
    private static FileHandle lastDirectory = homeDirectory;

    private Table files;
    private FileHandle directory = lastDirectory;
    private ScrollPane pane;
    private TextField navigation, filefield;
    private TextButton ok;
    private FileHistory stack = new FileHistory();
    private Predicate<FileHandle> filter;
    private Consumer<FileHandle> selectListener;
    private boolean open;
    private int lastWidth = Core.graphics.getWidth(), lastHeight = Core.graphics.getHeight();

    public static final Predicate<String> pngFiles = str -> str.equals("png");
    public static final Predicate<String> anyMapFiles = str -> str.equals(Vars.oldMapExtension) || str.equals(Vars.mapExtension);
    public static final Predicate<String> mapFiles = str -> str.equals(Vars.mapExtension);
    public static final Predicate<String> saveFiles = str -> str.equals(Vars.saveExtension);

    public FileChooser(String title, Predicate<FileHandle> filter, boolean open, Consumer<FileHandle> result){
        super(title);
        this.open = open;
        this.filter = filter;
        this.selectListener = result;

        update(() -> {
            if(Core.graphics.getWidth() != lastWidth || Core.graphics.getHeight() != lastHeight){
                updateFiles(false);
                lastHeight = Core.graphics.getHeight();
                lastWidth = Core.graphics.getWidth();
            }
        });
    }

    private void setupWidgets(){
        cont.margin(-10);

        Table content = new Table();

        filefield = new TextField();
        filefield.setOnlyFontChars(false);
        if(!open) Platform.instance.addDialog(filefield);
        filefield.setDisabled(open);

        ok = new TextButton(open ? "$load" : "$save");

        ok.clicked(() -> {
            if(ok.isDisabled()) return;
            if(selectListener != null)
                selectListener.accept(directory.child(filefield.getText()));
            hide();
        });

        filefield.changed(() -> {
            ok.setDisabled(filefield.getText().replace(" ", "").isEmpty());
        });

        filefield.change();

        TextButton cancel = new TextButton("$cancel");
        cancel.clicked(this::hide);

        navigation = new TextField("");
        navigation.touchable(Touchable.disabled);

        files = new Table();
        files.marginRight(10);
        files.marginLeft(3);

        pane = new ScrollPane(files){
            public float getPrefHeight(){
                return Core.graphics.getHeight();
            }
        };
        pane.setOverscroll(false, false);
        pane.setFadeScrollBars(false);

        updateFiles(true);

        Table icontable = new Table();

        float isize = 14 * 2;

        ImageButton up = new ImageButton("icon-folder-parent");
        up.resizeImage(isize);
        up.clicked(() -> {
            directory = directory.parent();
            updateFiles(true);
        });

        //Macs are confined to the Downloads/ directory
        if(OS.isMac){
            up.setDisabled(true);
        }

        ImageButton back = new ImageButton("icon-arrow-left");
        back.resizeImage(isize);

        ImageButton forward = new ImageButton("icon-arrow-right");
        forward.resizeImage(isize);

        forward.clicked(() -> stack.forward());

        back.clicked(() -> stack.back());

        ImageButton home = new ImageButton("icon-home");
        home.resizeImage(isize);
        home.clicked(() -> {
            directory = homeDirectory;
            lastDirectory = directory;
            updateFiles(true);
        });

        icontable.defaults().height(50).growX().padTop(5).uniform();
        icontable.add(home);
        icontable.add(back);
        icontable.add(forward);
        icontable.add(up);

        Table fieldcontent = new Table();
        fieldcontent.bottom().left().add(new Label("$filename"));
        fieldcontent.add(filefield).height(40f).fillX().expandX().padLeft(10f);

        Table buttons = new Table();
        buttons.defaults().growX().height(50);
        buttons.add(cancel);
        buttons.add(ok);

        content.top().left();
        content.add(icontable).expandX().fillX();
        content.row();

        content.center().add(pane).width(Core.graphics.isPortrait() ? Core.graphics.getWidth() / Unit.dp.scl(1) : Core.graphics.getWidth() / Unit.dp.scl(2)).colspan(3).grow();
        content.row();

        if(!open){
            content.bottom().left().add(fieldcontent).colspan(3).grow().padTop(-2).padBottom(2);
            content.row();
        }

        content.add(buttons).growX();

        cont.add(content);
    }

    private void updateFileFieldStatus(){
        if(!open){
            ok.setDisabled(filefield.getText().replace(" ", "").isEmpty());
        }else{
            ok.setDisabled(!directory.child(filefield.getText()).exists() || directory.child(filefield.getText()).isDirectory());
        }
    }

    private FileHandle[] getFileNames(){
        FileHandle[] handles = directory.list(file -> !file.getName().startsWith("."));

        Arrays.sort(handles, (a, b) -> {
            if(a.isDirectory() && !b.isDirectory()) return -1;
            if(!a.isDirectory() && b.isDirectory()) return 1;
            return String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name());
        });
        return handles;
    }

    private void updateFiles(boolean push){
        if(push) stack.push(directory);
        //if is mac, don't display extra info since you can only ever go to downloads
        navigation.setText(OS.isMac ? directory.name() : directory.toString());

        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

        layout.setText(Core.scene.skin.getFont("default-font"), navigation.getText());

        if(layout.width < navigation.getWidth()){
            navigation.setCursorPosition(0);
        }else{
            navigation.setCursorPosition(navigation.getText().length());
        }

        Pools.free(layout);

        files.clearChildren();
        files.top().left();
        FileHandle[] names = getFileNames();

        //macs are confined to the Downloads/ directory
        if(!OS.isMac){
            Image upimage = new Image("icon-folder-parent");
            TextButton upbutton = new TextButton(".." + directory.toString(), "clear-toggle");
            upbutton.clicked(() -> {
                directory = directory.parent();
                lastDirectory = directory;
                updateFiles(true);
            });

            upbutton.left().add(upimage).padRight(4f).size(14 * 2);
            upbutton.getLabel().setAlignment(Align.left);
            upbutton.getCells().reverse();

            files.add(upbutton).align(Align.topLeft).fillX().expandX().height(50).pad(2).colspan(2);
            files.row();
        }

        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);

        for(FileHandle file : names){
            if(!file.isDirectory() && !filter.test(file)) continue; //skip non-filtered files

            String filename = file.name();

            TextButton button = new TextButton(shorten(filename), "clear-toggle");
            group.add(button);

            button.clicked(() -> {
                if(!file.isDirectory()){
                    filefield.setText(filename);
                    updateFileFieldStatus();
                }else{
                    directory = directory.child(filename);
                    lastDirectory = directory;
                    updateFiles(true);
                }
            });

            filefield.changed(() -> {
                button.setChecked(filename.equals(filefield.getText()));
            });

            Image image = new Image(file.isDirectory() ? "icon-folder" : "icon-file-text");

            button.add(image).padRight(4f).size(14 * 2f);
            button.getCells().reverse();
            files.top().left().add(button).align(Align.topLeft).fillX().expandX()
            .height(50).pad(2).padTop(0).padBottom(0).colspan(2);
            button.getLabel().setAlignment(Align.left);
            files.row();
        }

        pane.setScrollY(0f);
        updateFileFieldStatus();

        if(open) filefield.clearText();
    }

    private String shorten(String string){
        int max = 30;
        if(string.length() <= max){
            return string;
        }else{
            return string.substring(0, max - 3).concat("...");
        }
    }

    @Override
    public Dialog show(){
        Time.runTask(2f, () -> {
            cont.clear();
            setupWidgets();
            super.show();
            Core.scene.setScrollFocus(pane);
        });
        return this;
    }

    public class FileHistory{
        private Array<FileHandle> history = new Array<>();
        private int index;

        public FileHistory(){

        }

        public void push(FileHandle file){
            if(index != history.size) history.truncate(index);
            history.add(file);
            index++;
        }

        public void back(){
            if(!canBack()) return;
            index--;
            directory = history.get(index - 1);
            lastDirectory = directory;
            updateFiles(false);
        }

        public void forward(){
            if(!canForward()) return;
            directory = history.get(index);
            lastDirectory = directory;
            index++;
            updateFiles(false);
        }

        public boolean canForward(){
            return !(index >= history.size);
        }

        public boolean canBack(){
            return !(index == 1) && index > 0;
        }

        void print(){

            System.out.println("\n\n\n\n\n\n");
            int i = 0;
            for(FileHandle file : history){
                i++;
                if(index == i){
                    System.out.println("[[" + file.toString() + "]]");
                }else{
                    System.out.println("--" + file.toString() + "--");
                }
            }
        }
    }
}
