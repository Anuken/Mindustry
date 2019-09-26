package io.anuke.mindustry.ui;

public enum IconSize{
    def(48),
    small(32),
    smaller(30),
    tiny(16);

    public final int size;

    IconSize(int size){
        this.size = size;
    }
}
