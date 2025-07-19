package de.felixhertweck.seatreservation.utils;

import java.util.Collection;
import java.util.Set;

import de.felixhertweck.seatreservation.model.entity.Seat;

public class SvgRenderer {

    public static String renderSeats(Collection<Seat> seats, Set<String> highlightSeatNumbers) {
        int width = 800;
        int height = 600;
        int radius = 15;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        for (Seat seat : seats) {
            String color = highlightSeatNumbers.contains(seat.getSeatNumber()) ? "red" : "gray";
            sb.append("<circle cx=\"")
                    .append(seat.getXCoordinate())
                    .append("\" cy=\"")
                    .append(seat.getYCoordinate())
                    .append("\" r=\"")
                    .append(radius)
                    .append("\" fill=\"")
                    .append(color)
                    .append("\" stroke=\"black\" stroke-width=\"1\" />\n");
            sb.append("<text x=\"")
                    .append(seat.getXCoordinate())
                    .append("\" y=\"")
                    .append(seat.getYCoordinate() + radius + 12)
                    .append("\" font-size=\"12\" text-anchor=\"middle\">")
                    .append(seat.getSeatNumber())
                    .append("</text>\n");
        }

        sb.append("</svg>");
        return sb.toString();
    }
}
