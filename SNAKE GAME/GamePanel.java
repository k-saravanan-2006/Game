import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 25;
    private final int TILES_X = 28;
    private final int TILES_Y = 24;
    private final int PANEL_WIDTH = TILE_SIZE * TILES_X;
    private final int PANEL_HEIGHT = TILE_SIZE * TILES_Y;
    private final int MAX_LENGTH = TILES_X * TILES_Y;

    private final int[] snakeX = new int[MAX_LENGTH];
    private final int[] snakeY = new int[MAX_LENGTH];
    private int snakeLength;

    private Direction direction = Direction.RIGHT;
    private boolean running = false;

    private int appleX, appleY;
    private final Random rand = new Random();

    private Timer timer;
    private final int INIT_DELAY = 110;
    private int score = 0;

    private Clip eatClip;
    private Clip gameOverClip;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadSounds();
        startNewGame();
    }

    private void loadSounds() {
        try {
            eatClip = AudioSystem.getClip();
            eatClip.open(AudioSystem.getAudioInputStream(new File("src/eat.wav")));
            gameOverClip = AudioSystem.getClip();
            gameOverClip.open(AudioSystem.getAudioInputStream(new File("src/gameover.wav")));
        } catch (Exception e) {
            System.out.println("Sound load error: " + e.getMessage());
        }
    }

    private void playEatSound() {
        if (eatClip != null) {
            eatClip.setFramePosition(0);
            eatClip.start();
        }
    }

    private void playGameOverSound() {
        if (gameOverClip != null) {
            gameOverClip.setFramePosition(0);
            gameOverClip.start();
        }
    }

    private void startNewGame() {
        snakeLength = 5;
        int startX = TILES_X / 2;
        int startY = TILES_Y / 2;
        for (int i = 0; i < snakeLength; i++) {
            snakeX[i] = (startX - i) * TILE_SIZE;
            snakeY[i] = startY * TILE_SIZE;
        }
        direction = Direction.RIGHT;
        score = 0;
        running = true;
        placeApple();

        if (timer != null) timer.stop();
        timer = new Timer(INIT_DELAY, this);
        timer.start();
    }

    private void placeApple() {
        boolean onSnake;
        do {
            onSnake = false;
            appleX = rand.nextInt(TILES_X) * TILE_SIZE;
            appleY = rand.nextInt(TILES_Y) * TILE_SIZE;
            for (int i = 0; i < snakeLength; i++) {
                if (snakeX[i] == appleX && snakeY[i] == appleY) {
                    onSnake = true;
                    break;
                }
            }
        } while (onSnake);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw apple
        g.setColor(Color.RED);
        g.fillOval(appleX + 3, appleY + 3, TILE_SIZE - 6, TILE_SIZE - 6);

        // Draw snake
        for (int i = 0; i < snakeLength; i++) {
            if (i == 0) {
                g.setColor(new Color(0, 200, 0));
                g.fillRect(snakeX[i], snakeY[i], TILE_SIZE, TILE_SIZE);
                drawSnakeFace(g, snakeX[i], snakeY[i]); // 👀 draw face
            } else {
                g.setColor(new Color(0, 120, 0));
                g.fillRect(snakeX[i], snakeY[i], TILE_SIZE, TILE_SIZE);
            }
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Helvetica", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);

        if (!running) drawGameOver(g);
    }

    // 🐍 Snake Face Drawing
    private void drawSnakeFace(Graphics g, int x, int y) {
        g.setColor(Color.WHITE);
        int eyeSize = 4;
        int offset = 5;

        switch (direction) {
            case UP -> {
                g.fillOval(x + 6, y + 5, eyeSize, eyeSize);
                g.fillOval(x + 15, y + 5, eyeSize, eyeSize);
                g.setColor(Color.PINK);
                g.drawLine(x + 12, y, x + 12, y - 5); // tongue
            }
            case DOWN -> {
                g.fillOval(x + 6, y + 15, eyeSize, eyeSize);
                g.fillOval(x + 15, y + 15, eyeSize, eyeSize);
                g.setColor(Color.PINK);
                g.drawLine(x + 12, y + 25, x + 12, y + 30); // tongue
            }
            case LEFT -> {
                g.fillOval(x + 5, y + 6, eyeSize, eyeSize);
                g.fillOval(x + 5, y + 15, eyeSize, eyeSize);
                g.setColor(Color.PINK);
                g.drawLine(x, y + 12, x - 5, y + 12); // tongue
            }
            case RIGHT -> {
                g.fillOval(x + 15, y + 6, eyeSize, eyeSize);
                g.fillOval(x + 15, y + 15, eyeSize, eyeSize);
                g.setColor(Color.PINK);
                g.drawLine(x + 25, y + 12, x + 30, y + 12); // tongue
            }
        }
    }

    private void drawGameOver(Graphics g) {
        playGameOverSound();
        String msg = "Game Over";
        String restart = "Press ENTER to restart";
        g.setColor(Color.WHITE);
        g.setFont(new Font("Helvetica", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (PANEL_WIDTH - fm.stringWidth(msg)) / 2, PANEL_HEIGHT / 2 - 20);

        g.setFont(new Font("Helvetica", Font.PLAIN, 18));
        g.drawString(restart, (PANEL_WIDTH - g.getFontMetrics().stringWidth(restart)) / 2, PANEL_HEIGHT / 2 + 20);
        g.drawString("Final Score: " + score,
                (PANEL_WIDTH - g.getFontMetrics().stringWidth("Final Score: " + score)) / 2,
                PANEL_HEIGHT / 2 + 45);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            moveSnake();
            checkApple();
            checkCollisions();
        }
        repaint();
    }

    private void moveSnake() {
        for (int i = snakeLength - 1; i > 0; i--) {
            snakeX[i] = snakeX[i - 1];
            snakeY[i] = snakeY[i - 1];
        }

        switch (direction) {
            case LEFT -> snakeX[0] -= TILE_SIZE;
            case RIGHT -> snakeX[0] += TILE_SIZE;
            case UP -> snakeY[0] -= TILE_SIZE;
            case DOWN -> snakeY[0] += TILE_SIZE;
        }
    }

    private void checkApple() {
        if (snakeX[0] == appleX && snakeY[0] == appleY) {
            snakeLength++;
            score += 10;
            placeApple();
            playEatSound();

            int newDelay = Math.max(30, timer.getDelay() - 2);
            timer.setDelay(newDelay);
        }
    }

    private void checkCollisions() {
        if (snakeX[0] < 0) snakeX[0] = PANEL_WIDTH - TILE_SIZE;
        else if (snakeX[0] >= PANEL_WIDTH) snakeX[0] = 0;
        if (snakeY[0] < 0) snakeY[0] = PANEL_HEIGHT - TILE_SIZE;
        else if (snakeY[0] >= PANEL_HEIGHT) snakeY[0] = 0;

        for (int i = 1; i < snakeLength; i++) {
            if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                running = false;
                timer.stop();
                return;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (!running && key == KeyEvent.VK_ENTER) {
            startNewGame();
            return;
        }

        switch (key) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                if (direction != Direction.RIGHT) direction = Direction.LEFT;
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (direction != Direction.LEFT) direction = Direction.RIGHT;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                if (direction != Direction.DOWN) direction = Direction.UP;
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (direction != Direction.UP) direction = Direction.DOWN;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
