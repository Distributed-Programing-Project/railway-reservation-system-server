package vn.edu.iuh.fit.common.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StationStatus {
    DRAFT("DRAFT"),
    PAUSED("PAUSED"),
    READY("READY");

    private final String value;
}
