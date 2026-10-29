package ru.vrazumi.core.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.vrazumi.core.common.ConflictException;
import ru.vrazumi.core.group.dto.DeckGroupRequest;
import ru.vrazumi.core.group.dto.DeckGroupResponse;
import ru.vrazumi.core.user.User;
import ru.vrazumi.core.user.UserService;

@ExtendWith(MockitoExtension.class)
class DeckGroupServiceTest {

  private static final long USER = 5L;

  @Mock private DeckGroupRepository groupRepository;
  @Mock private UserService userService;
  private DeckGroupService groupService;

  @BeforeEach
  void setUp() {
    groupService =
        new DeckGroupService(
            groupRepository, Mappers.getMapper(DeckGroupMapper.class), userService);
  }

  @Test
  void createStoresTrimmedNameWithOwner() {
    User owner = new User();
    when(userService.getReference(USER)).thenReturn(owner);
    when(groupRepository.saveAndFlush(any(DeckGroup.class)))
        .thenAnswer(
            invocation -> {
              DeckGroup group = invocation.getArgument(0);
              group.setId(3L);
              return group;
            });

    DeckGroupResponse response = groupService.create(new DeckGroupRequest("  Английский  "), USER);

    ArgumentCaptor<DeckGroup> saved = ArgumentCaptor.forClass(DeckGroup.class);
    verify(groupRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getName()).isEqualTo("Английский");
    assertThat(saved.getValue().getOwner()).isSameAs(owner);
    assertThat(response.id()).isEqualTo(3L);
    assertThat(response.deckCount()).isZero();
  }

  @Test
  void createRejectsDuplicateName() {
    when(groupRepository.existsByOwnerIdAndNameIgnoreCase(USER, "Английский")).thenReturn(true);

    assertThatThrownBy(() -> groupService.create(new DeckGroupRequest("Английский"), USER))
        .isInstanceOfSatisfying(
            ConflictException.class, ex -> assertThat(ex.getField()).isEqualTo("name"));
    verify(groupRepository, never()).saveAndFlush(any());
  }

  @Test
  void createTranslatesUniqueIndexRaceIntoConflict() {
    when(groupRepository.saveAndFlush(any(DeckGroup.class)))
        .thenThrow(new DataIntegrityViolationException("uq_deck_group_owner_name_lower"));

    assertThatThrownBy(() -> groupService.create(new DeckGroupRequest("Английский"), USER))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void findAllMapsDeckCounts() {
    when(groupRepository.findAllWithDeckCount(USER))
        .thenReturn(
            List.of(
                new DeckGroupWithDeckCount(group(1L, "Английский"), 3L),
                new DeckGroupWithDeckCount(group(2L, "История"), 0L)));

    assertThat(groupService.findAll(USER))
        .extracting(DeckGroupResponse::name, DeckGroupResponse::deckCount)
        .containsExactly(tuple("Английский", 3L), tuple("История", 0L));
  }

  @Test
  void renameRejectsNameOfAnotherGroup() {
    when(groupRepository.findByIdAndOwnerId(1L, USER))
        .thenReturn(Optional.of(group(1L, "Английский")));
    when(groupRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(USER, "История", 1L))
        .thenReturn(true);

    assertThatThrownBy(() -> groupService.rename(1L, new DeckGroupRequest("История"), USER))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void deleteRemovesGroupOfUser() {
    DeckGroup group = group(1L, "Английский");
    when(groupRepository.findByIdAndOwnerId(1L, USER)).thenReturn(Optional.of(group));

    groupService.delete(1L, USER);

    verify(groupRepository).delete(group);
  }

  @Test
  void foreignOrMissingGroupIsNotFound() {
    when(groupRepository.findByIdAndOwnerId(9L, USER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.delete(9L, USER))
        .isInstanceOf(DeckGroupNotFoundException.class)
        .hasMessage("Группа с id=9 не найдена");
    verify(groupRepository, never()).delete(any());
  }

  private static DeckGroup group(Long id, String name) {
    DeckGroup group = new DeckGroup();
    group.setId(id);
    group.setName(name);
    return group;
  }
}
