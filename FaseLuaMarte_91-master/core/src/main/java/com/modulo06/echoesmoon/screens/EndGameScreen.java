package com.modulo06.echoesmoon.screens; // Ajuste para o seu pacote atual se necessário

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.modulo06.echoesmoon.EchoesMoonGame;

public class EndGameScreen implements Screen {
    private Game game;
    private String message;
    private SpriteBatch batch;
    private BitmapFont font;

    // CONSTRUTOR ATUALIZADO: Agora aceita o jogo e a mensagem de vitória/derrota
    public EndGameScreen(Game game, String message) {
        this.game = game;
        this.message = message;
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.draw(batch, message, 300, 350);
        font.draw(batch, "Pressione ESPACO para Voltar ao Menu", 270, 300);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new MenuScreen((EchoesMoonGame) game));
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
