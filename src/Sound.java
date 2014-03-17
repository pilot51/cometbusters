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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public enum Sound {
	THRUST("snd/thrust.wav"),
	SHOOT("snd/shoot.wav"),
	ROCK_EXPLODE("snd/rock_explode.wav");
	
	static void init() {
		values();
	}
	
	private Clip clip;
	
	private Sound(String filename) {
		try {
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(getClass().getClassLoader().getResource(filename));
			clip = (Clip)AudioSystem.getLine(new DataLine.Info(Clip.class, audioStream.getFormat()));
			clip.open(audioStream);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	void play() {
		clip.setFramePosition(0);
		clip.loop(0); // Workaround for Clip.start() to avoid using Clip.stop()
	}
	
	void loop() {
		if (!clip.isRunning()) {
			clip.setFramePosition(0);
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
	}
	
	void stop() {
		if (clip.isRunning()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					clip.stop(); // FIXME: This is very laggy
				}
			}).start();
		}
	}
}