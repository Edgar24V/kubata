package ao.allon.kubata.core;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.FileWriter;
import java.io.IOException;

public class PasswordGenerator {
    public static void main(String[] args) throws IOException {
        String hash = new BCryptPasswordEncoder().encode("dev1234");
        try (FileWriter writer = new FileWriter("hash.txt")) {
            writer.write(hash);
        }
        System.out.println(hash);
    }
}
