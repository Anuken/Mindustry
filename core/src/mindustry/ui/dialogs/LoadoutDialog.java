package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class LoadoutDialog extends BaseDialog{
    private Runnable hider;
    private Runnable resetter;
    private Runnable updater;
    //TODO use itemseqs
    private Seq<ItemStack> stacks = new Seq<>();
    private Seq<ItemStack> originalStacks = new Seq<>();
    private Boolf<Item> validator = i -> true;
    private Table items;
    private int capacity;
    private @Nullable ItemSeq total;

    public LoadoutDialog(){
        super("@configure");
        setFillParent(true);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
            }
        });

        cont.pane(t -> items = t.margin(10f)).left();

        shown(this::setup);
        hidden(() -> {
            originalStacks.selectFrom(stacks, s -> s.amount > 0);
            updater.run();
            if(hider != null){
                hider.run();
            }
        });

        buttons.button("@back", Icon.left, this::hide).size(210f, 64f);

        buttons.button("@max", Icon.export, this::maxItems).size(210f, 64f);

        buttons.button("@settings.reset", Icon.refresh, () -> {
            resetter.run();
            reseed();
            updater.run();
            setup();
        }).size(210f, 64f);
    }

    public void maxItems() {
        for(ItemStack stack : stacks){
            stack.amount = total == null ? capacity : Math.max(Math.min(capacity, total.get(stack.item)), 0);
        }
    }

    public void show(int capacity, Seq<ItemStack> stacks, Boolf<Item> validator, Runnable reseter, Runnable updater, Runnable hider){
        show(capacity, null, stacks, validator, reseter, updater, hider);
    }

    public void show(int capacity, ItemSeq total, Seq<ItemStack> stacks, Boolf<Item> validator, Runnable reseter, Runnable updater, Runnable hider){
        this.originalStacks = stacks;
        this.validator = validator;
        this.resetter = reseter;
        this.updater = updater;
        this.capacity = capacity;
        this.total = total;
        this.hider = hider;
        reseed();
        show();
    }

    void setup(){
        items.clearChildren();
        items.left();
        float bsize = 40f;

        int i = 0;

        for(ItemStack stack : stacks){
            items.table(Tex.pane, t -> {
                t.margin(4).marginRight(8).left();
                t.button("-", Styles.cleart, () -> {
                    stack.amount = Math.max(stack.amount - step(stack.amount), 0);
                    updater.run();
                }).size(bsize);

                t.button("+", Styles.cleart, () -> {
                    stack.amount = Math.min(stack.amount + step(stack.amount), capacity);
                    updater.run();
                }).size(bsize);

                t.button(Icon.pencil, Styles.cleari, () -> ui.showTextInput("@configure", stack.item.localizedName, 10, stack.amount + "", true, str -> {
                    if(Strings.canParsePositiveInt(str)){
                        int amount = Strings.parseInt(str);
                        if(amount >= 0 && amount <= capacity){
                            stack.amount = amount;
                            updater.run();
                            return;
                        }
                    }
                    ui.showInfo(Core.bundle.format("configure.invalid", capacity));
                })).size(bsize);

                t.image(stack.item.uiIcon).size(8 * 3).padRight(4).padLeft(4);
                t.label(() -> stack.amount + "").left().width(90f);
            }).pad(2).left().fillX();


            if(++i % 2 == 0 || (mobile && Core.graphics.isPortrait())){
                items.row();
            }
        }
    }

    private void reseed(){
        this.stacks = originalStacks.map(ItemStack::copy);
        this.stacks.addAll(content.items().select(i -> validator.get(i) && !stacks.contains(stack -> stack.item == i)).map(i -> new ItemStack(i, 0)));
        this.stacks.sort(Structs.comparingInt(s -> s.item.id));
    }

    private int step(int amount){
        if(amount < 1000){
            return 100;
        }else if(amount < 2000){
            return 200;
        }else if(amount < 5000){
            return 500;
        }else{
            return 1000;
        }
    }
}
