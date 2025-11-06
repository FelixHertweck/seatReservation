/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jboss.logging.Logger;

/**
 * Utility class for converting SVG graphics to PNG images. Uses Apache Batik for the conversion.
 */
public class SvgToPngConverter {
    private static final Logger LOG = Logger.getLogger(SvgToPngConverter.class);

    /**
     * Converts an SVG string to a PNG byte array.
     *
     * @param svgContent the SVG content as a string
     * @return the PNG image as a byte array
     * @throws IOException if an I/O error occurs
     * @throws TranscoderException if the transcoding fails
     */
    public static byte[] convertSvgToPng(String svgContent)
            throws IOException, TranscoderException {
        if (svgContent == null || svgContent.isEmpty()) {
            throw new IllegalArgumentException("SVG content cannot be null or empty");
        }

        LOG.debug("Converting SVG to PNG...");

        try (ByteArrayInputStream inputStream =
                        new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8));
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PNGTranscoder transcoder = new PNGTranscoder();

            // Set desired image dimensions
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 800f);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 800f);

            // Set white background color
            transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, java.awt.Color.WHITE);

            // Use indexed color with higher bit depth for smaller file size
            // 256 colors (8-bit) provides good quality with significant compression
            transcoder.addTranscodingHint(PNGTranscoder.KEY_INDEXED, 256);

            TranscoderInput input = new TranscoderInput(inputStream);
            TranscoderOutput output = new TranscoderOutput(outputStream);

            transcoder.transcode(input, output);

            byte[] pngData = outputStream.toByteArray();
            LOG.debugf("Successfully converted SVG to PNG (%d bytes)", pngData.length);
            return pngData;
        }
    }
}
