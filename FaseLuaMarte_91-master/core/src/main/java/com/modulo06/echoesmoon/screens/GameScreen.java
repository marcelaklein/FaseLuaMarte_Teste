package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.modulo06.echoesmoon.utils.QuestTracker;

public class GameScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, playerGunTex, enemyTex, fragmentTex, bgTex, benchTex, guideTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle workbench;
    private Rectangle guide;
    private Rectangle portalMars;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;
    private int crystalsCollected = 0;
    private final int TOTAL_CRYSTALS = 4;
    private boolean hasGun = false;

    private int ammo = 10;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.5f;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;

    private String[] dialogLines = {
        "GUIA: Bem-vindo à Base da Lua, soldado!",
        "GUIA: Colete os 4 fragmentos espalhados pelo mapa e leve-os até a bancada.",
        "GUIA: Precisamos deles para construir sua arma e liberar o portal para Marte!"
    };
    private int dialogIndex = 0;

    class Bullet {
        Rectangle rect;
        Vector2 velocity;

        public Bullet(float x, float y, Vector2 dir, float speed) {
            this.rect = new Rectangle(x - 6, y - 6, 12, 12);
            this.velocity = new Vector2(dir).nor().scl(speed);
        }

        public void update(float delta) {
            rect.x += velocity.x * delta;
            rect.y += velocity.y * delta;
        }
    }

    class Fragment {
        Rectangle rect;
        boolean collected = false;
        public Fragment(float x, float y) {
            this.rect = new Rectangle(x, y, 40, 40);
        }
    }

    enum EnemyType { PATROL, CHASER }

    class Enemy {
        Rectangle rect;
        float speed;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;
        int health = 2;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB, float speed) {
            this.rect = new Rectangle(x, y, 64, 64);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 200, y);
            this.speed = speed;
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER) {
                float dist = playerPos.dst(rect.x, rect.y);
                if (dist < 500f) {
                    Vector2 dir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
            } else {
                Vector2 target = movingToB ? patrolB : patrolA;
                Vector2 dir = new Vector2(target.x - rect.x, target.y - rect.y);
                if (dir.len() < 10f) movingToB = !movingToB;
                else {
                    dir.nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
            }
        }
    }

    private Array<Fragment> fragments;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;

    public GameScreen(Game game, int startingCrystals) {
        this.game = game;
        this.crystalsCollected = startingCrystals;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        guide = new Rectangle(150, 200, 64, 96);
        workbench = new Rectangle(300, 200, 80, 80);
        portalMars = new Rectangle(1800, 1800, 120, 120);

        fragments = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();

        fragments.add(new Fragment(500, 500));
        fragments.add(new Fragment(1500, 400));
        fragments.add(new Fragment(800, 1600));
        fragments.add(new Fragment(1600, 1500));

        enemies.add(new Enemy(600, 400, EnemyType.PATROL, new Vector2(1000, 400), 100f));
        enemies.add(new Enemy(1200, 1000, EnemyType.CHASER, null, 120f));
        enemies.add(new Enemy(400, 1400, EnemyType.PATROL, new Vector2(400, 1800), 110f));

        loadGame();

        playerTex = safeLoadTexture("player.png");
        playerGunTex = safeLoadTexture("player_gun.png");
        benchTex = safeLoadTexture("bancada.png");
        guideTex = safeLoadTexture("cientista.png");
        bgTex = safeLoadTexture("fundo_lua.png");
        fragmentTex = safeLoadTexture("fragmento.png");
        enemyTex = safeLoadTexture("alien_lua.png");

        if (!hasGun && crystalsCollected == 0) {
            dialogIndex = 0;
            dialogState = DialogState.OPEN;
        }
    }

    private Texture safeLoadTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) return new Texture(path);
        } catch (Exception ignored) {}
        return null;
    }

    private void loadGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        this.o2 = prefs.getFloat("o2", 100f);
        this.energy = prefs.getFloat("energy", 100f);
        this.ammo = prefs.getInteger("ammo", 10);
        this.hasGun = prefs.getBoolean("hasGun", false);
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);
        prefs.putBoolean("hasGun", hasGun);
        prefs.putString("currentStage", "LUA");
        prefs.flush();
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (crystalsCollected >= TOTAL_CRYSTALS && hasGun) {
            shapeRenderer.setColor(Color.RED);
        } else {
            shapeRenderer.setColor(Color.GRAY);
        }
        shapeRenderer.rect(portalMars.x, portalMars.y, portalMars.width, portalMars.height);

        if (benchTex == null) {
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(workbench.x, workbench.y, workbench.width, workbench.height);
        }

        if (guideTex == null) {
            shapeRenderer.setColor(Color.MAGENTA);
            shapeRenderer.rect(guide.x, guide.y, guide.width, guide.height);
        }

        shapeRenderer.setColor(Color.PURPLE);
        for (Enemy e : enemies) {
            if (enemyTex == null) {
                shapeRenderer.rect(e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        if (playerTex == null) {
            shapeRenderer.setColor(Color.CYAN);
            shapeRenderer.rect(player.x, player.y, player.width, player.height);
        }

        shapeRenderer.setColor(Color.GREEN);
        for (Fragment f : fragments) {
            if (!f.collected && fragmentTex == null) {
                shapeRenderer.rect(f.rect.x, f.rect.y, f.rect.width, f.rect.height);
            }
        }

        shapeRenderer.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            shapeRenderer.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (benchTex != null) {
            batch.draw(benchTex, workbench.x, workbench.y, workbench.width, workbench.height);
        }
        font.draw(batch, "BANCADA [E]", workbench.x - 5, workbench.y + workbench.height + 15);

        if (guideTex != null) {
            batch.draw(guideTex, guide.x, guide.y, guide.width, guide.height);
        }
        font.draw(batch, "GUIA [E]", guide.x - 5, guide.y + guide.height + 15);

        if (crystalsCollected >= TOTAL_CRYSTALS && hasGun) {
            font.draw(batch, "PORTAL PARA MARTE [LIBERADO]", portalMars.x - 20, portalMars.y - 10);
        } else {
            font.draw(batch, "PORTAL BLOQUEADO", portalMars.x - 5, portalMars.y - 10);
            font.draw(batch, "(Requer: 4 Fragmentos + Arma)", portalMars.x - 25, portalMars.y - 30);
        }

        Texture currentTex = hasGun ? playerGunTex : playerTex;
        if (currentTex != null) {
            batch.draw(currentTex, player.x, player.y, player.width, player.height);
        }

        for (Enemy e : enemies) {
            if (enemyTex != null) batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
        }

        for (Fragment f : fragments) {
            if (!f.collected && fragmentTex != null) {
                batch.draw(fragmentTex, f.rect.x, f.rect.y, f.rect.width, f.rect.height);
            }
        }

        batch.end();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: LUA | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "Fragmentos Coletados: " + crystalsCollected + "/" + TOTAL_CRYSTALS, 20, Gdx.graphics.getHeight() - 40);

        String currentObjective = !QuestTracker.getInstance().isWeaponBuilt() ?
            "Objetivo: Colete 4 Fragmentos e use a Bancada." :
            "Objetivo: Entre no Portal para Marte.";
        font.setColor(Color.YELLOW);
        font.draw(batch, currentObjective, 20, Gdx.graphics.getHeight() - 65);
        font.setColor(Color.WHITE);

        batch.end();

        if (dialogState == DialogState.OPEN) {
            renderDialogBox();
        }
    }

    private void renderDialogBox() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.85f);
        shapeRenderer.rect(50, 20, Gdx.graphics.getWidth() - 100, 120);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, dialogLines[dialogIndex], 70, 110);
        font.setColor(Color.YELLOW);
        font.draw(batch, "[Pressione ESPACO para continuar]", Gdx.graphics.getWidth() - 320, 45);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void update(float delta) {
        if (o2 <= 0 || energy <= 0) {
            game.setScreen(new GameOverScreen(game));
            return;
        }

        if (dialogState == DialogState.OPEN) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                dialogIndex++;
                if (dialogIndex >= dialogLines.length) dialogState = DialogState.FINISHED;
            }
            return;
        }

        if (dialogState == DialogState.FINISHED) {
            saveGame();
            dialogState = DialogState.CLOSED;
        }

        if (player.overlaps(guide) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            dialogLines = new String[]{
                "GUIA: Lembre-se: explore o mapa e colete os 4 fragmentos.",
                "GUIA: Traga-os para a bancada logo ao lado para criarmos sua arma."
            };
            dialogIndex = 0;
            dialogState = DialogState.OPEN;
            return;
        }

        if (player.overlaps(workbench) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (crystalsCollected >= TOTAL_CRYSTALS) {
                hasGun = true;
                QuestTracker.getInstance().setWeaponBuilt(true);
                dialogLines = new String[]{
                    "BANCADA: Arma construída com sucesso!",
                    "BANCADA: Suprimentos totalmente restaurados na base."
                };
            } else {
                int faltando = TOTAL_CRYSTALS - crystalsCollected;
                dialogLines = new String[]{
                    "BANCADA: Base da Lua - Suprimentos recarregados!",
                    "BANCADA: Faltam " + faltando + " fragmentos para fabricar a arma."
                };
            }
            o2 = 100f;
            energy = 100f;

            dialogIndex = 0;
            dialogState = DialogState.OPEN;
            saveGame();
            return;
        }

        // Portal para Marte (Mantém O2 em 100%)
        if (player.overlaps(portalMars)) {
            if (crystalsCollected >= TOTAL_CRYSTALS && hasGun) {
                o2 = 100f;
                energy = energy * 0.4f;
                saveGame();
                game.setScreen(new MarsScreen(game));
                return;
            }
        }

        if (attackCooldown > 0) attackCooldown -= delta;

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= playerSpeed * delta;

        player.x += moveX; player.y += moveY;
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
        player.y = MathUtils.clamp(player.y, 0, WORLD_HEIGHT - player.height);

        camera.position.set(
            MathUtils.clamp(player.x, camera.viewportWidth / 2f, WORLD_WIDTH - camera.viewportWidth / 2f),
            MathUtils.clamp(player.y, camera.viewportHeight / 2f, WORLD_HEIGHT - camera.viewportHeight / 2f), 0
        );

        o2 -= 1.0f * delta;
        energy -= 0.8f * delta;

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 10f * delta;
                energy -= 10f * delta;
            }
        }

        if (hasGun && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && attackCooldown <= 0 && ammo > 0) {
            ammo--;
            attackCooldown = COOLDOWN_TIME;

            Vector3 mouseWorldPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouseWorldPos);

            float spawnX = player.x + player.width / 2f;
            float spawnY = player.y + player.height / 2f;

            Vector2 dir = new Vector2(mouseWorldPos.x - spawnX, mouseWorldPos.y - spawnY);
            bullets.add(new Bullet(spawnX, spawnY, dir, 800f));
        }

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(delta);

            if (b.rect.x < 0 || b.rect.x > WORLD_WIDTH || b.rect.y < 0 || b.rect.y > WORLD_HEIGHT) {
                bullets.removeIndex(i);
                continue;
            }

            for (int j = enemies.size - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (b.rect.overlaps(e.rect)) {
                    e.health--;
                    bullets.removeIndex(i);
                    if (e.health <= 0) {
                        enemies.removeIndex(j);
                    }
                    break;
                }
            }
        }

        for (Fragment f : fragments) {
            if (!f.collected && player.overlaps(f.rect)) {
                f.collected = true;
                crystalsCollected++;
            }
        }
    }

    @Override public void show() {} @Override public void resize(int w, int h) {}
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        if (playerTex != null) playerTex.dispose();
        if (playerGunTex != null) playerGunTex.dispose();
        if (benchTex != null) benchTex.dispose();
        if (guideTex != null) guideTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (fragmentTex != null) fragmentTex.dispose();
        if (bgTex != null) bgTex.dispose();
    }
}
