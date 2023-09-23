package mindustry.logic;

public enum MessageType{
    notify,
    announce,
    toast,
    mission,
    label;

    public static final MessageType[] all = values();
}
