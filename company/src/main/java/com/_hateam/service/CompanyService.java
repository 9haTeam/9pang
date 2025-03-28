package com._hateam.service;

import com._hateam.dto.CompanyDto;
import com._hateam.dto.CompanyRequestDto;
import com._hateam.entity.Company;
import com._hateam.entity.Hub;
import com._hateam.feign.HubController;
import com._hateam.repository.CompanyRepository;
import com._hateam.repository.HubRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com._hateam.dto.CompanyDto.companyToCompanyDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final HubController hubController; // Feign Client로 허브 조회
    private final HubRepository hubRepository;

    @Transactional
    public CompanyDto createCompany(CompanyRequestDto requestDto) {
        // 관리 허브 존재 여부 검증
        validateHubExists(requestDto.getHubId());
        // 중복 회사 존재 여부 검증
        validateDuplicateCompany(requestDto);
        Company company = createCompanyEntity(requestDto);
        companyRepository.save(company);
        return companyToCompanyDto(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies(int page, int size, String sortBy, boolean isAsc) {
        // 페이지 사이즈가 10, 30, 50 외의 값이면 기본 10으로 설정
        if (size != 10 && size != 30 && size != 50) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(isAsc ? Sort.Direction.ASC : Sort.Direction.DESC,
                        sortBy.equals("updatedAt") ? "updatedAt" : "createdAt"));
        List<Company> companyList = companyRepository.findAll(pageable).getContent();
        for (Company company : companyList) {
            log.info("Finding Company Id : " + company.getId().toString());
        }
        return companyList.stream()
                .map(CompanyDto::companyToCompanyDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyDto getCompany(UUID id) {
        Company company = findCompany(id);
        return companyToCompanyDto(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> getCompaniesByHubId(UUID hubId, int page, int size, String sortBy, boolean isAsc) {
        // 페이지 사이즈가 10, 30, 50이 아니면 기본 10으로 설정
        if (size != 10 && size != 30 && size != 50) {
            size = 10;
        }
        // 정렬 기준: sortBy가 "updatedAt"이면 updatedAt, 그 외는 createdAt
        Sort sort = Sort.by(isAsc ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy.equals("updatedAt") ? "updatedAt" : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        // 해당 hubId를 가진 회사 목록을 페이지 단위로 조회
        List<Company> companyList = companyRepository.findByHubIdAndDeletedAtIsNull(hubId, pageable).getContent();
        return companyList.stream()
                .map(CompanyDto::companyToCompanyDto)
                .collect(Collectors.toList());
    }

    public CompanyDto getCompanyByCompanyIdAndHubId(UUID companyId, UUID hubId) {
        Company company = companyRepository.findByIdAndHubIdAndDeletedAtIsNull(companyId, hubId);
        if (company == null) {
            throw new EntityNotFoundException();
        }
        return companyToCompanyDto(company);
    }


    @Transactional
    public CompanyDto updateCompany(UUID id, CompanyRequestDto requestDto) {
        Company company = findCompany(id);
        // 회사 이름이 변경되면 중복 검증 수행
        if (!company.getName().equals(requestDto.getCompanyName())) {
            validateDuplicateCompany(requestDto);
        }
        // 관리 허브 존재 여부 검증
        validateHubExists(requestDto.getHubId());
        updateCompanyInfo(company, requestDto);
        return CompanyDto.companyToCompanyDto(company);
    }

    @Transactional
    public void deleteCompany(UUID id) {
        Company company = findCompany(id);
        companyRepository.delete(company);
    }

    // 내부 메소드들

    private Company findCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with id: " + id));
    }

    private Company createCompanyEntity(CompanyRequestDto requestDto) {
        return Company.builder()
                .hubId(requestDto.getHubId())
                .userId(requestDto.getUserId())
                .name(requestDto.getCompanyName())
                .address(requestDto.getCompanyAddress())
                .companyType(requestDto.getCompanyType())
                .postalCode(requestDto.getPostalCode())
                .build();
    }

    private void updateCompanyInfo(Company company, CompanyRequestDto requestDto) {
        company.setHubId(requestDto.getHubId());
        company.setUserId(requestDto.getUserId());
        company.setName(requestDto.getCompanyName());
        company.setAddress(requestDto.getCompanyAddress());
        company.setCompanyType(requestDto.getCompanyType());
        company.setPostalCode(requestDto.getPostalCode());
    }

    private void validateDuplicateCompany(CompanyRequestDto requestDto) {
        companyRepository.findByNameAndDeletedAtIsNull(requestDto.getCompanyName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("중복된 회사가 존재합니다.");
                });
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
