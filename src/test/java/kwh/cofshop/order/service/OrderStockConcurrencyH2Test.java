package kwh.cofshop.order.service;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// Docker 없이도 실행되는 빠른 회귀 테스트. 운영 DB 기준 검증은 OrderStockConcurrencyMySqlTest가 담당
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-concurrency;MODE=MySQL;LOCK_TIMEOUT=30000;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class OrderStockConcurrencyH2Test extends OrderStockConcurrencyTestBase {
}
