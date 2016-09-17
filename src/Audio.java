/*
 * Copyright 2013 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public enum Audio {
	MUSIC_GAME("snd/comet.mid"),
	MUSIC_DEATH("snd/comet1.mid"),
	MUSIC_HIGHSCORE("snd/comet2.mid"),
	THRUST("snd/thrust.wav"),
	SHOOT("snd/shoot.wav"),
	SPAWN("snd/spawn.wav"),
	EXPLODE_PLAYER("snd/explode_player.wav"),
	EXPLODE_LARGE("snd/explode_large.wav"),
	EXPLODE_MEDIUM("snd/explode_medium.wav"),
	EXPLODE_SMALL("snd/explode_small.wav");

	static void init() {
		values();
	}

	private static boolean isSoundEnabled = true, isMusicEnabled = true;
	private AudioPlayer player;
	private final String filename;
	private final boolean isMusic;

	private Audio(String filename) {
		this.filename = filename;
		isMusic = filename.endsWith(".mid");
	}

	/**
	 * @return True if sound has been enabled, false if disabled.
	 */
	static boolean toggleSound() {
		isSoundEnabled ^= true;
		if (!isSoundEnabled) {
			stopAll(false);
		}
		return isSoundEnabled;
	}

	/**
	 * @return True if music has been enabled, false if disabled.
	 */
	static boolean toggleMusic() {
		isMusicEnabled ^= true;
		if (!isMusicEnabled) {
			stopAll(true);
		}
		return isMusicEnabled;
	}

	/**
	 * Plays this media if the respective sound or music setting is enabled. If the media is music, any other music playing is stopped first.
	 * @param loop True to loop the sound until stopped, false to play it only once.
	 */
	void play(boolean loop) {
		if (isMusic ? isMusicEnabled : isSoundEnabled) {
			if (isMusic) {
				stopAll(true);
			}
			player = new AudioPlayer(loop);
			player.start();
		}
	}

	/** Convenience for {@link #play(boolean)}, passing false for loop. */
	void play() {
		play(false);
	}

	/** Convenience for {@link #play(boolean)}, passing true for loop. */
	void loop() {
		play(true);
	}

	void stop() {
		if (player != null) {
			player.stop = true;
			player = null;
		}
	}

	/**
	 * Stops all sound effects or music.
	 * @param music True to stop music, false to stop sound effects.
	 */
	private static void stopAll(boolean music) {
		for (Audio s : values()) {
			if (s.isMusic == music) {
				s.stop();
			}
		}
	}

	private class AudioPlayer extends Thread {
		private boolean doLoop, stop;

		private AudioPlayer(boolean loop) {
			doLoop = loop;
		}

		@Override
		public void run() {
			AudioInputStream stream;
			try {
				stream = AudioSystem.getAudioInputStream(getClass().getClassLoader().getResource(filename));
			} catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			AudioFormat format = stream.getFormat();
			SourceDataLine line;
			try {
				line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
				line.open(format);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				return;
			}
			line.start();
			int nBytesRead = 0;
			byte[] buffer = new byte[256];
			stream.mark(32000);
			try {
				while (nBytesRead != -1 && !stop) {
					nBytesRead = stream.read(buffer, 0, buffer.length);
					if (nBytesRead >= 0 && !stop) {
						line.write(buffer, 0, nBytesRead);
					} else if (doLoop && !stop) {
						if (stream.markSupported()) {
							stream.reset();
							nBytesRead = 0;
						} else {
							loop();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				if (doLoop) {
					line.flush();
				} else {
					line.drain();
				}
				line.close();
			}
		}
	}
}
