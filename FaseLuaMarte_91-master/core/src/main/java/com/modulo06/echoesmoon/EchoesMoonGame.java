package com.modulo06.echoesmoon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.modulo06.echoesmoon.screens.MenuScreen;

public class EchoesMoonGame extends Game {

    // NOVO: AssetManager global do jogo
    public AssetManager assets;

    @Override
    public void create() {
        assets = new AssetManager();
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        // Libera a memória de todos os sprites quando o jogo fecha
        if (assets != null) {
            assets.dispose();
        }
    }
}
