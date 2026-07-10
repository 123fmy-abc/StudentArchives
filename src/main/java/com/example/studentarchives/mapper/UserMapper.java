package com.example.studentarchives.mapper;

import com.example.studentarchives.dto.response.user.UserProfileResponse;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.GenderEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 用户实体与 DTO 之间的转换映射
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /** User → UserProfileResponse 基本字段（不含角色权限，由 Service 层补充） */
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderToInt")
    @Mapping(target = "genderLabel", source = "gender", qualifiedByName = "genderToLabel")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "maskPhone")
    @Mapping(target = "schoolName", expression = "java(user.getSchool() != null ? user.getSchool().getName() : null)")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    UserProfileResponse toProfile(User user);

    /** Byte 性别 → int */
    @Named("genderToInt")
    default Integer genderToInt(Byte gender) {
        return gender != null ? gender.intValue() : null;
    }

    /** Byte 性别 → 中文标签 */
    @Named("genderToLabel")
    default String genderToLabel(Byte gender) {
        if (gender == null) return GenderEnum.UNKNOWN.getLabel();
        GenderEnum ge = GenderEnum.of(gender.intValue());
        return ge != null ? ge.getLabel() : GenderEnum.UNKNOWN.getLabel();
    }

    /** 手机号脱敏：138****0001 */
    @Named("maskPhone")
    default String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
