package de.felixhertweck.seatreservation.utils;

import java.util.UUID;

public class RandomUUIDString {
    public static String generate() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString().replaceAll("_", "");
    }
}
