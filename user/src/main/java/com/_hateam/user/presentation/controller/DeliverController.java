package com._hateam.user.presentation.controller;

import com._hateam.common.dto.ResponseDto;
import com._hateam.user.application.dto.DeliverUserCreateReqDto;
import com._hateam.user.application.dto.DeliverUserUpdateReqDto;
import com._hateam.user.application.service.DeliverUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-users")
@RequiredArgsConstructor
public class DeliverController {
    private final DeliverUserService deliverUserService;

    //배송 담당자 추가
    @PostMapping("/add")
    public ResponseEntity<?> createDeliverUser(@RequestBody DeliverUserCreateReqDto deliverUserCreateReqDto) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.createDeliverUser(deliverUserCreateReqDto)));
    }


    // 검색(페이징추가, 권한별 분리)
    @GetMapping("/admin/search")
    public ResponseEntity<?> searchDeliverUsers(
            @RequestParam String name,@RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order, @PageableDefault(page = 0, size = 10) Pageable pageable,
            HttpServletRequest request) {
        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.searchDeliverUsersByName(name, userId, userRole, sortBy, order, pageable)));
    }

    // 배송 담당자 목록 조회(권한별 분리)
    @GetMapping("/")
    public ResponseEntity<?> getAllDeliverUsers(HttpServletRequest request) {

        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        // null 체크 추가
        if (userId == null || userRole == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("필수 헤더 값이 누락되었습니다."+ "x-user-id와 x-user-role 헤더가 필요합니다."+userId+" "+userRole);
        }
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.getAllDeliverUsers(userId,userRole)));
    }

    // 배송 담당자 단일 조회(권한별 분리)
    @GetMapping("/{deliverId}")
    public ResponseEntity<?> getDeliverUser(
            @PathVariable UUID deliverId,
            HttpServletRequest request) {
        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.getDeliverUserById(deliverId, userId, userRole)));
    }


    // 배송담당자 수정(관리자만)
    @PutMapping("/{deliverId}")
    public ResponseEntity<?> updateDeliverUser(
            @PathVariable UUID deliverId,
            @RequestBody DeliverUserUpdateReqDto updateDto,
            HttpServletRequest request) {
        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.updateDeliverUser(deliverId, updateDto, userId, userRole)));
    }

    // 배송담당자 삭제(관리자별)
    @DeleteMapping("/{deliverId}")
    public ResponseEntity<?> deleteDeliverUser(
            @PathVariable UUID deliverId,
            HttpServletRequest request) {
        String userId = request.getHeader("x-user-id");
        String userRole = request.getHeader("x-user-role");
        deliverUserService.deleteDeliverUser(deliverId, userId, userRole);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success("배송담당자 삭제 완료"));
    }


    @GetMapping("/delivery")//업체 소속 배달담당자 조회 Deliverer중 COM
    public ResponseEntity<?> getCompanyDeliver() {

        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(deliverUserService.getCompanyDeliver()));
    }


    @GetMapping("/hub-deliver")//허브 소속 배달담당자 조회 Deliverer중 HUB
    public ResponseEntity<?> getHubDeliver() {

        return ResponseEntity.status(HttpStatus.OK).body(
                ResponseDto.success(deliverUserService.getHubDeliver()));
    }

    // 배송 담당자 SlackId및 유저 조회
    @GetMapping("/{deliverId}/slack")
    public ResponseEntity<?> getDeliverSlackId(
            @PathVariable UUID deliverId) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.success(deliverUserService.getDeliverSlackUserById(deliverId)));
    }




}
