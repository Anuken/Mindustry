package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.ui.*;

import java.util.*;

import static io.anuke.mindustry.Vars.platform;

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

    public FileChooser(String title, Predicate<FileHandle> filter, boolean open, Consumer<FileHandle> result){
        super(title);
        setFillParent(true);
        this.open = open;
        this.filter = filter;
        this.selectListener = result;

        onResize(() -> {
            cont.clear();
            setupWidgets();
        });

        shown(() -> {
            cont.clear();
            setupWidgets();
        });
    }

    private void setupWidgets(){
        cont.margin(-10);

        Table content = new Table();

        filefield = new TextField();
        filefield.setOnlyFontChars(false);
        if(!open) platform.addDialog(filefield);
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

        pane = new ScrollPane(files);
        pane.setOverscroll(false, false);
        pane.setFadeScrollBars(false);

        updateFiles(true);

        Table icontable = new Table();

        ImageButton up = new ImageButton(Icon.folderParent);
        up.clicked(() -> {
            directory = directory.parent();
            updateFiles(true);
        });

        //Macs are confined to the Downloads/ directory
        if(OS.isMac){
            up.setDisabled(true);
        }

        ImageButton back = new ImageButton(Icon.arrowLeft);
        ImageButton forward = new ImageButton(Icon.arrowRight);

        forward.clicked(() -> stack.forward());
        back.clicked(() -> stack.back());
        forward.setDisabled(() -> !stack.canForward());
        back.setDisabled(() -> !stack.canBack());

        ImageButton home = new ImageButton(Icon.home);
        home.clicked(() -> {
            directory = homeDirectory;
            lastDirectory = directory;
            updateFiles(true);
        });

        icontable.defaults().height(60).growX().padTop(5).uniform();
        icontable.add(home);
        icontable.add(back);
        icontable.add(forward);
        icontable.add(up);

        Table fieldcontent = new Table();
        fieldcontent.bottom().left().add(new Label("$filename"));
        fieldcontent.add(filefield).height(40f).fillX().expandX().padLeft(10f);

        Table buttons = new Table();
        buttons.defaults().growX().height(60);
        buttons.add(cancel);
        buttons.add(ok);

        content.top().left();
        content.add(icontable).expandX().fillX();
        content.row();

        content.center().add(pane).colspan(3).grow();
        content.row();

        if(!open){
            content.bottom().left().add(fieldcontent).colspan(3).grow().padTop(-2).padBottom(2);
            content.row();
        }

        content.add(buttons).growX();

        cont.add(content).grow();
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

        layout.setText(Fonts.def, navigation.getText());

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
            Image upimage = new Image(Icon.folderParentSmall);
            TextButton upbutton = new TextButton(".." + directory.toString(), Styles.clearTogglet);
            upbutton.clicked(() -> {
                directory = directory.parent();
                lastDirectory = directory;
                updateFiles(true);
            });

            upbutton.left().add(upimage).padRight(4f).padLeft(4);
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

            TextButton button = new TextButton(filename, Styles.clearTogglet);
            button.getLabel().setWrap(false);
            button.getLabel().setEllipsis(true);
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

            Image image = new Image(file.isDirectory() ? Icon.folderSmall : Icon.fileTextSmall);

            button.add(image).padRight(4f).padLeft(4);
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
