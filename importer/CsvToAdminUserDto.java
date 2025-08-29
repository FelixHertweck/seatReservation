package importer;

import java.io.*;
import java.util.*;

public class CsvToAdminUserDto {

    private static final Set<String> DEFAULT_ROLES = Set.of("USER");
    private static final Set<String> DEFAULT_TAGS = Set.of("imported");

    private static final String CSV_SEPARATOR = ";";
    private static final String JSON_OUTPUT_FILE = "output.json";
    private static final String CSV_INPUT_FILE = "input.csv";

    public static void main(String[] args) {
        List<AdminUserCreationDto> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_INPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(CSV_SEPARATOR);
                if (parts.length < 3) {
                    System.err.println("Skipping line: " + line);
                    continue;
                }
                String firstname = parts[0].trim();
                String lastname = parts[1].trim();
                String password = parts[2].trim();
                String username = (firstname + "." + lastname).toLowerCase().replaceAll("\\s+", "");

                if(firstname.isEmpty() || lastname.isEmpty() || password.isEmpty()) {
                    System.err.println("Skipping line due to empty fields: " + line);
                    continue;
                }

                AdminUserCreationDto dto = new AdminUserCreationDto(
                        username, null, password, firstname, lastname,
                        DEFAULT_ROLES, DEFAULT_TAGS
                );
                users.add(dto);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            return;
        }

        
        try (PrintWriter pw = new PrintWriter(new FileWriter(JSON_OUTPUT_FILE))) {
            pw.println("[");
            for (int i = 0; i < users.size(); i++) {
                AdminUserCreationDto u = users.get(i);
                pw.print("  {");
                pw.print("\"username\": \"" + escape(u.getUsername()) + "\", ");
                pw.print("\"password\": \"" + escape(u.getPassword()) + "\", ");
                pw.print("\"firstname\": \"" + escape(u.getFirstname()) + "\", ");
                pw.print("\"lastname\": \"" + escape(u.getLastname()) + "\", ");

                // Rollen-Array
                pw.print("\"roles\": [");
                int rcount = 0;
                for (String r : u.getRoles()) {
                    if (rcount++ > 0) pw.print(", ");
                    pw.print("\"" + escape(r) + "\"");
                }
                pw.print("], ");

                // Tags-Array
                pw.print("\"tags\": [");
                int tcount = 0;
                for (String t : u.getTags()) {
                    if (tcount++ > 0) pw.print(", ");
                    pw.print("\"" + escape(t) + "\"");
                }
                pw.print("]");

                pw.print("}");

                if (i < users.size() - 1) pw.println(","); else pw.println();
            }
            pw.println("]");
        } catch (IOException e) {
            System.err.println("Error writing JSON: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


class AdminUserCreationDto {
    private final String username;
    private final String email;
    private final String password;
    private final String firstname;
    private final String lastname;

    private final Set<String> roles;
    private final Set<String> tags;

    public AdminUserCreationDto(String username, String email, String password,
                                String firstname, String lastname, Set<String> roles, Set<String> tags) {
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.password = Objects.requireNonNull(password);
        this.firstname = Objects.requireNonNull(firstname);
        this.lastname = Objects.requireNonNull(lastname);
        this.roles = Objects.requireNonNull(roles);
        this.tags = Objects.requireNonNull(tags);
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstname() { return firstname; }
    public String getLastname() { return lastname; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getTags() { return tags; }
}
