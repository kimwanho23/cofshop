package kwh.cofshop.chat.repository;

import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.repository.custom.ChatRoomRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

}
