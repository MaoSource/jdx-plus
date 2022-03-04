package cn.yiidii.jdx.model.enums;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * NoticeModelEnum
 *
 * @author ed w
 * @since 1.0
 */
public enum NoticeModelEnum {
    TOP, HTML;

    public static NoticeModelEnum get(String val) {
        return getOrDefault(val, null);
    }

    public static NoticeModelEnum getOrDefault(String val, NoticeModelEnum def) {
        if (Objects.isNull(val)) {
            return def;
        }
        return Stream.of(values()).parallel().filter(item -> item.name().equalsIgnoreCase(val)).findAny().orElse(def);
    }
}
