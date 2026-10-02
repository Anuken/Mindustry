package arc.input;

import arc.struct.*;

public class KeyboardDevice implements InputProcessor{
    private final IntSet pressed = new IntSet(), justReleased = new IntSet();
    private final IntSet lastFramePressed = new IntSet();
    private final IntSet justPressed = new IntSet();
    private final IntFloatMap axes = new IntFloatMap();

    public void postUpdate(){
        lastFramePressed.clear();
        lastFramePressed.addAll(pressed);
        justPressed.clear();
        justReleased.clear();
        axes.clear();
    }

    public boolean isPressed(KeyCode key){
        if(key == KeyCode.anyKey) return pressed.size > 0;

        return pressed.contains(key.ordinal());
    }

    public boolean isTapped(KeyCode key){
        return justPressed.contains(key.ordinal());
    }

    public boolean isReleased(KeyCode key){
        return justReleased.contains(key.ordinal());
    }

    public float getAxis(KeyCode keyCode){
        return axes.get(keyCode.ordinal(), 0);
    }

    @Override
    public boolean keyDown(KeyCode key){
        pressed.add(key.ordinal());
        justPressed.add(key.ordinal());
        return false;
    }

    @Override
    public boolean keyUp(KeyCode key){
        pressed.remove(key.ordinal());
        justReleased.add(key.ordinal());
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        keyDown(button);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        if(pointer == 0) keyUp(button);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY){
        axes.put(KeyCode.scroll.ordinal(), -amountY);
        return false;
    }
}
