package kwh.cofshop.coupon.messaging.rabbit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "coupon.rabbit.enabled", havingValue = "true")
public class CouponIssueRabbitConfig {

    @Bean(name = "couponIssueExchange")
    public DirectExchange couponIssueExchange(
            @Value("${coupon.rabbit.exchange:coupon.issue.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean(name = "couponIssueDlxExchange")
    public DirectExchange couponIssueDlxExchange(
            @Value("${coupon.rabbit.dlx:coupon.issue.dlx}") String dlxName) {
        return new DirectExchange(dlxName, true, false);
    }

    @Bean(name = "couponIssueQueue")
    public Queue couponIssueQueue(
            @Value("${coupon.rabbit.queue:coupon.issue.queue}") String queueName,
            @Value("${coupon.rabbit.dlx:coupon.issue.dlx}") String dlxName,
            @Value("${coupon.rabbit.dlq-routing-key:coupon.issue.failed}") String dlqRoutingKey) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean(name = "couponIssueDlqQueue")
    public Queue couponIssueDlqQueue(
            @Value("${coupon.rabbit.dlq:coupon.issue.dlq}") String dlqName) {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding couponIssueBinding(
            @Qualifier("couponIssueQueue") Queue couponIssueQueue,
            @Qualifier("couponIssueExchange") DirectExchange couponIssueExchange,
            @Value("${coupon.rabbit.routing-key:coupon.issue.created}") String routingKey) {
        return BindingBuilder.bind(couponIssueQueue)
                .to(couponIssueExchange)
                .with(routingKey);
    }

    @Bean
    public Binding couponIssueDlqBinding(
            @Qualifier("couponIssueDlqQueue") Queue couponIssueDlqQueue,
            @Qualifier("couponIssueDlxExchange") DirectExchange couponIssueDlxExchange,
            @Value("${coupon.rabbit.dlq-routing-key:coupon.issue.failed}") String dlqRoutingKey) {
        return BindingBuilder.bind(couponIssueDlqQueue)
                .to(couponIssueDlxExchange)
                .with(dlqRoutingKey);
    }
}
