# 스파르타 물류 - MSA 기반 국내 물류 관리 및 배송 시스템

## 📌 프로젝트 목적 및 개요

본 프로젝트의 목적은 기업 간 거래(B2B)를 지원하는 국내 물류 관리 및 배송 시스템을 개발하는 것입니다. 마이크로서비스 아키텍처(MSA)를 사용하여 각 서비스의 독립적인 개발과 관리가 가능하도록 설계하고 구현합니다. 스파르타 물류 시스템은 각 지역에 물류 허브를 가지고 있으며, 각 허브는 주문 관리, 재고 관리, 물류 운영을 독립적으로 수행합니다. 기업의 상품은 지역 허브에서 관리되며, 배송 요청이 발생하면 해당 허브 간 물품 이동 및 최종 목적지 업체로의 배송이 이루어집니다.

## 📌 팀원 및 역할

| 이름 | 역할 |
|------|------|
| 유남규 | 허브와 허브 경로 관리, 허브 경로 알고리즘, 업체 관리, 상품 관리, Docker + Eureka 로 MSA 구성 |
| 손민주 | 주문 관리, 슬랙 메시지 관리, Kafka 이벤트 처리, 공통 모듈 생성 |
| 김승수 | 배송 관리, 배송경로 관리, Kafka 관리, 공통모듈 기여 |
| 윤창근 | 유저와 배송담당자 관리, 인증서버(Auth)-게이트웨이(Gateway)의 Jwt토큰 생성 및 검증, Security 설정 |

## 📌 서비스 구성 및 실행 방법

### 서비스 구성

[서비스 구성](https://github.com/9haTeam/9pang/wiki/%EC%84%9C%EB%B9%84%EC%8A%A4-%EA%B5%AC%EC%84%B1)

### 실행 방법

```shell
root 디렉토리에서 'docker-compose up --build' 명령어 실행
```

### 각 서비스 엔드포인트

[엔드 포인트](https://github.com/9haTeam/9pang/wiki/%EC%97%94%EB%93%9C%ED%8F%AC%EC%9D%B8%ED%8A%B8)

## 📌 ERD

![ERD 명세서](https://github.com/user-attachments/assets/6f6b057c-16b4-448e-ad57-7c07bf0e99ac)


## 📌 인프라 설계도

![인프라설계도](https://github.com/user-attachments/assets/806ac7a2-b367-436d-93d4-29bfc7c95283)

## 📌 기술 스택

- Backend: Java 17, Spring Boot 3.4, Spring Cloud (Eureka, Gateway, Feign)
- Database: PostgreSQL, Redis
- Infrastructure: Docker, Docker Compose, Kafka, Zipkin
- Communication: REST API, Event-driven architecture (Kafka)
- Authentication: Spring Security, JWT
- API Integration: Slack API
- Documentation: Swagger/OpenAPI
- Build Tool: Gradle

## 📌 트러블 슈팅

[트러블 슈팅](https://github.com/9haTeam/9pang/wiki/%ED%8A%B8%EB%9F%AC%EB%B8%94-%EC%8A%88%ED%8C%85)

## 📌 API Docs

- [Eureka 대시보드](http://localhost:8761)
- [Swagger API 문서 링크](http://localhost:8080/swagger-ui.html)

---

