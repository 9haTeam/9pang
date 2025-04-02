package com._hateam.entity;

import com._hateam.CompanyType;
import com._hateam.common.entity.Timestamped;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통한 생성만 허용
@Builder
@Table(name = "p_company")
public class Company extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false)
    private UUID id;

    // 소속 허브 id (필요한 경우 별도 매핑)
    @Column(name = "hub_id", nullable = false, length = 50)
    private UUID hubId;

    // 관리자 id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "company_name", nullable = false, length = 255, unique = true)
    private String name;

    @Column(name = "company_address", nullable = false, length = 20)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 20)
    private CompanyType companyType;

    @Column(name = "postal_code", nullable = false, length = 5)
    private String postalCode;

    // One-to-Many 관계: 하나의 Company는 여러 Product를 가질 수 있음 (Products는 선택적임)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // 양방향 연관관계 관리 편의 메소드
    public void addProduct(Product product) {
        products.add(product);
        product.setCompany(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCompany(null);
    }
}
