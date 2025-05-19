package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

/**
 * A statement is an intermediate representation of an instruction, to be used mostly in UI.
 * Contains all relevant variable information. */
public abstract class LStatement{
    public transient @Nullable StatementElem elem;

    public abstract void build(Table table);

    public abstract LInstruction build(LAssembler builder);

    public LCategory category(){
        return LCategory.unknown;
    }

    public LStatement copy(){
        StringBuilder build = new StringBuilder();
        write(build);
        //assume privileged when copying, because there's no way privileged instructions can appear here anyway, and the instructions get validated on load anyway
        Seq<LStatement> read = LAssembler.read(build.toString(), true);
        return read.size == 0 ? null : read.first();
    }

    public boolean hidden(){
        return false;
    }

    /** Privileged instructions are only allowed in world processors. */
    public boolean privileged(){
        return false;
    }

    /** If true, this statement is considered useless with privileged processors and is not allowed in them. */
    public boolean nonPrivileged(){
        return false;
    }

    //protected methods are only for internal UI layout utilities

    protected void param(Cell<Label> label){
        String text = name() + "." + label.get().getText().toString().trim();
        tooltip(label, text);
    }

    protected String sanitize(String value){
        if(value.length() == 0){
            return "";
        }else if(value.length() == 1){
            if(value.charAt(0) == '"' || value.charAt(0) == ';' || value.charAt(0) == ' '){
                return "invalid";
            }
        }else{
            StringBuilder res = new StringBuilder(value.length());
            if(value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'){
                res.append('\"');
                //strip out extra quotes
                for(int i = 1; i < value.length() - 1; i++){
                    if(value.charAt(i) == '"'){
                        res.append('\'');
                    }else{
                        res.append(value.charAt(i));
                    }
                }
                res.append('\"');
            }else{
                //otherwise, strip out semicolons, spaces and quotes
                for(int i = 0; i < value.length(); i++){
                    char c = value.charAt(i);
                    res.append(switch(c){
                        case ';' -> 's';
                        case '"' -> '\'';
                        case ' ' -> '_';
                        default -> c;
                    });
                }
            }

            return res.toString();
        }

        return value;
    }

    protected Cell<TextField> field(Table table, String value, Cons<String> setter){
        return table.field(value, Styles.nodeField, s -> setter.get(sanitize(s)))
            .size(144f, 40f).pad(2f).color(table.color);
    }

    protected Cell<TextField> fields(Table table, String desc, String value, Cons<String> setter){
        table.add(desc).padLeft(10).left().self(this::param);
        return field(table, value, setter).width(85f).padRight(10).left();
    }

    /** Puts the text and field in one table, taking up one cell. */
    protected Cell<TextField> fieldst(Table table, String desc, String value, Cons<String> setter){
        Cell[] result = {null};
        table.table(t -> {
            t.setColor(table.color);
            t.add(desc).padLeft(10).left().self(this::param);
            result[0] = field(t, value, setter).width(85f).padRight(10).left();
        });

        return result[0];
    }

    /** Adds color edit button */
    protected Cell<Button> col(Table table, String value, Cons<Color> setter){
        return table.button(b -> {
            b.image(Icon.pencilSmall);
            b.clicked(() -> {
                Color current = Pal.accent.cpy();
                if(value.startsWith("%")){
                    try{
                        current = Color.valueOf(value.substring(1));
                    }catch(Exception ignored){}
                }

                ui.picker.show(current, setter);
            });
        }, Styles.logict, () -> {}).size(40f).padLeft(-11).color(table.color);
    }

    protected Cell<TextField> fields(Table table, String value, Cons<String> setter){
        return field(table, value, setter).width(85f);
    }

    protected void row(Table table){
        if(LCanvas.useRows()){
            table.row();
        }
    }

    protected <T> void showSelect(Button b, T[] values, T current, Cons<T> getter, int cols, Cons<Cell> sizer){
        showSelectTable(b, (t, hide) -> {
            ButtonGroup<Button> group = new ButtonGroup<>();
            int i = 0;
            t.defaults().size(60f, 38f);

            for(T p : values){
                sizer.get(t.button(p.toString(), Styles.logicTogglet, () -> {
                    getter.get(p);
                    hide.run();
                }).self(c -> {
                    if(p instanceof Enum e){
                        tooltip(c, e);
                    }
                }).checked(current.equals(p)).group(group));

                if(++i % cols == 0) t.row();
            }
        });
    }

    protected <T> void showSelect(Button b, T[] values, T current, Cons<T> getter){
        showSelect(b, values, current, getter, 4, c -> {});
    }

    protected void showSelectTable(Button b, Cons2<Table, Runnable> hideCons){
        Table t = new Table(Tex.paneSolid){
            @Override
            public float getPrefHeight(){
                return Math.min(super.getPrefHeight(), Core.graphics.getHeight());
            }

            @Override
            public float getPrefWidth(){
                return Math.min(super.getPrefWidth(), Core.graphics.getWidth());
            }
        };
        t.margin(4);

        //triggers events behind the element to simulate deselection
        Element hitter = new Element();

        Runnable hide = () -> {
            Core.app.post(hitter::remove);
            t.actions(Actions.fadeOut(0.3f, Interp.fade), Actions.remove());
        };

        hitter.fillParent = true;
        hitter.tapped(hide);

        Core.scene.add(hitter);
        Core.scene.add(t);

        t.update(() -> {
            if(b.parent == null || !b.isDescendantOf(Core.scene.root)){
                Core.app.post(() -> {
                    hitter.remove();
                    t.remove();
                });
                return;
            }

            b.localToStageCoordinates(Tmp.v1.set(b.getWidth()/2f, b.getHeight()/2f));
            t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
            if(t.getWidth() > Core.scene.getWidth()) t.setWidth(Core.graphics.getWidth());
            if(t.getHeight() > Core.scene.getHeight()) t.setHeight(Core.graphics.getHeight());
            t.keepInStage();
            t.invalidateHierarchy();
            t.pack();
        });
        t.actions(Actions.alpha(0), Actions.fadeIn(0.3f, Interp.fade));

        t.top().pane(inner -> {
            inner.top();
            hideCons.get(inner, hide);
        }).pad(0f).top().scrollX(false);

        t.pack();
    }

    public void afterRead(){}

    public void write(StringBuilder builder){
        LogicIO.write(this, builder);
    }

    public void setupUI(){

    }

    public void saveUI(){

    }

    public String typeName(){
        return getClass().getSimpleName().replace("Statement", "");
    }

    public String name(){
        return Strings.insertSpaces(getClass().getSimpleName().replace("Statement", ""));
    }

}
