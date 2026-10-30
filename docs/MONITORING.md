# 모니터링

메트릭 수집과 알림 규칙을 정리한 문서입니다.

## 구성

- 앱은 관리 포트(`MANAGEMENT_PORT`, 기본 8081)로 `/actuator/health`, `/actuator/prometheus`를 제공합니다.
- **관리 포트는 외부에 공개하지 않습니다.** 인증 없이 접근할 수 있으므로 내부 네트워크에서만 열어 둡니다.
- Prometheus 설정은 `monitoring/prometheus/`에 있습니다.

```bash
docker compose --profile monitoring up -d
```

Prometheus 화면은 서버 안에서 `http://localhost:9090`으로 볼 수 있습니다.

## 알림 수신 (Discord)

Alertmanager가 Discord 웹훅으로 알림을 보냅니다. `critical`은 1시간, `warning`은 4시간마다 반복하고 해결되면 해결 메시지를 보냅니다.

1. Discord 채널 편집 → 연동 → 웹후크에서 URL을 만듭니다.
2. `.env`에 `ALERT_DISCORD_WEBHOOK_URL=<URL>`을 추가합니다. **URL을 아는 사람은 그 채널에 글을 쓸 수 있으므로 커밋하지 않습니다.**
3. Alertmanager를 시작하고 테스트 알림을 보냅니다.

```bash
docker compose --profile monitoring up -d alertmanager
```

```bash
./scripts/send-test-alert.sh
```

약 30초 뒤 Discord에 메시지가 오면 정상입니다. 웹훅 URL이 없으면 Alertmanager가 시작되지 않습니다.

## 업무 메트릭

| 메트릭 | 의미 |
| --- | --- |
| `refund_recovery_queue_size` | 환불 보상 복구 대기 건수 |
| `refund_recovery_dlq_size` | 복구에 실패해 수동 확인이 필요한 건수 |
| `refund_approved_stuck` | 승인 후 30분이 지나도 끝나지 않은 환불 건수 |
| `payment_provider_failures_total` | PG 호출 실패 횟수 (`operation`: `lookup`, `cancel`) |
| `payment_reconciliation_recovered_total` | 대사로 맞춘 결제 건수 (`result`: `confirmed`, `refunded`) |
| `payment_reconciliation_mismatch_total` | 대사 중 금액이나 거래 ID가 달랐던 건수 |
| `lock_acquisition_failures_total` | 분산 락 획득 실패 횟수 |
| `coupon_outbox_events` | 쿠폰 outbox 건수 (`status`: `PENDING`, `FAILED`) |

## 알림 대응

규칙은 `monitoring/prometheus/alerts.yml`에 있고, `alerts.test.yml`로 CI에서 검증합니다.

| 알림 | 등급 | 먼저 확인할 것 |
| --- | --- | --- |
| `AppDown` | critical | 앱 프로세스, 컨테이너 상태, 관리 포트 |
| `HighErrorRate` | critical | 앱 로그의 `UnhandledException`, DB·Redis 상태 |
| `SlowRequests` | warning | 느린 URI, DB 커넥션 대기, 락 대기 |
| `DatabasePoolExhausted` | warning | 긴 트랜잭션, 락 대기, DB 상태 |
| `HighHeapUsage` | warning | 힙 추세, 대용량 조회 |
| `RefundRecoveryDeadLetter` | critical | `[RefundRecoveryDLQ]` 로그의 주문과 PG 상태를 대조해 수동 처리 |
| `RefundRecoveryBacklog` | warning | `[RefundRecovery]` 로그의 실패 사유 |
| `ApprovedRefundStuck` | critical | `[ApprovedRefund]` 로그, PG 상태, 주문·결제 상태 |
| `PaymentProviderFailures` | critical | PortOne 상태, API 키, 네트워크 |
| `PaymentReconciliationMismatch` | critical | `[Reconcile]` 로그의 결제와 PortOne 내역 대조. 자동 처리되지 않으므로 수동 판단 |
| `LockAcquisitionFailures` | warning | Redis 상태, 락 대기 시간, 같은 주문의 중복 요청 |
| `CouponOutboxBacklog` | warning | `CouponIssueOutboxScheduler` 동작 |
| `CouponOutboxFailed` | warning | outbox의 `last_error` |

## 아직 없는 것

- **대시보드:** Grafana가 없습니다.
- **임계값:** 현재 값은 초기 가정치이며, 부하 테스트와 운영 데이터로 조정해야 합니다.
