package io.anuke.mindustry;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.TimeUtils;

public class SplashScreen implements Screen {
    private final Game myGame;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final Sprite sprite;
    private Texture texture;
    private long startTime;

    public SplashScreen(Game g) // ** constructor called initially **//
    {
        myGame = g;

        batch = new SpriteBatch();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera(1, h / w);

        texture = new Texture(Gdx.files.internal("ui/splash.png"));
        TextureRegion region =
                new TextureRegion(texture, 0, 0, 320, 240);
        sprite = new Sprite(region);
        sprite.setSize(1f,
                1f * sprite.getHeight() / sprite.getWidth());
        sprite.setOrigin(sprite.getWidth() / 2,
                sprite.getHeight() / 2);
        sprite.setPosition(-sprite.getWidth() / 2,
                -sprite.getHeight() / 2);

    }

    @Override
    public void show() {
        texture = new Texture(Gdx.files.internal("ui/splash.png"));
        startTime = TimeUtils.millis();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        sprite.draw(batch);
        batch.end();
        Gdx.app.postRunnable(() -> myGame.setScreen(new GameScreen()));
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        texture.dispose();
        batch.dispose();
    }
}
