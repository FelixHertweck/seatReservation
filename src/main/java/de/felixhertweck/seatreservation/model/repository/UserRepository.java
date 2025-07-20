package de.felixhertweck.seatreservation.model.repository;

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Optional<User> findByUsernameOptional(String username) {
        return find("username", username).firstResultOptional();
    }
}
