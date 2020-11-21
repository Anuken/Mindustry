package mindustry.ui;


import arc.*;
import arc.func.*;

/**
 * A low-garbage way to format bundle strings.
 */
public class IntFormat{
    private final StringBuilder builder = new StringBuilder();
    private final String text;
    private int lastValue = Integer.MIN_VALUE, lastValue2 = Integer.MIN_VALUE;
    private Func<Integer, String> converter = String::valueOf;

    public IntFormat(String text){
        this.text = text;
    }

    public IntFormat(String text, Func<Integer, String> converter){
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

    public CharSequence get(int value1, int value2){
        if(lastValue != value1 || lastValue2 != value2){
            builder.setLength(0);
            builder.append(Core.bundle.format(text, value1, value2));
        }
        lastValue = value1;
        lastValue2 = value2;
        return builder;
    }
}
