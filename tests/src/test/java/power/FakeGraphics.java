package power;

import arc.Graphics;
import arc.Graphics.Cursor.SystemCursor;
import arc.graphics.*;
import arc.graphics.gl.GLVersion;

public class FakeGraphics extends Graphics{
    static int frame;

    @Override
    public boolean isGL30Available(){
        return false;
    }

    @Override
    public GL20 getGL20(){
        return null;
    }

    @Override
    public void setGL20(GL20 gl20){

    }

    @Override
    public GL30 getGL30(){
        return null;
    }

    @Override
    public void setGL30(GL30 gl30){

    }

    @Override
    public int getWidth(){
        return 0;
    }

    @Override
    public int getHeight(){
        return 0;
    }

    @Override
    public int getBackBufferWidth(){
        return 0;
    }

    @Override
    public int getBackBufferHeight(){
        return 0;
    }

    @Override
    public long getFrameId(){
        return frame++;
    }

    @Override
    public float getDeltaTime(){
        return 0;
    }

    @Override
    public float getRawDeltaTime(){
        return 0;
    }

    @Override
    public int getFramesPerSecond(){
        return 0;
    }

    @Override
    public GLVersion getGLVersion(){
        return null;
    }

    @Override
    public float getPpiX(){
        return 0;
    }

    @Override
    public float getPpiY(){
        return 0;
    }

    @Override
    public float getPpcX(){
        return 0;
    }

    @Override
    public float getPpcY(){
        return 0;
    }

    @Override
    public float getDensity(){
        return 0;
    }

    @Override
    public boolean supportsDisplayModeChange(){
        return false;
    }

    @Override
    public Monitor getPrimaryMonitor(){
        return null;
    }

    @Override
    public Monitor getMonitor(){
        return null;
    }

    @Override
    public Monitor[] getMonitors(){
        return new Monitor[0];
    }

    @Override
    public DisplayMode[] getDisplayModes(){
        return new DisplayMode[0];
    }

    @Override
    public DisplayMode[] getDisplayModes(Monitor monitor){
        return new DisplayMode[0];
    }

    @Override
    public DisplayMode getDisplayMode(){
        return null;
    }

    @Override
    public DisplayMode getDisplayMode(Monitor monitor){
        return null;
    }

    @Override
    public boolean setFullscreenMode(DisplayMode displayMode){
        return false;
    }

    @Override
    public boolean setWindowedMode(int width, int height){
        return false;
    }

    @Override
    public void setTitle(String title){

    }

    @Override
    public void setUndecorated(boolean undecorated){

    }

    @Override
    public void setResizable(boolean resizable){

    }

    @Override
    public void setVSync(boolean vsync){

    }

    @Override
    public BufferFormat getBufferFormat(){
        return null;
    }

    @Override
    public boolean supportsExtension(String extension){
        return false;
    }

    @Override
    public boolean isContinuousRendering(){
        return false;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous){

    }

    @Override
    public void requestRendering(){

    }

    @Override
    public boolean isFullscreen(){
        return false;
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot){
        return null;
    }

    @Override
    protected void setCursor(Cursor cursor){

    }

    @Override
    protected void setSystemCursor(SystemCursor systemCursor){

    }
}
