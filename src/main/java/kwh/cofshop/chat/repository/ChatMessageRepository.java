package kwh.cofshop.chat.repository;

import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.repository.custom.ChatMessageRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    List<ChatMessage> findAllByMessageGroupId(String groupId);
}
