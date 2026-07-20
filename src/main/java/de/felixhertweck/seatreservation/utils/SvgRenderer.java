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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.felixhertweck.seatreservation.common.dto.AreaDTO;
import de.felixhertweck.seatreservation.common.dto.CoordinateDTO;
import de.felixhertweck.seatreservation.model.entity.EventLocationMarker;
import de.felixhertweck.seatreservation.model.entity.Seat;

/**
 * Utility class for rendering seats and markers as SVG graphics. Provides methods to generate SVG
 * output for seat layouts with support for different reservation statuses and markers.
 */
public class SvgRenderer {

    // Distinct from the seat-status colors below (#2B7FFF/#F0B100/#CCCCCC) and mirroring the
    // palette used for area zones in the web app's seat map, so the same area always looks the
    // same across the UI and the emailed image.
    private static final String[] AREA_COLORS = {
        "#fbbf24", // amber
        "#22d3ee", // cyan
        "#818cf8", // indigo
        "#f472b6", // pink
        "#fb923c", // orange
        "#2dd4bf", // teal
    };

    /**
     * Renders a collection of seats as SVG with reserved seat numbers. No markers are included in
     * the output.
     *
     * @param allSeats the collection of all seats to render
     * @param newReservedSeatNumbers the set of newly reserved seat numbers
     * @param existingReservedSeatNumbers the set of previously reserved seat numbers
     * @return an SVG string representation of the seats
     */
    public static String renderSeats(
            Collection<Seat> allSeats,
            Set<String> newReservedSeatNumbers,
            Set<String> existingReservedSeatNumbers) {
        return renderSeats(allSeats, newReservedSeatNumbers, existingReservedSeatNumbers, null);
    }

    /**
     * Renders a collection of seats and optional markers as SVG with reserved seat numbers.
     *
     * @param allSeats the collection of all seats to render
     * @param newReservedSeatNumbers the set of newly reserved seat numbers
     * @param existingReservedSeatNumbers the set of previously reserved seat numbers
     * @param markers the collection of markers to include in the SVG (optional)
     * @return an SVG string representation of the seats and markers
     */
    public static String renderSeats(
            Collection<Seat> allSeats,
            Set<String> newReservedSeatNumbers,
            Set<String> existingReservedSeatNumbers,
            Collection<EventLocationMarker> markers) {
        return renderSeats(
                allSeats, newReservedSeatNumbers, existingReservedSeatNumbers, markers, null);
    }

    /**
     * Renders a collection of seats, optional markers, and optional areas as SVG with reserved seat
     * numbers. Areas are drawn as the backmost layer, behind markers and seats: a custom boundary
     * polygon when {@link AreaDTO#boundary()} has at least 3 points, otherwise a bounding box
     * derived from the coordinates of the area's member seats.
     *
     * @param allSeats the collection of all seats to render
     * @param newReservedSeatNumbers the set of newly reserved seat numbers
     * @param existingReservedSeatNumbers the set of previously reserved seat numbers
     * @param markers the collection of markers to include in the SVG (optional)
     * @param areas the areas to render as zones behind the seats (optional)
     * @return an SVG string representation of the seats, markers, and areas
     */
    public static String renderSeats(
            Collection<Seat> allSeats,
            Set<String> newReservedSeatNumbers,
            Set<String> existingReservedSeatNumbers,
            Collection<EventLocationMarker> markers,
            Collection<AreaDTO> areas) {

        if (allSeats == null || allSeats.isEmpty()) {
            return "";
        }

        // Configuration
        int scale = 40; // Each logical unit is 40 SVG units
        int radius = 15; // Radius of the circle representing a seat
        int padding = 20; // Padding around the entire seat map
        int textHeight = 12; // Font size for the seat number
        int markerTextHeight = 14; // Font size for marker labels

        // Calculate Bounding Box of logical coordinates for seats
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Seat seat : allSeats) {
            minX = Math.min(minX, seat.getCoordinate().xCoordinate());
            minY = Math.min(minY, seat.getCoordinate().yCoordinate());
            maxX = Math.max(maxX, seat.getCoordinate().xCoordinate());
            maxY = Math.max(maxY, seat.getCoordinate().yCoordinate());
        }

        // Include markers in bounding box calculation if they exist
        if (markers != null && !markers.isEmpty()) {
            for (EventLocationMarker marker : markers) {
                if (marker.getCoordinate() != null) {
                    minX = Math.min(minX, marker.getCoordinate().xCoordinate());
                    minY = Math.min(minY, marker.getCoordinate().yCoordinate());
                    maxX = Math.max(maxX, marker.getCoordinate().xCoordinate());
                    maxY = Math.max(maxY, marker.getCoordinate().yCoordinate());
                }
            }
        }

        // Include custom area boundary points in the bounding box calculation so polygons drawn
        // outside the seats' extent (e.g. a rounded balcony edge) aren't clipped by the viewBox.
        if (areas != null && !areas.isEmpty()) {
            for (AreaDTO area : areas) {
                List<CoordinateDTO> boundary = area.boundary();
                if (boundary == null) {
                    continue;
                }
                for (CoordinateDTO point : boundary) {
                    minX = Math.min(minX, point.xCoordinate());
                    minY = Math.min(minY, point.yCoordinate());
                    maxX = Math.max(maxX, point.xCoordinate());
                    maxY = Math.max(maxY, point.yCoordinate());
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

        // Render area zones first (further back than markers and seats)
        appendAreaZones(sb, areas, allSeats, scale);

        // Render markers first (as background layer)
        if (markers != null && !markers.isEmpty()) {
            for (EventLocationMarker marker : markers) {
                if (marker.getCoordinate() != null
                        && marker.getLabel() != null
                        && !marker.getLabel().trim().isEmpty()) {

                    int markerX = marker.getCoordinate().xCoordinate() * scale;
                    int markerY = marker.getCoordinate().yCoordinate() * scale;
                    String label = marker.getLabel();

                    sb.append("<text x=\"")
                            .append(markerX)
                            .append("\" y=\"")
                            .append(markerY)
                            .append("\" font-size=\"")
                            .append(markerTextHeight)
                            .append(
                                    "\" text-anchor=\"middle\" dominant-baseline=\"central\""
                                            + " fill=\"#333333\" font-weight=\"bold\">")
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
            int cx = seat.getCoordinate().xCoordinate() * scale;
            int cy = seat.getCoordinate().yCoordinate() * scale;

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
                    .append("\" text-anchor=\"middle\" fill=\"black\">")
                    .append(escapeXml(seat.getSeatNumber()))
                    .append("</text>\n");
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private static final int AREA_ZONE_INSET = 20;
    private static final int AREA_LABEL_FONT_SIZE = 13;

    /**
     * Appends one zone per area to {@code sb}: a custom boundary polygon when the area supplies at
     * least 3 boundary points, otherwise a bounding box derived from its member seats' coordinates.
     * Areas without any resolvable geometry are skipped.
     */
    private static void appendAreaZones(
            StringBuilder sb, Collection<AreaDTO> areas, Collection<Seat> allSeats, int scale) {
        if (areas == null || areas.isEmpty()) {
            return;
        }

        Map<Long, Seat> seatById = new HashMap<>();
        for (Seat seat : allSeats) {
            if (seat.id != null) {
                seatById.put(seat.id, seat);
            }
        }

        int colorIndex = 0;
        for (AreaDTO area : areas) {
            String color = AREA_COLORS[colorIndex % AREA_COLORS.length];
            colorIndex++;

            List<CoordinateDTO> boundary = area.boundary();
            if (boundary != null && boundary.size() >= 3) {
                appendAreaPolygon(sb, area.name(), boundary, scale, color);
            } else {
                appendAreaBoundingBox(sb, area, seatById, scale, color);
            }
        }
    }

    private static void appendAreaPolygon(
            StringBuilder sb, String name, List<CoordinateDTO> boundary, int scale, String color) {
        int[] xs = new int[boundary.size()];
        int[] ys = new int[boundary.size()];
        double centroidX = 0;
        double centroidY = 0;
        for (int i = 0; i < boundary.size(); i++) {
            xs[i] = boundary.get(i).xCoordinate() * scale;
            ys[i] = boundary.get(i).yCoordinate() * scale;
            centroidX += xs[i];
            centroidY += ys[i];
        }
        centroidX /= boundary.size();
        centroidY /= boundary.size();

        // Push each vertex outward from the centroid so the outline doesn't just clip through the
        // seats it encloses.
        StringBuilder points = new StringBuilder();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < boundary.size(); i++) {
            double dx = xs[i] - centroidX;
            double dy = ys[i] - centroidY;
            double len = Math.sqrt(dx * dx + dy * dy);
            double factor = len == 0 ? 0 : AREA_ZONE_INSET / len;
            int px = (int) Math.round(xs[i] + dx * factor);
            int py = (int) Math.round(ys[i] + dy * factor);
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            if (i > 0) points.append(" ");
            points.append(px).append(",").append(py);
        }

        sb.append("<polygon points=\"")
                .append(points)
                .append("\" fill=\"")
                .append(color)
                .append("\" fill-opacity=\"0.12\" stroke=\"")
                .append(color)
                .append(
                        "\" stroke-opacity=\"0.8\" stroke-width=\"2\" stroke-dasharray=\"8"
                                + " 5\" />\n");

        appendAreaLabel(sb, name, minX, minY, color);
    }

    private static void appendAreaBoundingBox(
            StringBuilder sb, AreaDTO area, Map<Long, Seat> seatById, int scale, String color) {
        List<Long> seatIds = area.seatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;
        for (Long seatId : seatIds) {
            Seat seat = seatById.get(seatId);
            if (seat == null) continue;
            found = true;
            minX = Math.min(minX, seat.getCoordinate().xCoordinate());
            minY = Math.min(minY, seat.getCoordinate().yCoordinate());
            maxX = Math.max(maxX, seat.getCoordinate().xCoordinate());
            maxY = Math.max(maxY, seat.getCoordinate().yCoordinate());
        }
        if (!found) {
            return;
        }

        int left = minX * scale - AREA_ZONE_INSET;
        int top = minY * scale - AREA_ZONE_INSET;
        int width = (maxX - minX) * scale + AREA_ZONE_INSET * 2;
        int height = (maxY - minY) * scale + AREA_ZONE_INSET * 2;

        sb.append("<rect x=\"")
                .append(left)
                .append("\" y=\"")
                .append(top)
                .append("\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" rx=\"8\" fill=\"")
                .append(color)
                .append("\" fill-opacity=\"0.12\" stroke=\"")
                .append(color)
                .append(
                        "\" stroke-opacity=\"0.8\" stroke-width=\"2\" stroke-dasharray=\"8"
                                + " 5\" />\n");

        appendAreaLabel(sb, area.name(), left, top, color);
    }

    private static void appendAreaLabel(
            StringBuilder sb, String name, int left, int top, String color) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        sb.append("<text x=\"")
                .append(left + 6)
                .append("\" y=\"")
                .append(top + AREA_LABEL_FONT_SIZE)
                .append("\" font-size=\"")
                .append(AREA_LABEL_FONT_SIZE)
                .append("\" fill=\"")
                .append(color)
                .append("\" font-weight=\"bold\">")
                .append(escapeXml(name))
                .append("</text>\n");
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
