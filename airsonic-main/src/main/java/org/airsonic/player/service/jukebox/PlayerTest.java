package org.airsonic.player.service.jukebox;

import javax.swing.*;

import java.awt.*;
import java.io.FileInputStream;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayerTest implements AudioPlayer.Listener {

    private AudioPlayer player;

    public PlayerTest() {
        createGUI();
    }

    private void createGUI() {
        JFrame frame = new JFrame();

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");
        final JSlider gainSlider = new JSlider(0, 1000);

        startButton.addActionListener(e -> {
            createPlayer();
            player.play();
        });
        stopButton.addActionListener(e -> player.pause());
        resetButton.addActionListener(e -> {
            player.close();
            createPlayer();
        });
        gainSlider.addChangeListener(e -> {
            float gain = gainSlider.getValue() / 1000.0F;
            player.setGain(gain);
        });

        frame.setLayout(new FlowLayout());
        frame.add(startButton);
        frame.add(stopButton);
        frame.add(resetButton);
        frame.add(gainSlider);

        frame.pack();
        frame.setVisible(true);
    }

    private void createPlayer() {
        String command = "ffmpeg -ss %o -i %s -map 0:0 -v 0 -ar 176400 -ac 2 -f s24be -";
        try {
            player = new AudioPlayer(new FileInputStream("/Users/sindre/Downloads/sample.au"), command, this);
        } catch (Exception e) {
            player.close();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new PlayerTest();
    }

    @Override
    public void stateChanged(AudioPlayer player, AudioPlayer.State state) {
        System.out.println(state);
    }
}

