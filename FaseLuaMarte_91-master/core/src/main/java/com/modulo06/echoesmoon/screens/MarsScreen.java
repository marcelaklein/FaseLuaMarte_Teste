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

public class MarsScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, bossTex, crystalTex, bgTex, scientistTex, ammoTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle scientist;
    private Rectangle portalTitan;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;
    private int crystalsCollected = 0;
    private final int TOTAL_CRYSTALS = 4;

    private int ammo = 10;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.5f;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;

    private String[] dialogLines = {
        "CIENTISTA: Conseguimos chegar a Marte!",
        "CIENTISTA: Fique atento, os alienígenas daqui são perigosos e há um Chefão na área.",
        "CIENTISTA: Colete os cristais espalhados para conseguirmos avançar!"
    };
    private int dialogIndex = 0;
    private boolean talkedToScientist = false;
    private boolean crystalsCompletedDialogShown = false;

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

    class Crystal {
        Rectangle rect;
        boolean collected = false;
        public Crystal(float x, float y) {
            this.rect = new Rectangle(x, y, 48, 48);
        }
    }

    class AmmoPickup {
        Rectangle rect;
        int amount;
        boolean collected = false;

        public AmmoPickup(float x, float y, int amount) {
            this.rect = new Rectangle(x, y, 40, 40);
            this.amount = amount;
        }
    }

    enum EnemyType { PATROL, CHASER, BOSS }

    class Enemy {
        Rectangle rect;
        float speed;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;
        int health;
        boolean dropsCrystal;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB, int health, float speed, boolean dropsCrystal, float width, float height) {
            this.rect = new Rectangle(x, y, width, height);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 250, y);
            this.health = health;
            this.speed = speed;
            this.dropsCrystal = dropsCrystal;
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER || type == EnemyType.BOSS) {
                float dist = playerPos.dst(rect.x, rect.y);
                if (dist < 700f) {
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

    private Array<Crystal> crystals;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;

    public MarsScreen(Game game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        scientist = new Rectangle(320, 200, 64, 96);

        // Portal posicionado logo atrás do cientista (ex: logo acima dele)
        portalTitan = new Rectangle(320, 320, 120, 120);

        crystals = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();
        ammoPickups = new Array<>();

        ammoPickups.add(new AmmoPickup(500, 300, 10));
        ammoPickups.add(new AmmoPickup(1000, 800, 10));
        ammoPickups.add(new AmmoPickup(1600, 400, 10));
        ammoPickups.add(new AmmoPickup(700, 1600, 10));

        enemies.add(new Enemy(600, 600, EnemyType.CHASER, null, 2, 140f, true, 64, 64));
        enemies.add(new Enemy(1400, 800, EnemyType.PATROL, new Vector2(1700, 800), 2, 130f, true, 64, 64));
        enemies.add(new Enemy(800, 1500, EnemyType.CHASER, null, 2, 140f, true, 64, 64));
        enemies.add(new Enemy(1500, 1500, EnemyType.BOSS, null, 10, 110f, true, 128, 128));

        enemies.add(new Enemy(400, 1000, EnemyType.PATROL, new Vector2(800, 1000), 2, 120f, false, 64, 64));
        enemies.add(new Enemy(1100, 400, EnemyType.CHASER, null, 2, 130f, false, 64, 64));

        loadGame();

        playerTex = safeLoadTexture("player_gun.png");
        if (playerTex == null) playerTex = safeLoadTexture("player.png");
        scientistTex = safeLoadTexture("cientista.png");
        bgTex = safeLoadTexture("fundo_marte.png");
        crystalTex = safeLoadTexture("crystal.png");
        enemyTex = safeLoadTexture("alien_marte.png");
        bossTex = safeLoadTexture("chefao_marte.png");
        ammoTex = safeLoadTexture("ammo.png");
        if (bossTex == null) bossTex = enemyTex;
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
        this.talkedToScientist = prefs.getBoolean("talkedToScientist_marte", false);
        this.crystalsCompletedDialogShown = prefs.getBoolean("crystalsCompletedDialogShown", false);
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);
        prefs.putBoolean("talkedToScientist_marte", talkedToScientist);
        prefs.putBoolean("crystalsCompletedDialogShown", crystalsCompletedDialogShown);
        prefs.putString("currentStage", "MARTE");
        prefs.flush();
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

        if (crystalsCollected >= TOTAL_CRYSTALS) {
            shapeRenderer.setColor(Color.CYAN);
        } else {
            shapeRenderer.setColor(Color.GRAY);
        }
        shapeRenderer.rect(portalTitan.x, portalTitan.y, portalTitan.width, portalTitan.height);

        if (scientistTex == null) {
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.rect(scientist.x, scientist.y, scientist.width, scientist.height);
        }

        shapeRenderer.setColor(Color.ORANGE);
        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex == null) {
                shapeRenderer.rect(a.rect.x, a.rect.y, a.rect.width, a.rect.height);
            }
        }

        for (Enemy e : enemies) {
            if ((e.type == EnemyType.BOSS && bossTex == null) || (e.type != EnemyType.BOSS && enemyTex == null)) {
                shapeRenderer.setColor(e.type == EnemyType.BOSS ? Color.RED : Color.ORANGE);
                shapeRenderer.rect(e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        if (playerTex == null) {
            shapeRenderer.setColor(Color.CYAN);
            shapeRenderer.rect(player.x, player.y, player.width, player.height);
        }

        shapeRenderer.setColor(Color.YELLOW);
        for (Crystal c : crystals) {
            if (!c.collected && crystalTex == null) {
                shapeRenderer.rect(c.rect.x, c.rect.y, c.rect.width, c.rect.height);
            }
        }

        for (Bullet b : bullets) {
            shapeRenderer.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (scientistTex != null) {
            batch.draw(scientistTex, scientist.x, scientist.y, scientist.width, scientist.height);
        }
        font.draw(batch, "CIENTISTA [E]", scientist.x - 5, scientist.y + scientist.height + 15);

        if (crystalsCollected >= TOTAL_CRYSTALS) {
            font.draw(batch, "PORTAL PARA TITÃ [ATIVO]", portalTitan.x - 10, portalTitan.y + portalTitan.height + 15);
        } else {
            font.draw(batch, "PORTAL INATIVO", portalTitan.x + 5, portalTitan.y + portalTitan.height + 15);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) {
                batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
            }
        }

        if (playerTex != null) {
            batch.draw(playerTex, player.x, player.y, player.width, player.height);
        }

        for (Enemy e : enemies) {
            if (e.type == EnemyType.BOSS && bossTex != null) {
                batch.draw(bossTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
                font.draw(batch, "CHEFÃO MARTE (HP: " + e.health + ")", e.rect.x - 10, e.rect.y + e.rect.height + 20);
            } else if (enemyTex != null) {
                batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        for (Crystal c : crystals) {
            if (!c.collected && crystalTex != null) {
                batch.draw(crystalTex, c.rect.x, c.rect.y, c.rect.width, c.rect.height);
            }
        }
        batch.end();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: MARTE | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "Cristais de Marte Coletados: " + crystalsCollected + "/" + TOTAL_CRYSTALS, 20, Gdx.graphics.getHeight() - 40);
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
            talkedToScientist = true;
            saveGame();
            dialogState = DialogState.CLOSED;
        }

        // Interação manual com o cientista
        if (player.overlaps(scientist) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (crystalsCollected >= TOTAL_CRYSTALS) {
                dialogLines = new String[]{
                    "CIENTISTA: Você conseguiu todos os cristais!",
                    "CIENTISTA: Entre no portal logo atrás de mim para sairmos daqui!"
                };
            } else {
                int faltando = TOTAL_CRYSTALS - crystalsCollected;
                dialogLines = new String[]{
                    "CIENTISTA: Ainda faltam " + faltando + " cristais.",
                    "CIENTISTA: Continue procurando pelo mapa!"
                };
            }
            dialogState = DialogState.OPEN;
            dialogIndex = 0;
            return;
        }

        // Se coletou todos os cristais pela primeira vez, dispara o aviso automático do cientista
        if (crystalsCollected >= TOTAL_CRYSTALS && !crystalsCompletedDialogShown) {
            crystalsCompletedDialogShown = true;
            dialogLines = new String[]{
                "CIENTISTA: Excelente! Você coletou todos os cristais de Marte.",
                "CIENTISTA: O portal atrás de mim está aberto. Entre nele para continuarmos!"
            };
            dialogIndex = 0;
            dialogState = DialogState.OPEN;
            saveGame();
        }

        // Entrar no portal para Titã
        if (player.overlaps(portalTitan)) {
            if (crystalsCollected >= TOTAL_CRYSTALS) {
                o2 = o2 * 0.4f;
                energy = energy * 0.4f;
                saveGame();
                game.setScreen(new TitanScreen(game));
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

        o2 -= 1.2f * delta;
        energy -= 1.0f * delta;

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && player.overlaps(a.rect)) {
                a.collected = true;
                ammo = Math.min(maxAmmo, ammo + a.amount);
                saveGame();
            }
        }

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 15f * delta;
                energy -= 15f * delta;
            }
        }

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && attackCooldown <= 0 && ammo > 0) {
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
                        if (e.dropsCrystal) {
                            crystals.add(new Crystal(e.rect.x + e.rect.width / 4f, e.rect.y + e.rect.height / 4f));
                        }
                        enemies.removeIndex(j);
                    }
                    break;
                }
            }
        }

        for (Crystal c : crystals) {
            if (!c.collected && player.overlaps(c.rect)) {
                c.collected = true;
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
        if (scientistTex != null) scientistTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (bossTex != null) bossTex.dispose();
        if (crystalTex != null) crystalTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
