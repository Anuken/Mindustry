package io.anuke.mindustry.ui;


import io.anuke.arc.Core;
import io.anuke.arc.function.Function;

/**
 * A low-garbage way to format bundle strings.
 */
public class IntFormat{
    private final StringBuilder builder = new StringBuilder();
    private final String text;
    private int lastValue = Integer.MIN_VALUE;
    private Function<Integer, String> converter = String::valueOf;

    public IntFormat(String text){
        this.text = text;
    }

    public IntFormat(String text, Function<Integer, String> converter){
        this.text = text;
        this.converter = converter;
    }

    public CharSequence get(int value){
        if(lastValue != value){
            builder.setLength(0);
            builder.append(Core.bundle.format(text, converter.get(value)));
        }
        lastValue = value;
        return builder;
    }
}
