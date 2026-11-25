package com.swu.smartgrade.user.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.security.JwtService;
import com.swu.smartgrade.user.api.dto.CurrentUserResponse;
import com.swu.smartgrade.user.api.dto.LoginRequest;
import com.swu.smartgrade.user.api.dto.LoginResponse;
import com.swu.smartgrade.user.domain.Teacher;
import com.swu.smartgrade.user.domain.TeacherRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuthApplicationService {
    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;

    public AuthApplicationService(TeacherRepository teacherRepository, JwtService jwtService) {
        this.teacherRepository = teacherRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Teacher teacher = teacherRepository.findByAccount(request.username().trim())
                .orElseThrow(() -> BusinessException.unauthorized("账号或密码错误"));
        if (!"ACTIVE".equals(teacher.getStatus())
                || !PasswordHasher.matches(request.password(), teacher.getPasswordSalt(), teacher.getPasswordHash())) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        if (!teacher.getPasswordHash().startsWith("$2")) {
            teacher.setPasswordHash(PasswordHasher.hash(request.password(), teacher.getPasswordSalt()));
            teacherRepository.save(teacher);
        }
        String token = jwtService.issue(teacher.getTeacherId(), teacher.getAccount(),
                teacher.getDisplayName(), teacher.getRole());
        return new LoginResponse(token, teacher.getRole(), jwtService.expiresAt(token),
                teacher.getTeacherId(), teacher.getDisplayName());
    }

    public CurrentUserResponse me(Long teacherId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.unauthorized("请提供 Bearer 令牌");
        }
        Teacher teacher = find(teacherId);
        return new CurrentUserResponse(teacher.getTeacherId(), teacher.getDisplayName(),
                teacher.getAccount(), teacher.getRole(), teacher.getDisplayName(),
                jwtService.expiresAt(authorization.substring(7)));
    }

    public Teacher find(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> BusinessException.notFound("教师不存在"));
    }
}
