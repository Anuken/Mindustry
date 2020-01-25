package mindustry.ui;

public class TimeFormat{
    private final StringBuilder ibuild = new StringBuilder();
    private final IntFormat iformat;

    public TimeFormat(String text){
        this.iformat = new IntFormat(text, this::converter);
    }

    public CharSequence get(int value){
        return iformat.get(value);
    }

    private String converter(int seconds){
        ibuild.setLength(0);
        int m = seconds / 60;
        int s = seconds % 60;
        if(m > 0){
            ibuild.append(m);
            ibuild.append(":");
            if(s < 10){
                ibuild.append("0");
            }
        }
        ibuild.append(s);
        return ibuild.toString();
    }
}
