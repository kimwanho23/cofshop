package kwh.cofshop.chat.repository;

import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.repository.custom.ChatRoomRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    @Query("""
            SELECT (COUNT(cr) > 0)
            FROM ChatRoom cr
            WHERE cr.id = :roomId
              AND (cr.customer.id = :memberId OR cr.agent.id = :memberId)
            """)
    boolean existsParticipant(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

}
