package com.roadcrack.api.response.map;

import com.roadcrack.api.enums.DamageType;

public record MapDamageTypeRatioResponse(
        DamageType damageType,
        String damageTypeLabel,
        long count,
        double ratio
) {
}
