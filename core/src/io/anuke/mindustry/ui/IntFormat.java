package io.anuke.mindustry.ui;

import io.anuke.arc.util.Bundles;

/**
 * A low-garbage way to format bundle strings.
 */
public class IntFormat{
    private final StringBuilder builder = new StringBuilder();
    private final String text;
    private int lastValue = Integer.MIN_VALUE;

    public IntFormat(String text){
        this.text = text;
    }

    public CharSequence get(int value){
        if(lastValue != value){
            builder.setLength(0);
            builder.append(Core.bundle.format(text, value));
        }
        lastValue = value;
        return builder;
    }
}
