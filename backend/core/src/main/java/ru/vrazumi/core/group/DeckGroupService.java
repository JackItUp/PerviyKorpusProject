package ru.vrazumi.core.group;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.group.dto.DeckGroupRequest;
import ru.vrazumi.core.group.dto.DeckGroupResponse;
import ru.vrazumi.core.user.UserService;

/**
 * Группы колод текущего пользователя. Чужая группа неотличима от несуществующей (404). При удалении
 * группы колоды не удаляются, а остаются без группы (внешний ключ ON DELETE SET NULL).
 */
@Service
@RequiredArgsConstructor
public class DeckGroupService {

  static final String NAME_TAKEN = "Группа с таким названием уже есть";

  private final DeckGroupRepository groupRepository;
  private final DeckGroupMapper groupMapper;
  private final UserService userService;

  @Transactional(readOnly = true)
  public List<DeckGroupResponse> findAll(Long userId) {
    return groupRepository.findAllWithDeckCount(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public DeckGroupResponse findById(Long groupId, Long userId) {
    return groupRepository
        .findWithDeckCountById(groupId, userId)
        .map(this::toResponse)
        .orElseThrow(() -> new DeckGroupNotFoundException(groupId));
  }

  @Transactional
  public DeckGroupResponse create(DeckGroupRequest request, Long userId) {
    if (groupRepository.existsByOwnerIdAndNameIgnoreCase(userId, request.name())) {
      throw new ConflictException("name", NAME_TAKEN);
    }
    DeckGroup group = new DeckGroup();
    group.setOwner(userService.getReference(userId));
    group.setName(request.name());
    return groupMapper.toResponse(saveUnique(group), 0);
  }

  @Transactional
  public DeckGroupResponse rename(Long groupId, DeckGroupRequest request, Long userId) {
    DeckGroup group = getExisting(groupId, userId);
    if (groupRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, request.name(), groupId)) {
      throw new ConflictException("name", NAME_TAKEN);
    }
    group.setName(request.name());
    saveUnique(group);
    return findById(groupId, userId);
  }

  @Transactional
  public void delete(Long groupId, Long userId) {
    groupRepository.delete(getExisting(groupId, userId));
  }

  /** Группа пользователя для других фич (перенос колоды). Бросает 404, если её нет. */
  @Transactional(readOnly = true)
  public DeckGroup getExisting(Long groupId, Long userId) {
    return groupRepository
        .findByIdAndOwnerId(groupId, userId)
        .orElseThrow(() -> new DeckGroupNotFoundException(groupId));
  }

  /** Сохраняет группу; гонка двух одинаковых названий упирается в уникальный индекс → 409. */
  private DeckGroup saveUnique(DeckGroup group) {
    try {
      return groupRepository.saveAndFlush(group);
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("name", NAME_TAKEN);
    }
  }

  private DeckGroupResponse toResponse(DeckGroupWithDeckCount row) {
    return groupMapper.toResponse(row.group(), row.deckCount());
  }
}
