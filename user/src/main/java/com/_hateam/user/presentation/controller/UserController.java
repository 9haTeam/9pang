package com._hateam.user.presentation.controller;

import com._hateam.common.dto.ResponseDto;
import com._hateam.user.application.dto.RoleUpdateReqDto;
import com._hateam.user.application.dto.UserSignInReqDto;
import com._hateam.user.application.dto.UserSignUpReqDto;
import com._hateam.user.application.dto.UserUpdateReqDto;
import com._hateam.user.application.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

public class UserController {

    @Autowired
    private final UserService userService;

    @PostMapping("/signup")//회원가입
    public ResponseDto<?> addUser(@RequestBody @Valid UserSignUpReqDto signUpReqDto, BindingResult bindingResult) {
        return ResponseDto.success(HttpStatus.OK,userService.saveUser(signUpReqDto));
    }

    @GetMapping("/getuser")//자기자신 회원조회(내부에서 권한에따라)
    public ResponseEntity<?> getUser(HttpServletRequest request) {
        String myId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.getUser(Long.parseLong(myId),myId,userRole)));
    }


    @PutMapping("/roles/{userId}") // 권한 수정(ADMIN)
    public ResponseEntity<?> updateRole(@PathVariable Long userId, @RequestBody RoleUpdateReqDto roleUpdateDto,HttpServletRequest request) {
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(userService.updateUserRole(userId, roleUpdateDto.getRole(), userRole)));
    }

    @GetMapping("/search") // 유저 검색(내부에서 권한에 따라)
    public ResponseEntity<?> userSearch(@RequestParam String username, @RequestParam(defaultValue = "createdAt") String sortBy,
                                        @RequestParam(defaultValue = "desc") String order, @PageableDefault(page = 0, size = 10) Pageable pageable,
                                        HttpServletRequest request) {
        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");

        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(HttpStatus.OK, userService.searchUser(username,userRole,
                userId,sortBy,order,pageable)));
    }

    @GetMapping("/getusers")//회원목록 조회(ADMIN)
    public ResponseEntity<?> getAllUser(HttpServletRequest request){
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(HttpStatus.OK, userService.getAllUsers(userRole)));
    }

    @PutMapping("/{userId}")  // 회원 수정(ADMIN)
    public ResponseEntity<?> updateUser(@PathVariable String userId,@RequestBody @Valid UserUpdateReqDto userUpdateReqDto,HttpServletRequest request) {
        String userRole = request.getHeader("x-user-role");
        String userMyId = request.getHeader("x-user-id");
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(userUpdateReqDto, userMyId, Long.parseLong(userId), userRole));
    }

    @DeleteMapping("/{userId}")  // 회원 탈퇴(ADMIN)
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        String userRole = request.getHeader("x-user-role");
        String userMyId = request.getHeader("x-user-id");
        userService.deleteUser(userMyId, userId,userRole);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(HttpStatus.OK, "회원 탈퇴가 성공적으로 처리되었습니다."));
    }

    @GetMapping("/hub-admin")//허브의 관리자조회(Feign, ROLE=HUB)(
    public ResponseEntity<?> getHubAdmin() {

        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.getHub()));
    }

    @GetMapping("/company-admin")//업체 관리자 조회(Feign,ROLE=COMPANY)
    public ResponseEntity<?> getCompanyAdmin() {

        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.getCompany()));
    }

    @GetMapping("/{userId}/message")//사용자정보조회(Feign)
    public ResponseEntity<?> getUserByMessage(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.getUserByFeign(userId)));
    }


    @GetMapping("/verify/signin")//이름으로아이디,패스워드조회(Feign)
    public ResponseEntity<?> findByUsername(@RequestParam String username) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.verifyUserFeign(username)));
    }
    // userId는 외부로 나가면 안되는값, username을 대신 userid 처럼 사용
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(userService.getUserByUsername(username)));
    }


}
