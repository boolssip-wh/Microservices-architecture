package com.swu.smartgrade.user.config;

import com.swu.smartgrade.user.application.PasswordHasher;
import com.swu.smartgrade.user.domain.Teacher;
import com.swu.smartgrade.user.domain.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DemoUserInitializer {
    @Bean
    CommandLineRunner seedUsers(TeacherRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                // Upgrade only untouched seeded demo credentials; preserve custom passwords.
                for (String account : java.util.List.of("teacher1", "assistant1", "admin")) {
                    repository.findByAccount(account).ifPresent(existing -> {
                        if (PasswordHasher.matches("123456", existing.getPasswordSalt(), existing.getPasswordHash())) {
                            existing.setPasswordHash(PasswordHasher.hash("Teach2026", existing.getPasswordSalt()));
                            repository.save(existing);
                        }
                    });
                }
                return;
            }
            repository.save(teacher(1L, "teacher1", "李超", "TEACHER"));
            repository.save(teacher(2L, "assistant1", "周助教", "ASSISTANT"));
            repository.save(teacher(3L, "admin", "吴椿", "ADMIN"));
        };
    }

    private Teacher teacher(Long id, String account, String name, String role) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(id);
        teacher.setAccount(account);
        teacher.setDisplayName(name);
        teacher.setRole(role);
        teacher.setStatus("ACTIVE");
        String salt = PasswordHasher.newSalt();
        teacher.setPasswordSalt(salt);
        teacher.setPasswordHash(PasswordHasher.hash("Teach2026", salt));
        teacher.setCreatedAt(LocalDateTime.now());
        return teacher;
    }
}
