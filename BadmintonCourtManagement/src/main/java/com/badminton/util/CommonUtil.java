package com.badminton.util;

import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.requestmodel.CourtDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class CommonUtil {
    public static boolean isNotNullEmpty(String string) {
        return string != null && !string.isBlank();
    }

    public static boolean checkValidCourt(CourtDTO courtDTO) {
        return courtDTO != null && StringUtils.isNoneBlank(courtDTO.getCourtId()) && courtDTO.getCourtAreas() != null
                && existCourtArea(courtDTO.getCourtAreas());
    }

    private static boolean existCourtArea(List<CourtAreaDTO> courtAreas) {
        return courtAreas.stream().anyMatch(courtArea -> courtArea != null &&
                StringUtils.isNoneBlank(courtArea.getArea()) && courtArea.getPlayerInArea() != null
                && StringUtils.isNoneBlank(courtArea.getPlayerInArea().getPlayerName()
        ));
    }
}
