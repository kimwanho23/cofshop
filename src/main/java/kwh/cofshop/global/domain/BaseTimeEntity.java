package kwh.cofshop.global.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {

    //등록일
    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime createDate;

    //수정일
    @LastModifiedDate
    protected LocalDateTime lastModifiedDate;
}
