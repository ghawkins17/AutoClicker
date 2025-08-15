import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.logging.*;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class AutoClicker implements NativeKeyListener {
    private JFrame frame;
    private JTextField intervalField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;

    private Robot robot;
    private Thread clickThread;
    private volatile boolean clicking = false;

    public AutoClicker() {
        setupGUI();
        setupGlobalHotkeys();

        try {
            robot = new Robot();
        } catch (AWTException e) {
            showError("Failed to initialize Robot.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void setupGUI() {
        frame = new JFrame("AutoClicker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(null);

        JLabel intervalLabel = new JLabel("Interval (ms):");
        intervalLabel.setBounds(10, 10, 100, 25);
        frame.add(intervalLabel);

        intervalField = new JTextField("1000");
        intervalField.setBounds(120, 10, 150, 25);
        frame.add(intervalField);

        startButton = new JButton("Start");
        startButton.setBounds(30, 60, 100, 30);
        frame.add(startButton);

        stopButton = new JButton("Stop");
        stopButton.setBounds(150, 60, 100, 30);
        stopButton.setEnabled(false);
        frame.add(stopButton);

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setBounds(10, 110, 250, 25);
        frame.add(statusLabel);

        // Start button logic
        startButton.addActionListener(e -> {
            int interval = getIntervalInput();
            if (interval < 10) {
                showError("Interval must be at least 10 ms.");
                return;
            }
            if (interval > 0) {
                startClicking(interval);
            }
        });

        // Stop button logic
        stopButton.addActionListener(e -> stopClicking());

        frame.setVisible(true);
    }

    private int getIntervalInput() {
        try {
            int interval = Integer.parseInt(intervalField.getText());
            if (interval <= 0) throw new NumberFormatException();
            return interval;
        } catch (NumberFormatException e) {
            showError("Please enter a valid positive number for interval.");
            return -1;
        }
    }

    private void startClicking(int interval) {
        if (clicking) return;

        clicking = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Status: Clicking...");

        clickThread = new Thread(() -> {
            try {
                while (clicking) {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    Thread.sleep(interval);
                }
            } catch (InterruptedException e) {
                // thread interrupted
            }
        });
        clickThread.start();
    }

    private void stopClicking() {
        if (!clicking) return;

        clicking = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Status: Stopped");

        if (clickThread != null && clickThread.isAlive()) {
            clickThread.interrupt();
        }
    }

    private void setupGlobalHotkeys() {
        try {
            // Suppress native hook logging spam
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);

        } catch (NativeHookException e) {
            showError("Failed to register global hotkeys.");
            e.printStackTrace();
        }
    }

    // Called when any key is pressed globally
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == NativeKeyEvent.VC_F6) {
            SwingUtilities.invokeLater(() -> {
                if (clicking) {
                    stopClicking();
                } else {
                    int interval = getIntervalInput();
                    if (interval > 0) {
                        startClicking(interval);
                    }
                }
            });
        }

        if (keyCode == NativeKeyEvent.VC_ESCAPE) {
            SwingUtilities.invokeLater(() -> {
                stopClicking();
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {}

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutoClicker());
    }
}

