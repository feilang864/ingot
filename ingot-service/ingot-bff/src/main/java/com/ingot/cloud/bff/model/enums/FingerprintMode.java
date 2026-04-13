package com.ingot.cloud.bff.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>描述这个类的作用</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum FingerprintMode {
    DEVICE("device", "前端设备指纹"),
    IPUA("ip_ua", "IP+UA");

    private final String value;
    private final String text;
}
