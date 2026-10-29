package ru.vrazumi.core.group;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Все запросы ограничены владельцем: чужие группы для пользователя не существуют. */
public interface DeckGroupRepository extends JpaRepository<DeckGroup, Long> {

  @Query(
      """
      select new ru.vrazumi.core.group.DeckGroupWithDeckCount(g, count(d))
      from DeckGroup g
      left join Deck d on d.group = g
      where g.owner.id = :ownerId
      group by g
      order by lower(g.name), g.id
      """)
  List<DeckGroupWithDeckCount> findAllWithDeckCount(Long ownerId);

  @Query(
      """
      select new ru.vrazumi.core.group.DeckGroupWithDeckCount(g, count(d))
      from DeckGroup g
      left join Deck d on d.group = g
      where g.id = :groupId and g.owner.id = :ownerId
      group by g
      """)
  Optional<DeckGroupWithDeckCount> findWithDeckCountById(Long groupId, Long ownerId);

  Optional<DeckGroup> findByIdAndOwnerId(Long groupId, Long ownerId);

  boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

  boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long groupId);
}
