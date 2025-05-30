package mindustry.logic;

public enum CutsceneAction{
    active,
    pan,
    zoom,
    stop,
    getHud,
    setHud;

    public static final CutsceneAction[] all = values();
}
