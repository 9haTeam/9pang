package com._hateam.user.application.service;

import com._hateam.user.application.dto.*;
import com._hateam.user.domain.enums.UserRole;
import com._hateam.user.domain.model.DeliverUser;
import com._hateam.user.domain.model.User;
import com._hateam.user.domain.repository.DeliverUserRepository;
import com._hateam.user.domain.repository.UserRepository;
import com._hateam.user.application.exception.CustomException;
import com._hateam.user.infrastructure.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DeliverUserRepository deliverUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserResponseDto saveUser(UserSignUpReqDto userSignUpReqDto) {
        User savedUser =userRepository.save(UserSignUpReqDto.toEntity(userSignUpReqDto,passwordEncoder));
        savedUser.setCreatedAt(LocalDateTime.now());
        return UserResponseDto.from(savedUser);
    }

    @Transactional
    public AuthResponseDto authenticateUser(UserSignInReqDto signInReqDto) {//로그인
        try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    signInReqDto.getUsername(),
                    signInReqDto.getPassword()
            )
        );
        }catch (Exception e) {
        throw new CustomException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        // 토큰에 값 담기위해 값 조회
        User user = userRepository.findByUsername(signInReqDto.getUsername())
                .orElseThrow(() -> new CustomException("사용자가 존재하지 않습니다: " + signInReqDto.getUsername()));

        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getUserRoles().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        // 응답값 반환
        return AuthResponseDto.builder()
                .accessToken("Bearer " + accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .role(user.getUserRoles().name())
                .build();
    }

    @Override
    public List<UserResponseDto> getAllUsers(String userRole) {
        // 권한검증
        if(userRole.equals("COMPANY")) {
            throw new CustomException("해당 권한으로는 사용할 수 없습니다.");
        }
        List<User> users = userRepository.findAllByDeletedAtIsNull();

        return users.stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());

    }

    @Override
    public UserResponseDto getUser(Long userId, String myId, String userRole){//추후 권한에 따라
        User user;
        if(userRole.equals("ADMIN")) {
        user =userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다 " + userId));
        }
        else {
            user =userRepository.findById(Long.parseLong(myId))
                    .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다 " + userId));
        }

        return UserResponseDto.from(user);
    }


    @Transactional
    @Override
    public UserResponseDto updateUser(UserUpdateReqDto userUpdateReqDto,String userMyId, Long userId, String userRole) {
        // 권한검증
        if(userRole.equals("ADMIN")) {

        }

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));

        // 업데이트할 필드들만 설정
        if (userUpdateReqDto.getNickname() != null) {
            existingUser.setNickname(userUpdateReqDto.getNickname());
        }
        if (userUpdateReqDto.getSlackId() != null) {
            existingUser.setSlackId(userUpdateReqDto.getSlackId());
        }
        if (userUpdateReqDto.getHubId() != null) {
            existingUser.setHubId(userUpdateReqDto.getHubId());
        }

        User updatedUser = userRepository.save(existingUser);
        updatedUser.setUpdatedBy(userMyId);
        updatedUser.setUpdatedAt(LocalDateTime.now());
        return UserResponseDto.from(updatedUser);
    }

    @Transactional
    @Override
    public void deleteUser(String userMyId, Long userId, String userRole) {

        // ADMIN 권한이 있을 때 실행할 코드
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));

        Optional<DeliverUser> optionalDeliverUser = deliverUserRepository.findByUser_UserId(userId);
        optionalDeliverUser.ifPresent(deliverUser -> {
            deliverUser.setDeletedBy(userMyId);
            deliverUser.setDeletedAt(LocalDateTime.now());
        });

        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(user.getUsername());
        userRepository.save(user);

    }

    @Override
    public Page<UserResponseDto> searchUser(String username, String userRole, String userId,
                                            String sortBy, String order, Pageable pageable) {
        // 페이지 사이즈 적용
        if(pageable.getPageSize() != 10 && pageable.getPageSize() != 20 && pageable.getPageSize() != 30) {
            pageable = PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
        }

        // 정렬 설정 적용
        Sort sort = Sort.by(order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        // PageRequest에 정렬 적용
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<User> userPage;

        // 권한별 처리
        if (userRole.equals("ADMIN")) {
            // 관리자는 모든 사용자 검색 가능
            userPage = userRepository.findByUsernameContainingAndDeletedAtIsNull(username, pageRequest);
        } else {

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다: " + username));
            // 일반 사용자(COMPANY, HUB, DELIVERY)는 자신의 정보만 조회 가능
            if (!user.getUserId().equals(Long.parseLong(userId))) {
                throw new CustomException("본인의 아이디만 조회가능합니다.");
            }


            // 단일 사용자를 Page 객체로 변환
            userPage = new PageImpl<>(List.of(user), pageRequest, 1);
        }

        // User 엔티티를 UserResponseDto로 변환
        return userPage.map(UserResponseDto::from);
    }

    @Transactional
    @Override
    public UserResponseDto updateUserRole(Long userId, UserRole role, String userRole) {
//        // 권한검증
//        if(!userRole.equals("ADMIN")) {
//            throw new CustomForbiddenException("관리자 권한이 필요합니다.");
//        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다: " + userId));

        user.setUserRoles(role);
        User updatedUser = userRepository.save(user);
        return UserResponseDto.from(updatedUser);

    }

    @Override
    public List<FeignCompanyAdminResDto> getCompany() {
        List<User> companyUsers = userRepository.findAllByUserRolesAndDeletedAtIsNull(UserRole.COMPANY);

        return companyUsers.stream()
                .map(FeignCompanyAdminResDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeignHubAdminResDto> getHub() {
        List<User> hubUsers = userRepository.findAllByUserRolesAndDeletedAtIsNull(UserRole.HUB);

        return hubUsers.stream()
                .map(FeignHubAdminResDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public FeignUserResDto getUserByFeign(Long userId){//추후 권한에 따라
        User user =userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다 " + userId));
        return FeignUserResDto.from(user);
    }

    @Override
    public FeignVerifyResDto verifyUserFeign(String username){//추후 권한에 따라

        User user =userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다!! " + username));

        return FeignVerifyResDto.from(user);
    }

    @Override
    public UserResponseDto getUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다 " + username));
        return UserResponseDto.from(user);
    }

}
