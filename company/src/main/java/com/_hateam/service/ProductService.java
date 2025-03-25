package com._hateam.service;

import com._hateam.common.dto.ResponseDto;
import com._hateam.dto.HubDto;
import com._hateam.dto.ProductDto;
import com._hateam.dto.ProductRequestDto;
import com._hateam.entity.Company;
import com._hateam.entity.Hub;
import com._hateam.entity.Product;
import com._hateam.feign.HubController;
import com._hateam.repository.CompanyRepository;
import com._hateam.repository.HubRepository;
import com._hateam.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository; // Product의 소속 Company를 조회하기 위해
    private final HubController hubController;
    private final HubRepository hubRepository;


    @Transactional
    public ProductDto createProduct(ProductRequestDto requestDto) {
        // 중복 상품 체크
        validateDuplicateProduct(requestDto);
        // 소속 Company 존재 여부 검증
        validateCompanyExists(requestDto.getCompanyId());
        Product product = createProductEntity(requestDto);
        productRepository.save(product);
        return ProductDto.productToProductDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts(int page, int size, String sortBy, boolean isAsc) {
        if (size != 10 && size != 30 && size != 50) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(isAsc ? Sort.Direction.ASC : Sort.Direction.DESC,
                        sortBy.equals("updatedAt") ? "updatedAt" : "createdAt"));
        List<Product> productList = productRepository.findAll(pageable).getContent();
        return productList.stream()
                .map(ProductDto::productToProductDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID id) {
        Product product = findProduct(id);
        return ProductDto.productToProductDto(product);
    }

    public List<ProductDto> getProductsByHubId(UUID hubId) {
        // 기존 로직 그대로 유지 (회사의 소속 허브에 따라 제품을 조회)
        List<Company> companies = companyRepository.findByHubIdAndDeletedAtIsNull(hubId);
        if (companies == null || companies.isEmpty()) {
            return new ArrayList<>();
        }
        List<Product> allProducts = new ArrayList<>();
        for (Company company : companies) {
            allProducts.addAll(company.getProducts());
        }
        return allProducts.stream()
                .map(ProductDto::productToProductDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateProduct(UUID id, ProductRequestDto requestDto) {
        Product product = findProduct(id);

        // 만약 상품 이름이 변경되면 중복 체크 (필요 시)
        if (requestDto.getName() != null && !requestDto.getName().equals(product.getName())) {
            validateDuplicateProduct(requestDto);
        }
        // 소속 Company가 변경되었으면 존재 여부 검증 후 업데이트
        if (requestDto.getCompanyId() != null &&
                !requestDto.getCompanyId().equals(product.getCompany().getId())) {
            validateCompanyExists(requestDto.getCompanyId());
            Company newCompany = companyRepository.findById(requestDto.getCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + requestDto.getCompanyId()));
            product.setCompany(newCompany);
        }

        // 나머지 필드 업데이트
        product.setName(requestDto.getName());
        product.setQuantity(requestDto.getQuantity());
        product.setDescription(requestDto.getDescription());
        product.setPrice(requestDto.getPrice());

        return ProductDto.productToProductDto(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    // 내부 메소드들

    private Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    private Product createProductEntity(ProductRequestDto requestDto) {
        // 소속 Company 존재 여부 검증 (이미 처리됨)
        Company company = companyRepository.findById(requestDto.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + requestDto.getCompanyId()));
        validateHubExists(company.getHubId());
        return Product.builder()
                .company(company)
                .name(requestDto.getName())
                .quantity(requestDto.getQuantity())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .build();
    }

    // 중복 상품 체크: 동일한 이름의 상품이 존재하면 예외 발생
    private void validateDuplicateProduct(ProductRequestDto requestDto) {
        productRepository.findByNameAndDeletedAtIsNull(requestDto.getName())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("중복된 상품이 존재합니다.");
                });
    }

    /**
     * 소속 Company의 존재 여부를 검증합니다.
     *
     * @param companyId 검증할 회사 ID
     */
    private void validateCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new EntityNotFoundException("Company not found with id: " + companyId);
        }
    }

    private void validateHubExists(UUID hubId) {
//        ResponseEntity<ResponseDto<HubDto>> response = hubController.getHub(hubId);
//        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().getData() == null) {
//            throw new EntityNotFoundException("관리 허브가 존재하지 않습니다. hubId: " + hubId);
//        }
//    }
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new EntityNotFoundException("관리 허브가 존재하지 않습니다. hubId: " + hubId));
    }
}
