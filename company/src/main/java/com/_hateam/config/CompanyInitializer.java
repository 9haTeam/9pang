package com._hateam.config;


import com._hateam.CompanyType;
import com._hateam.common.dto.ResponseDto;
import com._hateam.dto.HubDto;
import com._hateam.entity.Company;
import com._hateam.entity.Hub;
import com._hateam.entity.Product;
import com._hateam.feign.HubController;
import com._hateam.repository.CompanyRepository;
import com._hateam.repository.HubRepository;
import com._hateam.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyInitializer {

    private final HubRepository hubRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final HubController hubController;

    public List<UUID> getRandomHubIds(int count) {
        log.info("getRandomHubIds Method Start");
        ResponseEntity<ResponseDto<List<HubDto>>> response =
                hubController.getAllHubs(0, 10, "createdAt", false); // 모든 허브를 가져옴

        if (response.getBody() == null
                || response.getBody().getData() == null
                || response.getBody().getData().isEmpty()) {
            throw new IllegalStateException("No hubs available in DB");
        }

        List<HubDto> hubDtos = response.getBody().getData();
        log.info("hubDtos : " + hubDtos);

        // HubDto를 Hub 엔티티로 변환 (필요에 따라 변환 로직 구현)
        List<Hub> hubs = new java.util.ArrayList<>(hubDtos.stream()
                .map(HubDto::hubToHubDto) // HubDto를 Hub로 변환하는 메서드
                .toList());

        if (hubs.size() < count) {
            throw new IllegalStateException("Not enough hubs available in DB");
        }

        // 리스트를 섞은 후 앞의 count 개를 선택
        Collections.shuffle(hubs);
        return hubs.stream()
                .limit(count)
                .map(Hub::getId)
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initTestData() {
        // 한 번 호출하여 3개의 랜덤 허브 ID를 가져옴
        List<UUID> hubIds = getRandomHubIds(3);
        // 각 허브에 소속된 회사 생성
        Company company1 = Company.builder()
                .hubId(hubIds.get(0))
                .userId("admin1")
                .name("서울회사1")
                .address("서울특별시 강남구 역삼동")
                .companyType(CompanyType.PRODUCE) // 예시 값
                .postalCode("12345")
                .build();
        company1 = companyRepository.save(company1);

        Company company2 = Company.builder()
                .hubId(hubIds.get(1))
                .userId("admin2")
                .name("서울회사2")
                .address("서울특별시 종로구")
                .companyType(CompanyType.RECEIVE)
                .postalCode("54321")
                .build();
        company2 = companyRepository.save(company2);

        Company company3 = Company.builder()
                .hubId(hubIds.get(2))
                .userId("admin3")
                .name("부산회사1")
                .address("부산광역시 해운대구")
                .companyType(CompanyType.RECEIVE)
                .postalCode("11111")
                .build();
        company3 = companyRepository.save(company3);

        // 각 회사에 제품 생성
        Product product1 = Product.builder()
                .company(company1)
                .name("제품1")
                .quantity(100)
                .description("서울회사1의 제품1")
                .price(1000)
                .build();
        productRepository.save(product1);

        Product product2 = Product.builder()
                .company(company1)
                .name("제품2")
                .quantity(200)
                .description("서울회사1의 제품2")
                .price(2000)
                .build();
        productRepository.save(product2);

        Product product3 = Product.builder()
                .company(company2)
                .name("제품3")
                .quantity(150)
                .description("서울회사2의 제품3")
                .price(1500)
                .build();
        productRepository.save(product3);

        Product product4 = Product.builder()
                .company(company3)
                .name("제품4")
                .quantity(300)
                .description("부산회사1의 제품4")
                .price(3000)
                .build();
        productRepository.save(product4);

        log.info("Test data initialized: {} hubs, {} companies, {} products",
                hubRepository.count(), companyRepository.count(), productRepository.count());
    }
}

