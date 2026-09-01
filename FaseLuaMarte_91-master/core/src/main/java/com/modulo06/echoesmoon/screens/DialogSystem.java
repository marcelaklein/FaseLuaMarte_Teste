package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

public class DialogSystem {

    // Máquina de estados exigida
    public enum State {
        CLOSED,
        OPEN,
        FINISHED
    }

    private State state = State.CLOSED;
    private Array<String> lines;
    private int currentIndex = 0;
    private boolean flagGenerated = false;

    public DialogSystem() {
        this.lines = new Array<>();
    }

    /**
     * Inicia o diálogo com 2 a 3 falas.
     */
    public void startDialog(String[] dialogLines) {
        this.lines.clear();
        for (String line : dialogLines) {
            this.lines.add(line);
        }
        this.currentIndex = 0;
        this.state = State.OPEN;
        this.flagGenerated = false;
    }

    /**
     * Atualiza o estado do diálogo.
     * Usa isKeyJustPressed para não queimar as falas em um único frame.
     */
    public void update(float delta) {
        if (state != State.OPEN) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            currentIndex++;

            // Quando passa da última fala -> FINISHED -> Gera a flag
            if (currentIndex >= lines.size) {
                state = State.FINISHED;
                flagGenerated = true;
            }
        }
    }

    /**
     * Renderiza a caixa de diálogo e o texto na tela.
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (state != State.OPEN) return;

        float margin = 20f;
        float boxWidth = screenWidth - (margin * 2);
        float boxHeight = 120f;
        float boxX = margin;
        float boxY = margin;

        // 1. Fundo semi-transparente e borda
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.05f, 0.05f, 0.15f, 0.9f));
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        // 2. Renderização do texto
        batch.begin();
        font.setColor(Color.WHITE);
        if (currentIndex < lines.size) {
            font.draw(batch, lines.get(currentIndex), boxX + 25, boxY + boxHeight - 30);
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "[ENTER / E] Avançar", boxX + boxWidth - 180, boxY + 25);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public State getState() {
        return state;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public boolean hasFlagGenerated() {
        return flagGenerated;
    }

    public void close() {
        this.state = State.CLOSED;
    }
}
