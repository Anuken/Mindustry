package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.OS;
import io.anuke.ucore.util.Pooling;

import java.util.Arrays;

public class FileChooser extends FloatingDialog{
    public static Predicate<FileHandle> pngFilter = file -> file.extension().equalsIgnoreCase("png");
    public static Predicate<FileHandle> mapFilter = file -> file.extension().equalsIgnoreCase(Vars.mapExtension);
    public static Predicate<FileHandle> jpegFilter = file -> file.extension().equalsIgnoreCase("png") || file.extension().equalsIgnoreCase("jpg") || file.extension().equalsIgnoreCase("jpeg");
    public static Predicate<FileHandle> defaultFilter = file -> true;
    private Table files;
    private FileHandle homeDirectory = Gdx.files.absolute(OS.isMac ? OS.getProperty("user.home") + "/Downloads/" :
            Gdx.files.getExternalStoragePath());
    private FileHandle directory = homeDirectory;
    private ScrollPane pane;
    private TextField navigation, filefield;
    private TextButton ok;
    private FileHistory stack = new FileHistory();
    private Predicate<FileHandle> filter;
    private Consumer<FileHandle> selectListener;
    private boolean open;

    public FileChooser(String title, Predicate<FileHandle> filter, boolean open, Consumer<FileHandle> result){
        super(title);
        this.open = open;
        this.filter = filter;
        this.selectListener = result;
    }

    private void setupWidgets(){
        content().margin(-10);

        Table content = new Table();

        filefield = new TextField();
        filefield.setOnlyFontChars(false);
        if(!open) Platform.instance.addDialog(filefield);
        filefield.setDisabled(open);

        ok = new TextButton(open ? "$text.load" : "$text.save");

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

        TextButton cancel = new TextButton("$text.cancel");
        cancel.clicked(this::hide);

        navigation = new TextField("");
        navigation.setTouchable(Touchable.disabled);

        files = new Table();
        files.marginRight(10);
        files.marginLeft(3);

        pane = new ScrollPane(files){
            public float getPrefHeight(){
                return Gdx.graphics.getHeight();
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
            updateFiles(true);
        });

        icontable.defaults().height(50).growX().padTop(5).uniform();
        icontable.add(home);
        icontable.add(back);
        icontable.add(forward);
        icontable.add(up);

        Table fieldcontent = new Table();
        fieldcontent.bottom().left().add(new Label("$text.filename"));
        fieldcontent.add(filefield).height(40f).fillX().expandX().padLeft(10f);

        Table buttons = new Table();
        buttons.defaults().growX().height(50);
        buttons.add(cancel);
        buttons.add(ok);

        content.top().left();
        content.add(icontable).expandX().fillX();
        content.row();

        content.center().add(pane).width(UIUtils.portrait() ? Gdx.graphics.getWidth() / Unit.dp.scl(1) : Gdx.graphics.getWidth() / Unit.dp.scl(2)).colspan(3).grow();
        content.row();

        if(!open){
            content.bottom().left().add(fieldcontent).colspan(3).grow().padTop(-2).padBottom(2);
            content.row();
        }

        content.add(buttons).growX();

        content().add(content);
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

        GlyphLayout layout = Pooling.obtain(GlyphLayout.class, GlyphLayout::new);

        layout.setText(Core.font, navigation.getText());

        if(layout.width < navigation.getWidth()){
            navigation.setCursorPosition(0);
        }else{
            navigation.setCursorPosition(navigation.getText().length());
        }

        Pooling.free(layout);

        files.clearChildren();
        files.top().left();
        FileHandle[] names = getFileNames();

        //macs are confined to the Downloads/ directory
        if(!OS.isMac){
            Image upimage = new Image("icon-folder-parent");
            TextButton upbutton = new TextButton(".." + directory.toString(), "clear-toggle");
            upbutton.clicked(() -> {
                directory = directory.parent();
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
        Timers.runTask(2f, () -> {
            content().clear();
            setupWidgets();
            super.show();
            Core.scene.setScrollFocus(pane);
        });
        return this;
    }

    public void fileSelected(Consumer<FileHandle> listener){
        this.selectListener = listener;
    }

    public interface FileHandleFilter{
        boolean accept(FileHandle file);
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
            updateFiles(false);
        }

        public void forward(){
            if(!canForward()) return;
            directory = history.get(index);
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
