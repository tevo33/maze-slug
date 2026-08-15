package br.com.estevao.mazeslug;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class MazeGame extends JFrame {
    private static final int TILE = 32;
    private static final int HUD_HEIGHT = 52;
    private static final int PLAYER_WIDTH = 28;
    private static final int PLAYER_HEIGHT = 32;
    private static final int ENEMY_WIDTH = 48;
    private static final int ENEMY_HEIGHT = 32;
    private static final int PLAYER_SPEED = 3;
    private static final int FRAME_TIME_MS = 16;

    private final GameMap map = GameMap.load("/rsc/mapa.txt");
    private final JLayeredPane scene = new JLayeredPane();
    private final JLabel player = icon("/rsc/soldier.gif");
    private final JLabel enemy = icon("/rsc/tank.gif");
    private final JLabel exit = icon("/rsc/exit.gif");
    private final JLabel status = new JLabel();
    private final JLabel overlay = new JLabel("", SwingConstants.CENTER);
    private final List<Rectangle> walls = new ArrayList<>();
    private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();
    private Rectangle exitBounds;

    private volatile boolean running = true;
    private volatile GameState state = GameState.PLAYING;
    private volatile int playerX;
    private volatile int playerY;
    private volatile int enemyX;
    private volatile int enemyY;
    private int enemyVelocityX = 3;
    private int enemyVelocityY = 2;
    private Thread gameThread;

    public MazeGame() {
        super("Maze Slug");
        configureWindow();
        createScene();
        configureKeys();
        resetPositions();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                running = false;
            }
        });
    }

    private void createScene() {
        int width = map.columns() * TILE;
        int height = map.rows() * TILE + HUD_HEIGHT;
        scene.setPreferredSize(new Dimension(width, height));
        scene.setOpaque(true);
        scene.setBackground(new Color(12, 14, 18));
        setContentPane(scene);

        ImageIcon floorIcon = loadIcon("/rsc/floor.png");
        ImageIcon wallIcon = loadIcon("/rsc/brick.png");

        for (int row = 0; row < map.rows(); row++) {
            for (int column = 0; column < map.columns(); column++) {
                int x = column * TILE;
                int y = HUD_HEIGHT + row * TILE;
                JLabel floor = new JLabel(floorIcon);
                floor.setBounds(x, y, TILE, TILE);
                scene.add(floor, Integer.valueOf(0));
                if (map.cellAt(row, column) == '#') {
                    JLabel wall = new JLabel(wallIcon);
                    wall.setBounds(x, y, TILE, TILE);
                    walls.add(new Rectangle(x, y, TILE, TILE));
                    scene.add(wall, Integer.valueOf(100));
                }
            }
        }

        Point exitPoint = map.exit();
        exitBounds = new Rectangle(exitPoint.x * TILE, HUD_HEIGHT + exitPoint.y * TILE, TILE, TILE);
        exit.setBounds(exitBounds);
        scene.add(exit, Integer.valueOf(150));

        player.setBounds(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT);
        enemy.setBounds(0, 0, ENEMY_WIDTH, ENEMY_HEIGHT);
        scene.add(player, Integer.valueOf(300));
        scene.add(enemy, Integer.valueOf(300));

        status.setOpaque(true);
        status.setBackground(new Color(20, 24, 30));
        status.setForeground(new Color(232, 235, 239));
        status.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBounds(0, 0, width, HUD_HEIGHT);
        scene.add(status, Integer.valueOf(500));

        overlay.setOpaque(true);
        overlay.setBackground(new Color(8, 10, 14, 225));
        overlay.setForeground(Color.WHITE);
        overlay.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        overlay.setBounds(0, HUD_HEIGHT, width, map.rows() * TILE);
        overlay.setVisible(false);
        scene.add(overlay, Integer.valueOf(600));

        pack();
        setLocationRelativeTo(null);
    }

    private void configureKeys() {
        bind("W", KeyEvent.VK_W);
        bind("A", KeyEvent.VK_A);
        bind("S", KeyEvent.VK_S);
        bind("D", KeyEvent.VK_D);
        bind("UP", KeyEvent.VK_UP);
        bind("LEFT", KeyEvent.VK_LEFT);
        bind("DOWN", KeyEvent.VK_DOWN);
        bind("RIGHT", KeyEvent.VK_RIGHT);
        bind("R", KeyEvent.VK_R);
        bind("ESCAPE", KeyEvent.VK_ESCAPE);
    }

    private void bind(String keyName, int keyCode) {
        String pressAction = keyCode + ".press";
        String releaseAction = keyCode + ".release";
        scene.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + keyName), pressAction);
        scene.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released " + keyName), releaseAction);
        scene.getActionMap().put(pressAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                keyChanged(keyCode, true);
            }
        });
        scene.getActionMap().put(releaseAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                keyChanged(keyCode, false);
            }
        });
    }

    private void keyChanged(int keyCode, boolean pressed) {
        if (keyCode == KeyEvent.VK_ESCAPE && pressed) {
            running = false;
            dispose();
            return;
        }
        if (keyCode == KeyEvent.VK_R && pressed) {
            restart();
            return;
        }
        if (pressed) {
            pressedKeys.add(keyCode);
        } else {
            pressedKeys.remove(keyCode);
        }
    }

    private synchronized void restart() {
        pressedKeys.clear();
        resetPositions();
        state = GameState.PLAYING;
        overlay.setVisible(false);
        render();
    }

    private void resetPositions() {
        Point playerStart = map.playerStart();
        Point enemyStart = map.enemyStart();
        playerX = playerStart.x * TILE + (TILE - PLAYER_WIDTH) / 2;
        playerY = HUD_HEIGHT + playerStart.y * TILE;
        enemyX = enemyStart.x * TILE + (TILE - ENEMY_WIDTH) / 2;
        enemyY = HUD_HEIGHT + enemyStart.y * TILE;
        enemyVelocityX = 3;
        enemyVelocityY = 2;
    }

    public void start() {
        setVisible(true);
        gameThread = new Thread(this::gameLoop, "maze-game-loop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void gameLoop() {
        while (running) {
            long startedAt = System.nanoTime();
            updateGame();
            SwingUtilities.invokeLater(this::render);
            long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
            long wait = Math.max(1L, FRAME_TIME_MS - elapsed);
            try {
                Thread.sleep(wait);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private synchronized void updateGame() {
        if (state != GameState.PLAYING) {
            return;
        }

        int deltaX = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - axis(KeyEvent.VK_A, KeyEvent.VK_LEFT);
        int deltaY = axis(KeyEvent.VK_S, KeyEvent.VK_DOWN) - axis(KeyEvent.VK_W, KeyEvent.VK_UP);
        if (deltaX != 0 && deltaY != 0) {
            deltaX *= 2;
            deltaY *= 2;
        } else {
            deltaX *= PLAYER_SPEED;
            deltaY *= PLAYER_SPEED;
        }

        if (deltaX != 0 || deltaY != 0) {
            Rectangle nextPlayer = new Rectangle(playerX + deltaX, playerY + deltaY, PLAYER_WIDTH, PLAYER_HEIGHT);
            if (outsideMap(nextPlayer) || collidesWithWall(nextPlayer)) {
                lose();
                return;
            }
            playerX += deltaX;
            playerY += deltaY;
        }

        moveEnemy();

        Rectangle playerBounds = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        Rectangle enemyBounds = new Rectangle(enemyX, enemyY, ENEMY_WIDTH, ENEMY_HEIGHT);
        if (playerBounds.intersects(enemyBounds)) {
            lose();
            return;
        }

        if (playerBounds.intersects(exitBounds)) {
            win();
        }
    }

    private int axis(int primary, int alternative) {
        return pressedKeys.contains(primary) || pressedKeys.contains(alternative) ? 1 : 0;
    }

    private void moveEnemy() {
        Rectangle nextX = new Rectangle(enemyX + enemyVelocityX, enemyY, ENEMY_WIDTH, ENEMY_HEIGHT);
        if (outsideMap(nextX) || collidesWithWall(nextX)) {
            enemyVelocityX = -enemyVelocityX;
        } else {
            enemyX += enemyVelocityX;
        }

        Rectangle nextY = new Rectangle(enemyX, enemyY + enemyVelocityY, ENEMY_WIDTH, ENEMY_HEIGHT);
        if (outsideMap(nextY) || collidesWithWall(nextY)) {
            enemyVelocityY = -enemyVelocityY;
        } else {
            enemyY += enemyVelocityY;
        }
    }

    private boolean collidesWithWall(Rectangle bounds) {
        for (Rectangle wall : walls) {
            if (bounds.intersects(wall)) {
                return true;
            }
        }
        return false;
    }

    private boolean outsideMap(Rectangle bounds) {
        int mapTop = HUD_HEIGHT;
        int mapRight = map.columns() * TILE;
        int mapBottom = HUD_HEIGHT + map.rows() * TILE;
        return bounds.x < 0 || bounds.y < mapTop || bounds.x + bounds.width > mapRight || bounds.y + bounds.height > mapBottom;
    }

    private void lose() {
        state = GameState.DEAD;
        pressedKeys.clear();
        SoundPlayer.play("/rsc/death.wav");
    }

    private void win() {
        state = GameState.WON;
        pressedKeys.clear();
        SoundPlayer.play("/rsc/victory.wav");
    }

    private void render() {
        player.setLocation(playerX, playerY);
        enemy.setLocation(enemyX, enemyY);
        if (state == GameState.PLAYING) {
            status.setText("WASD ou setas  |  alcance a saída  |  tocar parede ou tanque = morte");
            overlay.setVisible(false);
        } else if (state == GameState.DEAD) {
            status.setText("Você morreu  |  R reinicia  |  ESC sai");
            overlay.setText("<html><div style='text-align:center'>MISSÃO FALHOU<br><span style='font-size:16px'>Pressione R para tentar novamente</span></div></html>");
            overlay.setVisible(true);
        } else {
            status.setText("Saída alcançada  |  R joga novamente  |  ESC sai");
            overlay.setText("<html><div style='text-align:center'>MISSÃO CONCLUÍDA<br><span style='font-size:16px'>Pressione R para jogar novamente</span></div></html>");
            overlay.setVisible(true);
        }
    }

    private static JLabel icon(String resource) {
        return new JLabel(loadIcon(resource));
    }

    private static ImageIcon loadIcon(String resource) {
        java.net.URL url = MazeGame.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Imagem não encontrada: " + resource);
        }
        return new ImageIcon(url);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeGame().start());
    }

    private enum GameState {
        PLAYING,
        DEAD,
        WON
    }
}
