package de.felixhertweck.seatreservation.repository;

import de.felixhertweck.seatreservation.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    @Transactional
    public boolean createUser(User user) {
        if (findByUsername(user.getUsername()) != null) {
            return false;
        }
        persist(user);
        return true;
    }

    @Transactional
    public boolean updateUser(User user) {
        if (findByUsername(user.getUsername()) != null) {
            User existingUser = findByUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setFirstname(user.getFirstname());
            existingUser.setLastname(user.getLastname());
            existingUser.setPasswordHash(user.getPasswordHash());
            existingUser.setRoles(user.getRoles());
            persist(existingUser);
            return true;
        }
        return false;
    }

    public User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    @Transactional
    public boolean deleteUser(User user) {
        if (findByUsername(user.getUsername()) != null) {
            delete(user);
            return true;
        }
        return false;
    }
}
