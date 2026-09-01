package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.modulo06.echoesmoon.EchoesMoonGame;

public class PauseMenuScreen implements Screen {

    private Game game;
    private Screen previousScreen;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont fontTitle;
    private BitmapFont fontOptions;
    private String saveStatusMessage = "";
    private float messageTimer = 0f;

    public PauseMenuScreen(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(2.0f);

        fontOptions = new BitmapFont();
        fontOptions.getData().setScale(1.3f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "JOGO PAUSADO", 290, 450);

        fontOptions.setColor(Color.YELLOW);
        fontOptions.draw(batch, "Pressione [ENTER] para Retornar (Play)", 200, 340);
        fontOptions.draw(batch, "Pressione [S] para Salvar o Jogo", 230, 290);
        fontOptions.draw(batch, "Pressione [ESC] para Sair ao Menu Principal", 200, 240);

        if (!saveStatusMessage.isEmpty()) {
            fontOptions.setColor(Color.GREEN);
            fontOptions.draw(batch, saveStatusMessage, 280, 160);
            messageTimer -= delta;
            if (messageTimer <= 0) {
                saveStatusMessage = "";
            }
        }
        batch.end();

        // Retornar ao jogo
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(previousScreen);
        }

        // Salvar manualmente
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            saveGameManually();
        }

        // Sair para o Menu Principal
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen((EchoesMoonGame) game));
        }
    }

    private void saveGameManually() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putString("manualSave", "true");
        prefs.flush();
        saveStatusMessage = "Jogo Salvo com Sucesso!";
        messageTimer = 2.0f; // Mensagem exibe por 2 segundos
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
