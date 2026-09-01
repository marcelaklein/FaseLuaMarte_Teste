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

public class GameScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, baseTex, portalTex, itemTex, bgTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle base;
    private Rectangle portal;
    private float playerSpeed = 300f;
    private float gameTimer = 300.0f;

    private float o2 = 100f, maxO2 = 100f, o2ConsumptionRate = 1.0f;
    private float energy = 100f, maxEnergy = 100f, energyConsumptionRate = 1.0f;

    private int ammo = 20;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.8f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private Rectangle attackBox;

    private int crystalsFromMars = 0;
    private final int TOTAL_CRYSTALS_NEEDED = 4;
    private int weaponFragmentsCollected = 0;
    private final int TOTAL_FRAGMENTS_NEEDED = 3;
    private boolean hasWeapon = false;

    enum EnemyType { PATROL, CHASER }

    class Enemy {
        Rectangle rect;
        float speed = 135f;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB) {
            this.rect = new Rectangle(x, y, 64, 64);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 200, y);
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER) {
                float dist = playerPos.dst(rect.x, rect.y);
                if (dist < 600f) {
                    Vector2 dir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
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

    enum ItemType { O2, FOOD, WEAPON_FRAGMENT, AMMO }

    class Item {
        Rectangle rect;
        ItemType type;
        boolean collected = false;
        Color color;

        public Item(float x, float y, ItemType type) {
            this.rect = new Rectangle(x, y, 64, 64);
            this.type = type;
            if (type == ItemType.O2) color = Color.CYAN;
            else if (type == ItemType.FOOD) color = Color.GREEN;
            else if (type == ItemType.AMMO) color = Color.ORANGE;
            else color = Color.YELLOW;
        }
    }

    private Array<Item> items;
    private Array<Enemy> enemies;

    public GameScreen(Game game) {
        this(game, 0);
    }

    public GameScreen(Game game, int crystalsFromMars) {
        this.game = game;
        this.crystalsFromMars = crystalsFromMars;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        base = new Rectangle(WORLD_WIDTH / 2 - 100, WORLD_HEIGHT / 2 - 100, 200, 150);
        portal = new Rectangle(WORLD_WIDTH - 250, WORLD_HEIGHT - 250, 120, 120);
        attackBox = new Rectangle(0, 0, 120, 120);

        if (this.crystalsFromMars >= TOTAL_CRYSTALS_NEEDED) {
            player = new Rectangle(portal.x - 100, portal.y - 100, 64, 96);
        } else {
            player = new Rectangle(base.x + 68, base.y - 110, 64, 96);
        }

        loadGame();

        playerTex = safeLoadTexture("player_lunar.png");
        if (playerTex == null) playerTex = safeLoadTexture("player.png");

        enemyTex = safeLoadTexture("alien_lunar.png");
        if (enemyTex == null) enemyTex = safeLoadTexture("enemy.png");

        baseTex = safeLoadTexture("base_lua.png");
        if (baseTex == null) baseTex = safeLoadTexture("base.png");

        portalTex = safeLoadTexture("portal.png");
        itemTex = safeLoadTexture("item.png");
        bgTex = safeLoadTexture("fundo.png");

        enemies = new Array<>();
        enemies.add(new Enemy(WORLD_WIDTH / 2 + 300, WORLD_HEIGHT / 2 + 200, EnemyType.PATROL, new Vector2(WORLD_WIDTH / 2 + 700, WORLD_HEIGHT / 2 + 200)));
        enemies.add(new Enemy(WORLD_WIDTH / 2 - 500, WORLD_HEIGHT / 2 - 400, EnemyType.CHASER, null));

        items = new Array<>();
        if (this.crystalsFromMars < TOTAL_CRYSTALS_NEEDED) {
            items.add(new Item(WORLD_WIDTH / 2 + 300, WORLD_HEIGHT / 2 + 300, ItemType.WEAPON_FRAGMENT));
            items.add(new Item(WORLD_WIDTH / 2 - 400, WORLD_HEIGHT / 2 + 300, ItemType.WEAPON_FRAGMENT));
            items.add(new Item(WORLD_WIDTH / 2 - 300, WORLD_HEIGHT / 2 - 300, ItemType.WEAPON_FRAGMENT));
        }

        items.add(new Item(WORLD_WIDTH / 2 + 100, WORLD_HEIGHT / 2 - 200, ItemType.O2));
        items.add(new Item(WORLD_WIDTH / 2 - 100, WORLD_HEIGHT / 2 + 200, ItemType.FOOD));
        items.add(new Item(WORLD_WIDTH / 2 + 400, WORLD_HEIGHT / 2 - 400, ItemType.AMMO));
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);
        prefs.putInteger("fragments", weaponFragmentsCollected);
        prefs.putBoolean("hasWeapon", hasWeapon);
        prefs.putInteger("crystals", crystalsFromMars);
        prefs.putString("currentStage", "LUA");
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
            this.weaponFragmentsCollected = prefs.getInteger("fragments", 0);
            this.hasWeapon = prefs.getBoolean("hasWeapon", false);
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

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (portalTex == null) {
            shapeRenderer.setColor((hasWeapon || crystalsFromMars >= TOTAL_CRYSTALS_NEEDED) ? Color.PURPLE : Color.GRAY);
            shapeRenderer.rect(portal.x, portal.y, portal.width, portal.height);
        }

        if (baseTex == null) {
            shapeRenderer.setColor(Color.NAVY);
            shapeRenderer.rect(base.x, base.y, base.width, base.height);
        }

        if (enemyTex == null) {
            for (Enemy e : enemies) {
                shapeRenderer.setColor(e.type == EnemyType.PATROL ? Color.YELLOW : Color.RED);
                shapeRenderer.rect(e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        if (playerTex == null) {
            shapeRenderer.setColor(Color.ORANGE);
            shapeRenderer.rect(player.x, player.y, player.width, player.height);
        }

        if (isAttacking) {
            shapeRenderer.setColor(new Color(1, 0.8f, 0, 0.5f));
            shapeRenderer.rect(attackBox.x, attackBox.y, attackBox.width, attackBox.height);
        }

        shapeRenderer.end();

        batch.begin();
        if (portalTex != null) batch.draw(portalTex, portal.x, portal.y, portal.width, portal.height);
        if (baseTex != null) batch.draw(baseTex, base.x, base.y, base.width, base.height);

        if (itemTex != null) {
            for (Item item : items) {
                if (!item.collected) batch.draw(itemTex, item.rect.x, item.rect.y, item.rect.width, item.rect.height);
            }
        }

        if (enemyTex != null) {
            for (Enemy e : enemies) batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);
        batch.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, "BASE LUNAR", base.x + 10, base.y - 10);
        boolean canEnterPortal = (hasWeapon || crystalsFromMars >= TOTAL_CRYSTALS_NEEDED);
        font.draw(batch, canEnterPortal ? "PORTAL P/ MARTE [ABERTO]" : "PORTAL [BLOQUEADO]", portal.x - 20, portal.y - 10);
        batch.end();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.draw(batch, "FASE: LUA | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo + " [ESPACO Ataca] | [ESC] Pausa", 10, Gdx.graphics.getHeight() - 10);

        int hudY = Gdx.graphics.getHeight() - 40;
        if (crystalsFromMars < TOTAL_CRYSTALS_NEEDED) {
            font.draw(batch, "=== LUA PARTE 1 ===", 10, hudY); hudY -= 20;
            font.draw(batch, "- Coleta todos os fragmentos", 10, hudY); hudY -= 18;
            font.draw(batch, "- Leve-os ate a base [Pressione E]", 10, hudY); hudY -= 18;
            font.draw(batch, "- Va ate o portal", 10, hudY);
        } else {
            font.draw(batch, "=== LUA PARTE 2: RETORNO ===", 10, hudY); hudY -= 20;
            font.draw(batch, "Volte a base com os cristais (" + crystalsFromMars + "/" + TOTAL_CRYSTALS_NEEDED + ")", 10, hudY);
        }
        batch.end();
    }

    private void update(float delta) {
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

        boolean inBase = player.overlaps(base);

        if (inBase) {
            o2 = Math.min(maxO2, o2 + 15f * delta);
            energy = Math.min(maxEnergy, energy + 15f * delta);
            saveGame();

            if (crystalsFromMars < TOTAL_CRYSTALS_NEEDED) {
                if (weaponFragmentsCollected >= TOTAL_FRAGMENTS_NEEDED && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    hasWeapon = true;
                    saveGame();
                }
            } else {
                game.setScreen(new VictoryScreen(game));
                return;
            }
        } else {
            o2 -= o2ConsumptionRate * delta;
            energy -= energyConsumptionRate * delta;
        }

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 12 * delta;
                energy -= 12 * delta;
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
            attackBox.set(player.x - 30, player.y - 30, player.width + 60, player.height + 60);

            for (int i = enemies.size - 1; i >= 0; i--) {
                Enemy e = enemies.get(i);
                if (attackBox.overlaps(e.rect)) {
                    enemies.removeIndex(i);
                }
            }
        }

        for (Item item : items) {
            if (!item.collected && player.overlaps(item.rect)) {
                item.collected = true;
                if (item.type == ItemType.O2) o2 = Math.min(maxO2, o2 + 30);
                else if (item.type == ItemType.FOOD) energy = Math.min(maxEnergy, energy + 30);
                else if (item.type == ItemType.AMMO) ammo = Math.min(maxAmmo, ammo + 10);
                else if (item.type == ItemType.WEAPON_FRAGMENT) weaponFragmentsCollected++;
            }
        }

        if (player.overlaps(portal) && hasWeapon && crystalsFromMars < TOTAL_CRYSTALS_NEEDED) {
            saveGame();
            game.setScreen(new MarsScreen(game));
        }
    }

    @Override public void show() {} @Override public void resize(int w, int h) {}
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        if (playerTex != null) playerTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (baseTex != null) baseTex.dispose();
        if (portalTex != null) portalTex.dispose();
        if (itemTex != null) itemTex.dispose();
        if (bgTex != null) bgTex.dispose();
    }
}
