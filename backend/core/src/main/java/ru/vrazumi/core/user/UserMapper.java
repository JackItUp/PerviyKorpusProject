package ru.vrazumi.core.user;

import org.mapstruct.Mapper;
import ru.vrazumi.core.user.dto.UserResponse;

@Mapper
public interface UserMapper {

  UserResponse toResponse(User user);
}
