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

import java.util.Collection;
import java.util.Set;

import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Seat;

public class SvgRenderer {

    public static String renderSeats(
            Collection<Seat> allSeats,
            Set<String> newReservedSeatNumbers,
            Set<String> existingReservedSeatNumbers) {
        return renderSeats(allSeats, newReservedSeatNumbers, existingReservedSeatNumbers, null);
    }

    public static String renderSeats(
            Collection<Seat> allSeats,
            Set<String> newReservedSeatNumbers,
            Set<String> existingReservedSeatNumbers,
            Collection<EventLocationMarker> markers) {

        if (allSeats == null || allSeats.isEmpty()) {
            return "";
        }

        // Configuration
        int scale = 40; // Each logical unit is 40 SVG units
        int radius = 15; // Radius of the circle representing a seat
        int padding = 20; // Padding around the entire seat map
        int textHeight = 12; // Font size for the seat number
        int markerTextHeight = 10; // Font size for marker labels

        // Calculate Bounding Box of logical coordinates for seats
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Seat seat : allSeats) {
            minX = Math.min(minX, seat.getxCoordinate());
            minY = Math.min(minY, seat.getyCoordinate());
            maxX = Math.max(maxX, seat.getxCoordinate());
            maxY = Math.max(maxY, seat.getyCoordinate());
        }

        // Include markers in bounding box calculation if they exist
        if (markers != null && !markers.isEmpty()) {
            for (EventLocationMarker marker : markers) {
                if (marker.getxCoordinate() != null && marker.getyCoordinate() != null) {
                    minX = Math.min(minX, marker.getxCoordinate());
                    minY = Math.min(minY, marker.getyCoordinate());
                    maxX = Math.max(maxX, marker.getxCoordinate());
                    maxY = Math.max(maxY, marker.getyCoordinate());
                }
            }
        }

        // Calculate viewBox dimensions based on scaled coordinates
        int viewboxX = (minX * scale) - radius - padding;
        int viewboxY = (minY * scale) - radius - padding;
        int viewboxWidth = ((maxX - minX) * scale) + 2 * (radius + padding);
        int viewboxHeight = ((maxY - minY) * scale) + 2 * (radius + padding) + textHeight;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg width=\"100%\" viewBox=\"")
                .append(viewboxX)
                .append(" ")
                .append(viewboxY)
                .append(" ")
                .append(viewboxWidth)
                .append(" ")
                .append(viewboxHeight)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        // Render markers first (as background layer)
        if (markers != null && !markers.isEmpty()) {
            for (EventLocationMarker marker : markers) {
                if (marker.getxCoordinate() != null
                        && marker.getyCoordinate() != null
                        && marker.getLabel() != null
                        && !marker.getLabel().trim().isEmpty()) {

                    int markerX = marker.getxCoordinate() * scale;
                    int markerY = marker.getyCoordinate() * scale;

                    // Render marker label as text only
                    String label = marker.getLabel();

                    sb.append("<text x=\"")
                            .append(markerX)
                            .append("\" y=\"")
                            .append(markerY + (markerTextHeight / 3))
                            .append("\" font-size=\"")
                            .append(markerTextHeight)
                            .append(
                                    "\" text-anchor=\"middle\" fill=\"#666666\""
                                            + " font-weight=\"bold\">")
                            .append(escapeXml(label))
                            .append("</text>\n");
                }
            }
        }

        // Render seats (as foreground layer)
        for (Seat seat : allSeats) {
            String color;
            if (newReservedSeatNumbers.contains(seat.getSeatNumber())) {
                color = "#2B7FFF";
            } else if (existingReservedSeatNumbers.contains(seat.getSeatNumber())) {
                color = "#F0B100";
            } else {
                color = "#CCCCCC";
            }

            // Apply scaling to the coordinates when drawing
            int cx = seat.getxCoordinate() * scale;
            int cy = seat.getyCoordinate() * scale;

            sb.append("<circle cx=\"")
                    .append(cx)
                    .append("\" cy=\"")
                    .append(cy)
                    .append("\" r=\"")
                    .append(radius)
                    .append("\" fill=\"")
                    .append(color)
                    .append("\" stroke=\"#333333\" stroke-width=\"1\" />\n");
            sb.append("<text x=\"")
                    .append(cx)
                    .append("\" y=\"")
                    .append(cy + (textHeight / 3))
                    .append("\" font-size=\"")
                    .append(textHeight)
                    .append("\" text-anchor=\"middle\" fill=\"white\">")
                    .append(seat.getSeatNumber())
                    .append("</text>\n");
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
