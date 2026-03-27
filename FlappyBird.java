import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    Image backgroundImg, nightBackgroundImg, birdImg, nightBirdImg, topPipeImg, bottomPipeImg;
    boolean isNight = false;
    float transitionAlpha = 0f;

    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX, y = birdY, width = birdWidth, height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    int pipeX = boardWidth, pipeY = 0;
    int pipeWidth = 64;
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX, y = pipeY, width = pipeWidth, height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    Bird bird;
    double velocityX = -3, velocityY = 0, gravity = 0.5;
    ArrayList<Pipe> pipes;
    Random random = new Random();
    Timer gameLoop, placePipeTimer;
    boolean gameOver = false;
    boolean gameStarted = false;
    double score = 0, highScore = 0;

    JButton retryButton, startButton;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);

        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        nightBackgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg_night.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        nightBirdImg = new ImageIcon(getClass().getResource("./flappybird_night.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        bird = new Bird(birdImg);
        pipes = new ArrayList<>();

        placePipeTimer = new Timer(1500, _ -> placePipes());
        gameLoop = new Timer(1000 / 60, this);

        retryButton = new JButton("RETRY");
        retryButton.setFont(new Font("Arial", Font.BOLD, 45));
        retryButton.setOpaque(false);
        retryButton.setContentAreaFilled(false);
        retryButton.setBorderPainted(false);
        retryButton.setForeground(Color.WHITE);
        retryButton.setBackground(Color.YELLOW);
        retryButton.setBounds((boardWidth - 200) / 2, (boardHeight - 100) / 2, 200, 100);
        retryButton.addActionListener(_ -> restartGame());
        retryButton.setVisible(false);
        add(retryButton);

        startButton = new JButton("START");
        startButton.setFont(new Font("Arial", Font.BOLD, 45));
        startButton.setOpaque(false);
        startButton.setContentAreaFilled(false);
        startButton.setBorderPainted(false);
        startButton.setForeground(Color.WHITE);
        startButton.setBackground(Color.GREEN);
        startButton.setBounds((boardWidth - 200) / 2, (boardHeight - 100) / 2, 200, 100);
        startButton.addActionListener(_ -> startGame());
        add(startButton);

        gameLoop.stop();
        placePipeTimer.stop();
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background based on transition
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);
        if (transitionAlpha > 0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitionAlpha));
            g2d.drawImage(nightBackgroundImg, 0, 0, boardWidth, boardHeight, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 25));

        if (gameOver) {
            g.drawString("Game Over: " + (int) score, 10, 35);
        } else if (gameStarted) {
            g.drawString(String.valueOf((int) score), 10, 35);
        }

        g.drawString("High Score: " + (int) highScore, 10, 70);
    }

    public void move() {
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5;
                pipe.passed = true;
                playSound("score.wav");
            }

            if (collision(bird, pipe)) {
                gameOver = true;
                playSound("hit.wav");
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
            playSound("fall.wav");
        }

        // Handle night transition
        if (score >= 10 && score < 20) {
            transitionAlpha = Math.min(transitionAlpha + 0.01f, 1f);
            bird.img = nightBirdImg;
        } else {
            transitionAlpha = Math.max(transitionAlpha - 0.01f, 0f);
            bird.img = birdImg;
        }

        // Speed up game when score reaches 20
        if (score >= 20) {
            velocityX = -4.5;
            gravity = 0.75;
        } else {
            velocityX = -3;
            gravity = 0.5;
        }
    }

    public void playSound(String fileName) {
        new Thread(() -> {
            try {
                File soundFile = new File(fileName);
                if (!soundFile.exists()) {
                    System.out.println("Sound file not found: " + soundFile.getAbsolutePath());
                    return;
                }

                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            } catch (Exception e) {
                System.out.println("Failed to play sound:");
                e.printStackTrace();
            }
        }).start();
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameStarted) return;

        move();
        repaint();
        if (gameOver) {
            if (score > highScore) {
                highScore = score;
            }
            placePipeTimer.stop();
            gameLoop.stop();
            retryButton.setVisible(true);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!gameStarted) {
                startGame();
                return;
            }
            velocityY = -10;
            playSound("jump.wav");

            if (gameOver) {
                restartGame();
            }
        }
    }

    void restartGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        gameOver = false;
        score = 0;
        transitionAlpha = 0f;
        bird.img = birdImg;
        gameStarted = false;
        retryButton.setVisible(false);
        startButton.setVisible(true);
        gameLoop.stop();
        placePipeTimer.stop();
        repaint();
    }

    void startGame() {
        gameStarted = true;
        startButton.setVisible(false);
        gameLoop.start();
        placePipeTimer.start();
        repaint();
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}
