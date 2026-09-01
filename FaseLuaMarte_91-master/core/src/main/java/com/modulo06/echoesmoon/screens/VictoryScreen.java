package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class VictoryScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont fontTitle;
    private BitmapFont fontOptions;

    public VictoryScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(2.5f);

        fontOptions = new BitmapFont();
        fontOptions.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        fontTitle.setColor(Color.GREEN);
        fontTitle.draw(batch, "Missão completa!", 250, 420);

        fontOptions.setColor(Color.YELLOW);
        fontOptions.draw(batch, "Pressione [R] para JOGAR NOVAMENTE", 180, 240);

        fontOptions.setColor(Color.LIGHT_GRAY);
        fontOptions.draw(batch, "Pressione [ESC] para SAIR DO JOGO", 190, 180);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new GameScreen(game, 0));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fontTitle.dispose();
        fontOptions.dispose();
    }
}
