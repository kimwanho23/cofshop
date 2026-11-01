# 시스템 구성

코드, `compose.yml`, `package-info.java` 기준입니다. 운영 호스팅은 아직 정하지 않았습니다.

## 실행 구성

```mermaid
flowchart LR
    user[사용자] -->|HTTPS / STOMP| app[Spring Boot API]
    app -->|JPA, Flyway| mysql[(MySQL 8.0)]
    app -->|분산 락, 토큰, 채팅 중계| redis[(Redis)]
    app -->|결제 조회, 취소| portone[PortOne V2]
    app -->|상품 이미지| files[(로컬 디스크 또는 S3)]
    app -.->|쿠폰 발급 이벤트, 기본값 꺼짐| rabbit[(RabbitMQ)]
    prometheus[Prometheus] -->|관리 포트 수집| app
    prometheus --> alertmanager[Alertmanager]
    alertmanager --> discord[Discord]
```

- **RabbitMQ:** `coupon.rabbit.enabled=true`일 때만 씁니다. `compose.yml`에는 없고 소비자도 없습니다. 이유는 [쿠폰 메시징 결정](coupon-messaging-decision.md)에 있습니다.
- **MySQL:** `compose.yml`의 `local-db` 프로필로 띄우거나 외부 DB를 씁니다.
- **모니터링:** `compose.yml`의 `monitoring` 프로필입니다.

## CI

```mermaid
flowchart LR
    dev[개발자] -->|push| github[GitHub]
    github --> actions[GitHub Actions: 비밀 스캔, 테스트, 커버리지, 복구 리허설, 이미지 빌드]
    actions -.->|배포 작업은 현재 꺼짐| server[운영 서버]
```

## 모듈 의존성

`package-info.java`의 `allowedDependencies` 기준입니다. `global`은 대부분의 모듈이 쓰므로 생략했고, `file`은 다른 모듈에 의존하지 않습니다.

```mermaid
flowchart TD
    cart --> member
    cart --> item
    chat --> member
    chat --> security
    coupon --> member
    item --> member
    item --> file
    order --> member
    order --> item
    order --> coupon
    payment --> order
    security --> member
    statistics --> order
```
