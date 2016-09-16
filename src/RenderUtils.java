/*
 * Copyright 2016 Mark Injerd
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

import java.awt.Color;
import java.awt.image.BufferedImage;

public class RenderUtils {
	/**
	 * Converts all pixels from the source image that aren't fully transparent to the specified color.
	 * @param src Source image to convert.
	 * @param fillColor The desired color.
	 * @return The converted image as a new {@link BufferedImage}.
	 */
	static BufferedImage convertImageToSingleColorWithAlpha(BufferedImage src, Color fillColor) {
		BufferedImage newImage = new BufferedImage(src.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < src.getWidth(); x++) {
			for (int y = 0; y < src.getHeight(); y++) {
				if (new Color(src.getRGB(x, y), true).getAlpha() == 255) {
					newImage.setRGB(x, y, fillColor.getRGB());
				}
			}
		}
		return newImage;
	}
}
