package mindustry.ui;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.meta.*;

//TODO replace with static methods?
/** An item image with text. */
public class ItemDisplay extends Table{
    public final Item item;
    public final int amount;

    public ItemDisplay(Item item){
        this(item, 0);
    }

    public ItemDisplay(Item item, int amount, boolean showName){
        add(new ItemImage(new ItemStack(item, amount)));
        if(showName) add(item.localizedName).padLeft(4 + amount > 99 ? 4 : 0);

        this.item = item;
        this.amount = amount;
    }

    public ItemDisplay(Item item, int amount){
        this(item, amount, true);
    }

    /** Displays the item with a "/sec" qualifier based on the time period, in ticks. */
    public ItemDisplay(Item item, int amount, float timePeriod, boolean showName){
        add(new ItemImage(item.uiIcon, amount));
        add((showName ? item.localizedName + "\n" : "") + "[lightgray]" + Strings.autoFixed(amount / (timePeriod / 60f), 2) + StatUnit.perSecond.localized()).padLeft(2).padRight(5).style(Styles.outlineLabel);

        this.item = item;
        this.amount = amount;
    }
}
