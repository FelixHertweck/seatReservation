package de.felixhertweck.seatreservation;

import io.quarkus.elytron.security.common.BcryptUtil;

public class Test {
    public static void main(String[] args) {
        String password = "test";
        String hashedPassword = BcryptUtil.bcryptHash(password);
        System.out.println("Hashed Password: " + hashedPassword);
        boolean matches = BcryptUtil.matches(password, hashedPassword);
        System.out.println("Password matches: " + matches);
    }
}
