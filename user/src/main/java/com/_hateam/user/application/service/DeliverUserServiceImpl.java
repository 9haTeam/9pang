package com._hateam.user.application.service;

import com._hateam.common.dto.ResponseDto;
import com._hateam.common.exception.CustomNotFoundException;
import com._hateam.user.application.dto.*;
import com._hateam.user.domain.enums.DeliverType;
import com._hateam.user.domain.enums.Status;
import com._hateam.user.domain.model.DeliverAssignPointer;
import com._hateam.user.domain.model.DeliverUser;
import com._hateam.user.application.exception.CustomException;
import com._hateam.user.infrastructure.feign.HubClient;
import com._hateam.user.infrastructure.repository.DeliverAssignPointerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com._hateam.user.domain.model.User;
import com._hateam.user.domain.repository.DeliverUserRepository;
import com._hateam.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliverUserServiceImpl implements DeliverUserService {

    private final DeliverUserRepository deliverUserRepository;
    private final DeliverAssignPointerRepository pointerRepository;
    private final UserRepository userRepository;
    private final HubClient hubClient;

@Override//권한별 담당자 검색
@Transactional(readOnly = true)
public Page<DeliverUserResponseDto> searchDeliverUsersByName(String name, String userId, String userRole, String sortBy, String order, Pageable pageable) {

    Page<DeliverUser> deliverUserPage;
    //페이지 사이즈 적용
    if(pageable.getPageSize()!=10&&pageable.getPageSize()!=20&&pageable.getPageSize()!=30){
        pageable = PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
    }
    // 정렬 설정 적용
    Sort sort = Sort.by(order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
    // PageRequest에 정렬 적용
    PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

    // 권한 검증 및 권한별 처리
    if (userRole.equals("ADMIN")) {
        // 마스터 관리자: 모든 배송담당자 검색 가능
        deliverUserPage = deliverUserRepository.findByNameContainingAndDeletedAtIsNull(name, pageRequest);
    }
    else if (userRole.equals("HUB")) {

        // 허브관리자는 유저에서 추가,허브 관리자는 자신의 HubId를 불러와서 자신의 HubId 가지고 잇는 배송 담당자 검색
        // 애들 검색
        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new CustomNotFoundException("배송담당자 정보를 찾을 수 없습니다(H). ID: " + Long.parseLong(userId)));

        UUID hubId = user.getHubId();
        deliverUserPage = deliverUserRepository.findByNameContainingAndHubIdAndDeletedAtIsNull(name, hubId, pageRequest);
    }
    else if (userRole.equals("DELIVERY")) {
        // 배송 담당자: 본인 정보만 조회 가능(이름이 일치하는 경우만)
        DeliverUser deliverUser = deliverUserRepository.findByUser_UserId(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException("배송담당자 정보를 찾을 수 없습니다(D). ID: " + userId));

        // 자신의 이름이 검색어를 포함하는 경우에만 결과 반환
        if (deliverUser.getName().contains(name)) {
            // 단일 객체를 페이지로 변환
            deliverUserPage = new PageImpl<>(List.of(deliverUser), pageRequest, 1);
        } else {
            deliverUserPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);
        }
    }
    else { // COMPANY의 경우
        throw new CustomException("배송담당자 정보에 접근할 권한이 없습니다.");
    }

    return deliverUserPage.map(DeliverUserResponseDto::from);
}
    // 배송담당자 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<DeliverUserResponseDto> getAllDeliverUsers(String userId, String userRole) {
        // 권한 검증
        DeliverUser searchMan;
        List<DeliverUser> deliverUsers;

        if(userRole.equals("ADMIN")) {
            deliverUsers = deliverUserRepository.findByDeletedAtIsNull();
        }
        //ADMIN 은 전부조회
        else if(userRole.equals("HUB")){
            searchMan= deliverUserRepository.findByUser_UserId(Long.parseLong(userId)).orElseThrow(
                    () -> new CustomException("허브 관리자 정보를 찾을 수 없습니다. ID를 등록해주세요 " + Long.parseLong(userId)));
            UUID hubId = searchMan.getHubId(); // 관리자의 허브 ID
            deliverUsers = deliverUserRepository.findByHubIdAndDeletedAtIsNull(hubId);

        }
        else {
            throw new CustomException("배송담당자 정보에 접근할 권한이 없습니다. Delivery는 단일조회를 이용해주세요");
        }

        return deliverUsers.stream()
                .map(DeliverUserResponseDto::from)
                .collect(Collectors.toList());
    }

    // 단일 조회
    @Override
    @Transactional(readOnly = true)
    public DeliverUserResponseDto getDeliverUserById(UUID deliverId, String userId, String userRole) {
        // 권한 검증
        DeliverUser deliverUser = deliverUserRepository.findByDeliverId(deliverId)
                .orElseThrow(() -> new CustomException("배송담당자 정보를 찾을 수 없습니다(d). ID: " + deliverId));
        DeliverUser searchMan = deliverUserRepository.findByUser_UserId(Long.parseLong(userId)).orElseThrow(
                () -> new CustomException("배송담당자가 아니기 때문에 조회불가합니다. ID: " + Long.parseLong(userId))
        );

        //ADMIN 은 전부조회
        if(userRole.equals("HUB")){
            if (!deliverUser.getHubId().equals(searchMan.getHubId())) {//조회자의hubid와 나의 hubid
                throw new CustomException("본인 소속 허브의 배송담당자만 조회할 수 있습니다.");
            }
        }
        else if(userRole.equals("DELIVERY")){
            if (!deliverUser.getUser().getUserId().equals(Long.parseLong(userId))) {
                throw new CustomException("본인의 정보만 조회할 수 있습니다.");}
        }
        else if (userRole.equals("COMPANY")) {
            throw new CustomException("배송담당자 정보에 접근할 권한이 없습니다.");
        }

        if (deliverUser.getDeletedAt() != null) {
            throw new CustomException("삭제된 배송담당자입니다.");
        }

        return DeliverUserResponseDto.from(deliverUser);
    }


    // 배송 담당자 수정
    @Override
    @Transactional
    public DeliverUserResponseDto updateDeliverUser(UUID deliverId, DeliverUserUpdateReqDto updateDto, String userId, String userRole) {
        // 권한 검증
     //   System.out.println(userPrincipals.getRole()+"관리확인");
//        if (!userRole.equals("ADMIN")) {
//            throw new CustomForbiddenException("관리자 권한이 필요합니다!.");
//        }

        DeliverUser deliverUser = deliverUserRepository.findByDeliverId(deliverId)
                .orElseThrow(() -> new CustomException("수정하려는 배송담당자 정보를 찾을 수 없습니다. ID: " + deliverId));

        if (deliverUser.getDeletedAt() != null) {
            throw new CustomException("삭제된 배송담당자입니다.");
        }

        // 필드 업데이트
        if (updateDto.getHubId() != null) {
            deliverUser.setHubId(updateDto.getHubId());
        }
        if (updateDto.getSlackId() != null) {
            deliverUser.setSlackId(updateDto.getSlackId());
        }
        if (updateDto.getName() != null) {
            deliverUser.setName(updateDto.getName());
        }
        if (updateDto.getDeliverType() != null) {
            deliverUser.setDeliverType(updateDto.getDeliverType());
        }
        if (updateDto.getContactNumber() != null) {
            deliverUser.setContactNumber(updateDto.getContactNumber());
        }
        if (updateDto.getRotationOrder() != null) {
            deliverUser.setRotationOrder(updateDto.getRotationOrder());
        }
        if (updateDto.getStatus() != null) {
            deliverUser.setStatus(updateDto.getStatus());
        }

        deliverUser.setUpdatedBy(userId);
        deliverUser.setUpdatedAt(LocalDateTime.now());

        DeliverUser updatedDeliverUser = deliverUserRepository.save(deliverUser);

        return DeliverUserResponseDto.from(updatedDeliverUser);
    }

    // 배송 담당자 삭제
    @Override
    @Transactional
    public void deleteDeliverUser(UUID deliverId, String userId, String userRole) {
        // 권한 검증
//        if (userRole.equals("ADMIN")) {
//            throw new CustomForbiddenException("관리자 권한이 필요합니다.");
//        }

        DeliverUser deliverUser = deliverUserRepository.findByDeliverId(deliverId)
                .orElseThrow(() -> new CustomException("삭제하려는 배송담당자 정보를 찾을 수 없습니다. ID: " + deliverId));

        if (deliverUser.getDeletedAt() != null) {
            throw new CustomException("이미 삭제된 배송담당자입니다.");
        }

        // 논리적 삭제 처리
        deliverUser.setDeletedAt(LocalDateTime.now());
        deliverUser.setDeletedBy(userId);
        deliverUserRepository.save(deliverUser);

        // 관련 User 엔티티 업데이트
        User user = deliverUser.getUser();
        if (user != null) {
            user.setDeliver(false);
            userRepository.save(user);
        }
    }


    //업체 배송담당자목록 조회(Feign)
    @Override
    public List<FeignInCompanyDeliverResDto> getCompanyDeliver() {
        List<DeliverUser> deliverUser = deliverUserRepository.findByDeliverTypeAndDeletedAtIsNull(DeliverType.DELIVER_COMPANY);
        return deliverUser.stream().map(FeignInCompanyDeliverResDto::from).collect(Collectors.toList());
    }

    //허브 배송담당자목록 조회(Feign)
    @Override
    public List<FeignInHubDeliverResDto> getHubDeliver() {
        List<DeliverUser> deliverUser = deliverUserRepository.findByDeliverTypeAndDeletedAtIsNull(DeliverType.DELIVER_HUB);
        return deliverUser.stream().map(FeignInHubDeliverResDto::from).collect(Collectors.toList());
    }

    // 배송담당자 슬랙 ID조회(Feign)
    @Override
    @Transactional(readOnly = true)
    public FeignDeliverSlackIdResDto getDeliverSlackUserById(UUID deliverId) {
        // 권한 검증
        DeliverUser deliverUser = deliverUserRepository.findByDeliverId(deliverId)
                .orElseThrow(() -> new CustomException("배송담당자 정보를 찾을 수 없습니다(d). ID: " + deliverId));

        return FeignDeliverSlackIdResDto.from(deliverUser);
    }


    /**
     * (deliverType, hubId)를 기준으로,
     * - HUB이면 hubId가 null이 될 수도 있음
     * - COMPANY이면 반드시 hubId가 존재
     * 1) (deliverType, hubId)에 해당하는 담당자 목록 조회
     * 2) Pointer에서 "마지막 배정된 rotationOrder" 읽어옴
     * 3) 그 다음 순번을 찾아서 배정
     * 4) pointer 업데이트 & 결과 반환
     */
    public UUID assignNextDeliverUser(DeliverType deliverType, UUID hubId) {
        // 1) (deliverType, hubId)에 맞는 ACTIVE 담당자 목록 조회
        List<DeliverUser> users = getActiveDeliverUsers(deliverType, hubId);
        if (users.isEmpty()) {
            throw new CustomException("배정 가능한 ACTIVE 상태의 배송담당자가 없습니다. " +
                    "[deliverType=" + deliverType + ", hubId=" + hubId + "]");
        }

        // 2) pointer 조회 (없으면 신규 생성)
        DeliverAssignPointer pointer = pointerRepository.findByDeliverTypeAndHubId(deliverType, hubId)
                .orElseGet(() -> {
                    // 처음엔 lastAssignedRotationOrder를 -1 등으로 초기화
                    return pointerRepository.save(
                            DeliverAssignPointer.builder()
                                    .deliverType(deliverType)
                                    .hubId(hubId)
                                    .lastAssignedRotationOrder(-1)
                                    .build()
                    );
                });

        int lastAssigned = pointer.getLastAssignedRotationOrder();

        // 3) 다음 rotationOrder를 가진 담당자를 찾는다.
        //    “lastAssigned”보다 큰 rotationOrder 중 가장 작은 값 → 없으면 리스트의 첫 번째
        DeliverUser nextUser = users.stream()
                .filter(u -> u.getRotationOrder() > lastAssigned)
                .findFirst()
                .orElse(users.get(0)); // 못 찾으면 첫 번째

        // 4) pointer 갱신 & 저장
        pointer.setLastAssignedRotationOrder(nextUser.getRotationOrder());
        pointerRepository.save(pointer);

        return nextUser.getDeliverId();
    }

    /**
     * deliverType / hubId 조합에 따라 ACTIVE인 배송담당자들을 rotationOrder asc 로 조회
     */
    private List<DeliverUser> getActiveDeliverUsers(DeliverType deliverType, UUID hubId) {
        if (deliverType == DeliverType.DELIVER_HUB) {
            // 허브 담당자 → hubId가 없는(또는 의미 없는) 10명
            return deliverUserRepository.findByStatusAndDeliverTypeOrderByRotationOrderAsc(
                    Status.ACTIVE, DeliverType.DELIVER_HUB
            );
        } else {
            // COMPANY → 업체 담당자. hubId가 반드시 있어야 함
            return deliverUserRepository.findByStatusAndDeliverTypeAndHubIdOrderByRotationOrderAsc(
                    Status.ACTIVE, DeliverType.DELIVER_COMPANY, hubId
            );
        }
    }

    @Override
    @Transactional
    public DeliverUserResponseDto createDeliverUser(DeliverUserCreateReqDto deliverUserCreateReqDto) {
        // 1) User 존재 검사
        User user = userRepository.findById(deliverUserCreateReqDto.getUserId())
                .orElseThrow(() -> new CustomException("등록하려는 사용자가 존재하지 않습니다. "));

        if(user.getUserRoles().name().equals("DELIVER_COMPANY")) { //소속허브가 존재하지 않을경우 예외발생
            ResponseDto<FeignHubDto> response = hubClient.getHub(user.getHubId());
            if (response.getData() == null) {
                throw new CustomException("해당 허브가 존재하지 않습니다.");
            }
        }

        // 2) (deliverType, hubId)로 이미 등록된 DeliverUser들 중 가장 큰 rotationOrder 찾기
        DeliverType deliverType = deliverUserCreateReqDto.getDeliverType();
        UUID hubId = deliverUserCreateReqDto.getHubId(); // DELIVER_HUB인 경우 null일 수도 있음
        Integer maxRotation = deliverUserRepository.findMaxRotationOrder(deliverType, hubId);

        // 3) 없으면 -1로 보고, +1 해서 새 담당자 rotationOrder 결정
        int newRotation = (maxRotation == null ? -1 : maxRotation) + 1;

        // 4) Entity 생성
        DeliverUser deliverUser = DeliverUser.builder()
                .deliverId(UUID.randomUUID())
                .user(user)
                .hubId(hubId)
                .slackId(deliverUserCreateReqDto.getSlackId())
                .name(deliverUserCreateReqDto.getName())
                .deliverType(deliverType)
                .contactNumber(deliverUserCreateReqDto.getContactNumber())
                .status(deliverUserCreateReqDto.getStatus())
                .rotationOrder(newRotation)   // <-- 여기서 반영
                .build();

        // 5) 저장
        DeliverUser savedDeliverUser = deliverUserRepository.save(deliverUser);

        // 6) User 엔티티에 “배송담당자 여부” 세팅
        user.setDeliver(true);
        userRepository.save(user);

        return DeliverUserResponseDto.from(savedDeliverUser);
    }



}
