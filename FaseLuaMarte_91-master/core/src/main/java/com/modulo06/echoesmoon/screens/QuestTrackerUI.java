package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

public class QuestTrackerUI {

    public static class QuestStep {
        String description;
        boolean completed;

        public QuestStep(String description, boolean completed) {
            this.description = description;
            this.completed = completed;
        }
    }

    private Array<QuestStep> steps;
    private String trackerTitle;

    public QuestTrackerUI(String trackerTitle) {
        this.trackerTitle = trackerTitle;
        this.steps = new Array<>();
    }

    public void setSteps(Array<QuestStep> steps) {
        this.steps = steps;
    }

    /**
     * Desenha o Quest Tracker dentro de um quadrado/painel no canto superior direito da tela.
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (steps.size == 0) return;

        // Dimensões e Posição do Painel (Canto Superior Direito)
        float panelWidth = 340f;
        float padding = 15f;
        float lineHeight = 22f;
        float panelHeight = padding * 2 + 25f + (steps.size * lineHeight);

        float panelX = screenWidth - panelWidth - 20f;
        float panelY = screenHeight - panelHeight - 20f;

        // 1. Desenha o fundo semi-transparente e a borda com o ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.05f, 0.05f, 0.1f, 0.85f)); // Fundo escuro azulado
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.CYAN); // Borda estilizada
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // 2. Desenha os textos com o SpriteBatch
        batch.begin();
        // Título do Tracker
        font.setColor(Color.YELLOW);
        font.draw(batch, trackerTitle, panelX + padding, panelY + panelHeight - padding);

        // Linha divisória simulada ou espaçamento
        float currentY = panelY + panelHeight - padding - 25f;

        font.setColor(Color.WHITE);
        for (QuestStep step : steps) {
            String checkbox = step.completed ? "[X] " : "[ ] ";
            String text = checkbox + step.description;

            // Se estiver concluído, pinta o texto de verde claro para destacar
            if (step.completed) {
                font.setColor(Color.GREEN);
            } else {
                font.setColor(Color.LIGHT_GRAY);
            }

            font.draw(batch, text, panelX + padding, currentY);
            currentY -= lineHeight;
        }
        font.setColor(Color.WHITE); // Reseta a cor padrão da fonte
        batch.end();
    }
}
