package mindustry.ui;

import arc.util.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.type.*;
import mindustry.world.meta.*;

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

    public ItemDisplay(Item item, int amount, float timePeriod, boolean showName){
        add(new ItemImage(item.icon(Cicon.medium), amount));
        add(Strings.autoFixed(amount / (timePeriod / 60f), 1) + StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);
        if(showName) add(item.localizedName).padLeft(4 + amount > 99 ? 4 : 0);

        this.item = item;
        this.amount = amount;
    }

    public ItemDisplay(Item item, int amount){
        this(item, amount, true);
    }
}
