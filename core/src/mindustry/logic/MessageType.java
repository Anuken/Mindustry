package mindustry.logic;

public enum MessageType{
    notify,
    announce,
    toast,
    mission;

    public static final MessageType[] all = values();
}
