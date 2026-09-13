package com.swu.smartgrade.user;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.auth.AuthContext;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.JwtService;
import com.swu.smartgrade.common.auth.PasswordHasher;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.user.dto.LoginRequest;
import com.swu.smartgrade.user.dto.LoginResponse;
import com.swu.smartgrade.user.dto.RegisterRequest;
import com.swu.smartgrade.user.dto.UserView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务：注册、登录、按班级查人。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       ClassGroupRepository classGroupRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.classGroupRepository = classGroupRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserView register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(409, "用户名已存在：" + request.username());
        }
        Role role = request.role() == null ? Role.STUDENT : request.role();
        if (role == Role.ADMIN) {
            throw new BusinessException(403, "管理员账号不开放自助注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setRealName(request.realName());
        user.setRole(role);
        user.setClassId(request.classId());
        String salt = PasswordHasher.newSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(request.password(), salt));
        return toView(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码不正确"));
        if (!PasswordHasher.matches(request.password(), user.getPasswordSalt(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码不正确");
        }
        String token = jwtService.issue(user.getId(), user.getUsername(), user.getRealName(), user.getRole(), user.getClassId());
        return new LoginResponse(token, toView(user));
    }

    @Transactional(readOnly = true)
    public UserView currentUser() {
        return toView(requireUser(AuthContext.require().id()));
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在：" + userId));
    }

    @Transactional(readOnly = true)
    public List<User> listStudents(Long classId) {
        return userRepository.findByClassIdAndRole(classId, Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public UserView toView(Long userId) {
        return toView(requireUser(userId));
    }

    public UserView toView(User user) {
        String className = user.getClassId() == null ? null
                : classGroupRepository.findById(user.getClassId()).map(ClassGroup::getName).orElse(null);
        return new UserView(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole().name(), user.getClassId(), className);
    }

    public static CurrentUser current() {
        return AuthContext.require();
    }
}
