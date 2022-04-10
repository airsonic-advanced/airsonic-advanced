/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service.jukebox;

import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.airsonic.player.service.jukebox.AudioPlayer.State.*;

/**
 * A simple wrapper for playing sound from an input stream.
 * <p/>
 * Supports pause and resume, but not restarting.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class AudioPlayer implements AutoCloseable {

    public static final float DEFAULT_GAIN = 0.75f;
    private static final Logger LOG = LoggerFactory.getLogger(AudioPlayer.class);

    private final InputStream in;
    private final Listener listener;
    private final AtomicReference<State> state = new AtomicReference<State>(PAUSED);
    private SourceDataLine line;
    private AudioFormat format;
    private DataLine.Info info;
    private FloatControl gainControl;

    public AudioPlayer(InputStream in, String command, Listener listener) throws Exception {
        this.in = in;
        this.listener = listener;

        // SampleRate
        String sampleRate = "";
        Pattern r1 = Pattern.compile("\\-ar (.*?)\\ ");
        Matcher sampleRate1 = r1.matcher(command);
        while (sampleRate1.find()) {
            sampleRate = sampleRate1.group(1);
        }
        LOG.debug("SampleRate: " + sampleRate);

        // Channel
        String channel = "";
        Pattern r2 = Pattern.compile("\\-ac (.*?)\\ ");
        Matcher channel2 = r2.matcher(command);
        while (channel2.find()) {
            channel = channel2.group(1);
        }
        LOG.debug("Channel: " + channel);

        // Format
        String pt = "";
        Pattern r3 = Pattern.compile("\\-f (.*?)\\ ");
        Matcher pt3 = r3.matcher(command);
        while (pt3.find()) {
            pt = pt3.group(1);
        }
        LOG.debug("Format: " + pt);

        // Bits
        String bits = pt.substring(1, 3);
        LOG.debug("Bits: " + bits);

        // Big/Little Endian
        Boolean sl = true;
        if (pt.substring(3, 5).equals("be")) {
            sl = true;
        }
        if (pt.substring(3, 5).equals("le")) {
            sl = false;
        }
        LOG.debug("BigEndian: " + sl);

        // Try Big/Little Endian
        try {
            format = new AudioFormat(Integer.parseInt(sampleRate), Integer.parseInt(bits), Integer.parseInt(channel), true, sl);
            info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
        } catch (IllegalArgumentException e) {
            format = new AudioFormat(Integer.parseInt(sampleRate), Integer.parseInt(bits), Integer.parseInt(channel), false, sl);
            info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
        }

        line.open(format);
        LOG.debug("Opened line " + line);

        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            setGain(DEFAULT_GAIN);
        }
        new AudioDataWriter();
    }

    /**
     * Starts (or resumes) the player.  This only has effect if the current state is
     * {@link State#PAUSED}.
     */
    public synchronized void play() {
        if (state.get() == PAUSED) {
            line.start();
            setState(PLAYING);
        }
    }

    /**
     * Pauses the player.  This only has effect if the current state is
     * {@link State#PLAYING}.
     */
    public synchronized void pause() {
        if (state.get() == PLAYING) {
            setState(PAUSED);
            line.stop();
            line.flush();
        }
    }

    /**
     * Closes the player, releasing all resources. After this the player state is
     * {@link State#CLOSED} (unless the current state is {@link State#EOM}).
     */
    @Override
    public synchronized void close() {
        if (state.get() != CLOSED && state.get() != EOM) {
            setState(CLOSED);
        }

        try {
            line.stop();
        } catch (Throwable x) {
            LOG.warn("Failed to stop player: " + x, x);
        }
        try {
            if (line.isOpen()) {
                line.close();
                LOG.debug("Closed line " + line);
            }
        } catch (Throwable x) {
            LOG.warn("Failed to close player: " + x, x);
        }
        FileUtil.closeQuietly(in);
    }

    /**
     * Returns the player state.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Sets the gain.
     *
     * @param gain The gain between 0.0 and 1.0.
     */
    public void setGain(float gain) {
        if (gainControl != null) {

            double minGainDB = gainControl.getMinimum();
            double maxGainDB = Math.min(0.0, gainControl.getMaximum());  // Don't use positive gain to avoid distortion.
            double ampGainDB = 0.5f * maxGainDB - minGainDB;
            double cste = Math.log(10.0) / 20;
            double valueDB = minGainDB + (1 / cste) * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * gain);

            valueDB = Math.min(valueDB, maxGainDB);
            valueDB = Math.max(valueDB, minGainDB);

            gainControl.setValue((float) valueDB);
        }
    }

    /**
     * Returns the position in seconds.
     */
    public int getPosition() {
        return (int) (line.getMicrosecondPosition() / 1000000L);
    }

    private void setState(State state) {
        if (this.state.getAndSet(state) != state && listener != null) {
            listener.stateChanged(this, state);
        }
    }

    private class AudioDataWriter implements Runnable {

        public AudioDataWriter() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[line.getBufferSize()];

                while (true) {

                    switch (state.get()) {
                        case CLOSED:
                        case EOM:
                            return;
                        case PAUSED:
                            Thread.sleep(250);
                            break;
                        case PLAYING:
                            // Fill buffer in order to ensure that write() receives an integral number of frames.
                            int n = fill(buffer);
                            if (n == -1) {
                                setState(EOM);
                                return;
                            }
                            line.write(buffer, 0, n);
                            break;
                    }
                }
            } catch (Throwable x) {
                LOG.warn("Error when copying audio data: " + x, x);
            } finally {
                close();
            }
        }

        private int fill(byte[] buffer) throws IOException {
            int bytesRead = 0;
            while (bytesRead < buffer.length) {
                int n = in.read(buffer, bytesRead, buffer.length - bytesRead);
                if (n == -1) {
                    return bytesRead == 0 ? -1 : bytesRead;
                }
                bytesRead += n;
            }
            return bytesRead;
        }
    }

    public interface Listener {
        void stateChanged(AudioPlayer player, State state);
    }

    public static enum State {
        PAUSED,
        PLAYING,
        CLOSED,
        EOM
    }
}
