package fileeventlistener.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedFileRepo extends JpaRepository<fileeventlistener.com.entity.SharedFile, Long> {
}
