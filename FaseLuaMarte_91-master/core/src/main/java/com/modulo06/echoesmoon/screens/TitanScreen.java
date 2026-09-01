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

public class TitanScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, partTex, bgTex, scientistTex, ammoTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle scientist;
    private Rectangle portalMoon;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;

    private int partsCollected = 0;
    private final int TOTAL_PARTS = 4;
    private boolean portalCreated = false;

    private int ammo = 15;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.4f;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;
    private String[] dialogLines;
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

    class Part {
        Rectangle rect;
        boolean collected = false;
        public Part(float x, float y) {
            this.rect = new Rectangle(x, y, 40, 40);
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

    enum EnemyType { PATROL, CHASER }

    class Enemy {
        Rectangle rect;
        float speed;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;
        int health;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB, int health, float speed) {
            this.rect = new Rectangle(x, y, 64, 64);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 200, y);
            this.health = health;
            this.speed = speed;
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER) {
                float dist = playerPos.dst(rect.x, rect.y);
                if (dist < 600f) {
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

    private Array<Part> parts;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;

    public TitanScreen(Game game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        scientist = new Rectangle(300, 200, 64, 96);
        portalMoon = new Rectangle(1800, 1800, 120, 120);

        parts = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();
        ammoPickups = new Array<>();

        parts.add(new Part(500, 400));
        parts.add(new Part(1300, 300));
        parts.add(new Part(700, 1400));
        parts.add(new Part(1600, 1200));

        ammoPickups.add(new AmmoPickup(400, 600, 10));
        ammoPickups.add(new AmmoPickup(1200, 900, 10));

        enemies.add(new Enemy(600, 600, EnemyType.CHASER, null, 3, 130f));
        enemies.add(new Enemy(1400, 500, EnemyType.PATROL, new Vector2(1700, 500), 3, 120f));
        enemies.add(new Enemy(1000, 1300, EnemyType.CHASER, null, 3, 140f));

        loadGame();

        playerTex = safeLoadTexture("player_gun.png");
        scientistTex = safeLoadTexture("cientista.png");
        bgTex = safeLoadTexture("fundo_tita.png");
        partTex = safeLoadTexture("peca.png");
        enemyTex = safeLoadTexture("alien_tita.png");
        ammoTex = safeLoadTexture("ammo.png");

        // Diálogo inicial acionado automaticamente ao entrar na tela
        dialogLines = new String[]{
            "CIENTISTA: Opa...",
            "CIENTISTA: Parece que o portal que criei estava com coordenadas erradas.",
            "CIENTISTA: Colete alguns itens espalhados para que eu possa criar outro portal."
        };
        dialogIndex = 0;
        dialogState = DialogState.OPEN;
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
        this.ammo = prefs.getInteger("ammo", 15);
    }

    private void saveGame() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);
        prefs.putString("currentStage", "TITAN");
        prefs.flush();
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.05f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (portalCreated) {
            shapeRenderer.setColor(Color.GREEN);
        } else {
            shapeRenderer.setColor(Color.GRAY);
        }
        shapeRenderer.rect(portalMoon.x, portalMoon.y, portalMoon.width, portalMoon.height);

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

        shapeRenderer.setColor(Color.WHITE);
        for (Part p : parts) {
            if (!p.collected && partTex == null) {
                shapeRenderer.rect(p.rect.x, p.rect.y, p.rect.width, p.rect.height);
            }
        }

        shapeRenderer.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            shapeRenderer.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (scientistTex != null) batch.draw(scientistTex, scientist.x, scientist.y, scientist.width, scientist.height);
        font.draw(batch, "CIENTISTA [E]", scientist.x - 5, scientist.y + scientist.height + 15);

        if (portalCreated) {
            font.draw(batch, "PORTAL PARA LUA [LIBERADO]", portalMoon.x - 20, portalMoon.y - 10);
        } else {
            font.draw(batch, "PORTAL EM CONSTRUÇÃO", portalMoon.x - 15, portalMoon.y - 10);
            font.draw(batch, "(Entregue as pecas ao Cientista)", portalMoon.x - 30, portalMoon.y - 30);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) {
                batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
            }
        }

        for (Enemy e : enemies) {
            if (enemyTex != null) batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
        }

        for (Part p : parts) {
            if (!p.collected && partTex != null) {
                batch.draw(partTex, p.rect.x, p.rect.y, p.rect.width, p.rect.height);
            }
        }

        if (playerTex != null) {
            batch.draw(playerTex, player.x, player.y, player.width, player.height);
        }

        batch.end();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: TITÃ | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "Pecas de Portal Coletadas: " + partsCollected + "/" + TOTAL_PARTS, 20, Gdx.graphics.getHeight() - 40);
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
        if (dialogLines != null && dialogIndex < dialogLines.length) {
            font.draw(batch, dialogLines[dialogIndex], 70, 110);
        }
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
                if (dialogLines == null || dialogIndex >= dialogLines.length) {
                    dialogState = DialogState.FINISHED;
                }
            }
            return;
        }

        if (dialogState == DialogState.FINISHED) {
            saveGame();
            dialogState = DialogState.CLOSED;
        }

        // Interação manual com o Cientista para entrega das peças
        if (player.overlaps(scientist) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (partsCollected >= TOTAL_PARTS) {
                portalCreated = true;
                dialogLines = new String[]{
                    "CIENTISTA: Excelente! Você trouxe todas as 4 peças!",
                    "CIENTISTA: Consegui calibrar e ativar o portal final.",
                    "CIENTISTA: O Portal para a Lua está pronto. Vamos nessa!"
                };
            } else {
                int faltando = TOTAL_PARTS - partsCollected;
                dialogLines = new String[]{
                    "CIENTISTA: Você coletou " + partsCollected + "/" + TOTAL_PARTS + " peças.",
                    "CIENTISTA: Ainda precisamos de mais " + faltando + " peças espalhadas por Titã para abrir o portal de volta."
                };
            }
            dialogIndex = 0;
            dialogState = DialogState.OPEN;
            return;
        }

        if (player.overlaps(portalMoon)) {
            if (portalCreated) {
                saveGame();
                game.setScreen(new GameScreen(game, 4));
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

        for (Part p : parts) {
            if (!p.collected && player.overlaps(p.rect)) {
                p.collected = true;
                partsCollected++;
                saveGame();
            }
        }

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 12f * delta;
                energy -= 12f * delta;
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
                        enemies.removeIndex(j);
                    }
                    break;
                }
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
        if (partTex != null) partTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
