package br.com.estevao.mazeslug;

import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

final class SoundPlayer {
    private SoundPlayer() {
    }

    static void play(String resource) {
        Thread thread = new Thread(() -> {
            URL url = SoundPlayer.class.getResource(resource);
            if (url == null) {
                return;
            }
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try {
                            stream.close();
                        } catch (Exception ignored) {
                        }
                    }
                });
                clip.open(stream);
                clip.start();
            } catch (Exception ignored) {
            }
        }, "sound-player");
        thread.setDaemon(true);
        thread.start();
    }
}
