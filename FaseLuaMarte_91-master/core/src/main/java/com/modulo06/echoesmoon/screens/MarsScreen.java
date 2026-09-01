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
import com.badlogic.gdx.utils.Array;

public class MarsScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, bgTex, itemTex, crystalTex, portalTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle portalLua;
    private float playerSpeed = 300f;
    private float gameTimer = 300.0f;

    private float o2 = 100f, maxO2 = 100f, o2ConsumptionRate = 4f;
    private float energy = 100f, maxEnergy = 100f, energyConsumptionRate = 3f;

    private int totalEnemiesInitial = 4;
    private int enemiesKilled = 0;
    private int crystalsCollected = 0;
    private final int TOTAL_CRYSTALS = 4;

    private int ammo = 20;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.8f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private Rectangle attackBox;

    private QuestTrackerUI questTracker;

    enum EnemyType { PATROL, CHASER }

    class Enemy {
        Rectangle rect;
        float speed = 145f;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB) {
            this.rect = new Rectangle(x, y, 64, 64);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 250, y);
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER) {
                Vector2 dir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                rect.x += dir.x * speed * delta;
                rect.y += dir.y * speed * delta;
            } else if (type == EnemyType.PATROL) {
                Vector2 target = movingToB ? patrolB : patrolA;
                Vector2 dir = new Vector2(target.x - rect.x, target.y - rect.y);
                if (dir.len() < 10f) {
                    movingToB = !movingToB;
                } else {
                    dir.nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
            }
        }
    }

    enum ItemType { O2, FOOD, AMMO, CRYSTAL }

    class Item {
        Rectangle rect;
        ItemType type;
        boolean collected = false;
        Color color;

        public Item(float x, float y, ItemType type) {
            this.rect = new Rectangle(x, y, 54, 54);
            this.type = type;
            if (type == ItemType.O2) color = Color.CYAN;
            else if (type == ItemType.FOOD) color = Color.GREEN;
            else if (type == ItemType.AMMO) color = Color.ORANGE;
            else if (type == ItemType.CRYSTAL) color = Color.MAGENTA;
        }
    }

    private Array<Item> items;
    private Array<Enemy> enemies;

    public MarsScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        questTracker = new QuestTrackerUI("MISSAO: MARTE (CADEIA)");

        player = new Rectangle(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 64, 96);
        portalLua = new Rectangle(WORLD_WIDTH / 2 - 50, WORLD_HEIGHT / 2 - 50, 100, 100);
        attackBox = new Rectangle(0, 0, 140, 140);

        this.o2 = 100f;
        this.energy = 100f;
        this.gameTimer = 300.0f;

        loadGame();

        playerTex = safeLoadTexture("player_marte.png");
        if (playerTex == null) playerTex = safeLoadTexture("player.png");

        enemyTex = safeLoadTexture("alien_marte.png");
        if (enemyTex == null) enemyTex = safeLoadTexture("alien_lunar.png");
        if (enemyTex == null) enemyTex = safeLoadTexture("enemy.png");

        bgTex = safeLoadTexture("fundo_marte.png");
        if (bgTex == null) bgTex = safeLoadTexture("fundo.png");

        itemTex = safeLoadTexture("item.png");
        crystalTex = safeLoadTexture("cristal.png");
        if (crystalTex == null) crystalTex = safeLoadTexture("item.png");

        portalTex = safeLoadTexture("portal.png");

        enemies = new Array<>();
        enemies.add(new Enemy(WORLD_WIDTH / 2 + 400, WORLD_HEIGHT / 2 + 300, EnemyType.CHASER, null));
        enemies.add(new Enemy(WORLD_WIDTH / 2 - 400, WORLD_HEIGHT / 2 - 300, EnemyType.PATROL, new Vector2(WORLD_WIDTH / 2 - 100, WORLD_HEIGHT / 2 - 300)));
        enemies.add(new Enemy(WORLD_WIDTH / 2 + 200, WORLD_HEIGHT / 2 - 500, EnemyType.CHASER, null));
        enemies.add(new Enemy(WORLD_WIDTH / 2 - 500, WORLD_HEIGHT / 2 + 400, EnemyType.PATROL, new Vector2(WORLD_WIDTH / 2 - 500, WORLD_HEIGHT / 2 + 100)));

        totalEnemiesInitial = enemies.size;

        items = new Array<>();
        items.add(new Item(WORLD_WIDTH / 2 + 200, WORLD_HEIGHT / 2 + 100, ItemType.AMMO));
        items.add(new Item(WORLD_WIDTH / 2 - 200, WORLD_HEIGHT / 2 - 100, ItemType.O2));
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);
        prefs.putInteger("crystals", crystalsCollected);
        prefs.putString("currentStage", "MARTE");
        prefs.flush();
    }

    private void loadGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        if (prefs.contains("o2")) {
            float savedO2 = prefs.getFloat("o2", 100f);
            if (savedO2 > 0) this.o2 = savedO2;

            float savedEnergy = prefs.getFloat("energy", 100f);
            if (savedEnergy > 0) this.energy = savedEnergy;

            this.ammo = prefs.getInteger("ammo", 20);
        }
    }

    private Texture safeLoadTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) return new Texture(path);
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.2f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (portalTex == null) {
            shapeRenderer.setColor(Color.PURPLE);
            shapeRenderer.rect(portalLua.x, portalLua.y, portalLua.width, portalLua.height);
        }

        if (enemyTex == null) {
            for (Enemy e : enemies) {
                shapeRenderer.setColor(e.type == EnemyType.PATROL ? Color.YELLOW : Color.RED);
                shapeRenderer.rect(e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        if (playerTex == null) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(player.x, player.y, player.width, player.height);
        }

        if (isAttacking) {
            shapeRenderer.setColor(new Color(1, 0.5f, 0, 0.5f));
            shapeRenderer.rect(attackBox.x, attackBox.y, attackBox.width, attackBox.height);
        }

        shapeRenderer.end();

        batch.begin();
        if (portalTex != null) batch.draw(portalTex, portalLua.x, portalLua.y, portalLua.width, portalLua.height);

        for (Item item : items) {
            if (!item.collected) {
                if (item.type == ItemType.CRYSTAL && crystalTex != null) {
                    batch.draw(crystalTex, item.rect.x, item.rect.y, item.rect.width, item.rect.height);
                } else if (item.type != ItemType.CRYSTAL && itemTex != null) {
                    batch.draw(itemTex, item.rect.x, item.rect.y, item.rect.width, item.rect.height);
                }
            }
        }

        if (enemyTex != null) {
            for (Enemy e : enemies) {
                batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);
        batch.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, "PORTAL DE VOLTA P/ A LUA", portalLua.x - 30, portalLua.y - 10);
        batch.end();

        Array<QuestTrackerUI.QuestStep> steps = new Array<>();
        steps.add(new QuestTrackerUI.QuestStep("Derrotar alienigenas (" + enemiesKilled + "/" + totalEnemiesInitial + ")", enemiesKilled >= totalEnemiesInitial));
        steps.add(new QuestTrackerUI.QuestStep("Coletar cristais (" + crystalsCollected + "/" + TOTAL_CRYSTALS + ")", crystalsCollected >= TOTAL_CRYSTALS));
        steps.add(new QuestTrackerUI.QuestStep("Retornar pelo portal", crystalsCollected >= TOTAL_CRYSTALS));
        questTracker.setSteps(steps);

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.draw(batch, "FASE: MARTE | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo + " [ESPACO Ataca] | [ESC] Pausa", 10, Gdx.graphics.getHeight() - 10);
        batch.end();

        questTracker.render(shapeRenderer, batch, font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void update(float delta) {
        // Pausar o jogo e abrir o menu de pausa ao pressionar ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new PauseMenuScreen(game, this));
            return;
        }

        if (gameTimer <= 0 || o2 <= 0 || energy <= 0) {
            game.setScreen(new GameOverScreen(game));
            return;
        }

        gameTimer -= delta;

        if (attackCooldown > 0) {
            attackCooldown -= delta;
            if (attackCooldown < 0) attackCooldown = 0;
        }

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= playerSpeed * delta;

        player.x += moveX; player.y += moveY;
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
        player.y = MathUtils.clamp(player.y, 0, WORLD_HEIGHT - player.height);

        camera.position.set(MathUtils.clamp(player.x, camera.viewportWidth / 2f, WORLD_WIDTH - camera.viewportWidth / 2f),
            MathUtils.clamp(player.y, camera.viewportHeight / 2f, WORLD_HEIGHT - camera.viewportHeight / 2f), 0);

        o2 -= o2ConsumptionRate * delta;
        energy -= energyConsumptionRate * delta;

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 15 * delta;
                energy -= 15 * delta;
            }
        }

        if (isAttacking) {
            attackTimer -= delta;
            if (attackTimer <= 0) isAttacking = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && attackCooldown <= 0 && ammo > 0) {
            ammo--;
            attackCooldown = COOLDOWN_TIME;
            isAttacking = true;
            attackTimer = 0.2f;
            attackBox.set(player.x - 40, player.y - 40, player.width + 80, player.height + 80);

            for (int i = enemies.size - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                if (attackBox.overlaps(e.rect)) {
                    enemiesKilled++;
                    items.add(new Item(e.rect.x, e.rect.y, ItemType.CRYSTAL));
                    enemies.removeIndex(i);
                }
            }
        }

        for (Item item : items) {
            if (!item.collected && player.overlaps(item.rect)) {
                item.collected = true;
                if (item.type == ItemType.O2) o2 = Math.min(maxO2, o2 + 30);
                else if (item.type == ItemType.FOOD) energy = Math.min(maxEnergy, energy + 30);
                else if (item.type == ItemType.AMMO) ammo = Math.min(maxAmmo, ammo + 5);
                else if (item.type == ItemType.CRYSTAL) crystalsCollected++;
            }
        }

        if (player.overlaps(portalLua) && crystalsCollected >= TOTAL_CRYSTALS) {
            saveGame();
            game.setScreen(new GameScreen(game, crystalsCollected));
        }
    }

    @Override public void show() {} @Override public void resize(int w, int h) {}
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        if (playerTex != null) playerTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (itemTex != null) itemTex.dispose();
        if (crystalTex != null) crystalTex.dispose();
        if (portalTex != null) portalTex.dispose();
    }
}
