package gamestates;

import model.Cavalier;
import model.Enemy;
import org.json.JSONArray;
import org.json.JSONObject;
import persistence.Writable;
import ui.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;

import persistence.JsonReader;
import persistence.JsonWriter;

public class Playing extends State implements Statemethods, Writable {

    private static final String JSON_STORE = "./data/world.json";
    private PlayerUI player;
    private WorldHandler worldHandler;
    private EnemyHandler enemyHandler;
    private boolean paused = false;
    private PauseOverlay pauseOverlay;
    private GameOverOverlay gameOverOverlay;
    private GameWinOverlay gameWinOverlay;
    private JsonWriter jsonWriter;

    private int lvlOffsetX = 0;
    private int lvlOffsetY = 0;

    private int leftBorder = (int) (0.2 * Game.GAME_WIDTH);
    private int rightBorder = (int) (0.8 * Game.GAME_WIDTH);

    private int upBorder = (int) (0.2 * Game.GAME_HEIGHT);
    private int downBorder = (int) (0.8 * Game.GAME_HEIGHT);

    private int maxLvlOffsetX = 511;
    private int maxLvlOffsetY = 255;

    private boolean gameOver = false;
    private boolean gameWin = false;

    public Playing(Game game) {
        super(game);
        initClasses();
    }

    private void initClasses() {
        worldHandler = new WorldHandler(game);
        player = new PlayerUI(Game.GAME_WIDTH / 2,Game.GAME_HEIGHT / 2, this);
        enemyHandler = new EnemyHandler(this);
        pauseOverlay = new PauseOverlay(this);
        gameOverOverlay = new GameOverOverlay(this);
        gameWinOverlay = new GameWinOverlay(this);
        jsonWriter = new JsonWriter(JSON_STORE);
    }

    @Override
    public void update() {
        if (!paused && !gameOver && !gameWin) {
            worldHandler.update();
            player.update();
            //System.out.println("Num Enemies: " + game.getNumEnemies());
            enemyHandler.update(player, lvlOffsetX, lvlOffsetY, game.getNumEnemies());
            checkCloseToBorderX();
            checkCloseToBorderY();
        } else {
            pauseOverlay.update();
        }
    }

    private void checkCloseToBorderX() {
        int playerX = (int) player.getHitbox().x;
        int diff = playerX - lvlOffsetX;

        if (diff > rightBorder) {
            lvlOffsetX += diff - rightBorder;
        } else if (diff < leftBorder) {
            lvlOffsetX += diff - leftBorder;
        }

        if (lvlOffsetX > maxLvlOffsetX) {
            lvlOffsetX = maxLvlOffsetX;
        } else if (lvlOffsetX < 0) {
            lvlOffsetX = 0;
        }
    }

    private void checkCloseToBorderY() {
        int playerY = (int) player.getHitbox().y;
        int diff = playerY - lvlOffsetY;

        if (diff > downBorder) {
            lvlOffsetY += diff - downBorder;
        } else if (diff < upBorder) {
            lvlOffsetY += diff - upBorder;
        }

        if (lvlOffsetY > maxLvlOffsetY) {
            lvlOffsetY = maxLvlOffsetY;
        } else if (lvlOffsetY < 0) {
            lvlOffsetY = 0;
        }
    }

    @Override
    public void draw(Graphics g) {
        worldHandler.draw(g, lvlOffsetX, lvlOffsetY);
        player.render(g, lvlOffsetX, lvlOffsetY);
        enemyHandler.draw(g, lvlOffsetX, lvlOffsetY);

        if (paused) {
            g.setColor(new Color(0,0,0,150));
            g.fillRect(0,0,Game.GAME_WIDTH,Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        } else if (gameOver) {
            gameOverOverlay.draw(g);
        } else if (gameWin) {
            gameWinOverlay.draw(g);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!gameOver && !gameWin) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                player.setAttacking(true);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameOver && !gameWin) {
            if (paused) {
                pauseOverlay.mousePressed(e);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!gameOver && !gameWin) {
            if (paused) {
                pauseOverlay.mouseReleased(e);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!gameOver && !gameWin) {
            if (paused) {
                pauseOverlay.mouseMoved(e);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            gameOverOverlay.keyPressed(e);
        } else if (gameWin) {
            gameWinOverlay.keyPressed(e);
        } else {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    player.setUp(true);
                    break;
                case KeyEvent.VK_S:
                    player.setDown(true);
                    break;
                case KeyEvent.VK_A:
                    player.setLeft(true);
                    break;
                case KeyEvent.VK_D:
                    player.setRight(true);
                    break;
                case KeyEvent.VK_ESCAPE:
                    paused = !paused;
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameOver && !gameWin) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    player.setUp(false);
                    break;
                case KeyEvent.VK_S:
                    player.setDown(false);
                    break;
                case KeyEvent.VK_A:
                    player.setLeft(false);
                    break;
                case KeyEvent.VK_D:
                    player.setRight(false);
                    break;
            }
        }
    }

    public void unpauseGame() {
        paused = false;
    }

    public PlayerUI getPlayer() {
        return this.player;
    }

    public void resetAll() {
        gameOver = false;
        gameWin = false;
        paused = false;
        player.resetAll();
        enemyHandler.resetAllEnemies();
    }

    public void checkEnemyHit(Rectangle2D.Float attackBox, int strength) {
        enemyHandler.checkEnemyHit(attackBox, lvlOffsetX, lvlOffsetY, strength);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void setGameWin(boolean gameWin) {
        this.gameWin = gameWin;
    }

    public void setPlayer(PlayerUI player) {
        this.player = player;
    }

    public EnemyHandler getEnemyHandler() {
        return enemyHandler;
    }

    public void setEnemyHandler(EnemyHandler enemyHandler) {
        this.enemyHandler = enemyHandler;
    }

    public void setxLvlOffset(int lvlOffsetX) {
        this.lvlOffsetX = lvlOffsetX;
    }

    public void setyLvlOffset(int lvlOffsetY) {
        this.lvlOffsetY = lvlOffsetY;
    }

    public void saveGame() {
        try {
            jsonWriter.open();
            jsonWriter.write(this);
            jsonWriter.close();
            System.out.println("Saved to " + JSON_STORE);
        } catch (FileNotFoundException e) {
            System.out.println("Unable to write to file: " + JSON_STORE);
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("player", playerToJson());
        json.put("enemies", enemiesToJson());
        json.put("offsetX", this.lvlOffsetX);
        json.put("offsetY", this.lvlOffsetY);
        json.put("numAlive", enemyHandler.getNumAlive());
        json.put("numEnemies", game.getNumEnemies());
        return json;
    }

    // EFFECTS: returns player in this world as a JSON array
    private JSONArray playerToJson() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(player.toJson());
        return jsonArray;
    }

    // EFFECTS: returns enemies in this world as a JSON array
    private JSONArray enemiesToJson() {
        JSONArray jsonArray = new JSONArray();

        for (Enemy e : enemyHandler.getEnemies()) {
            jsonArray.put(e.toJson());
        }

        return jsonArray;
    }
}