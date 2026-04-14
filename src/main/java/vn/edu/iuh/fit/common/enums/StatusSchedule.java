package vn.edu.iuh.fit.common.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StatusSchedule {
    DRAFT("DRAFT"),
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    PAUSED("PAUSED"),
    READY("READY"),
    COMPLETED("COMPLETED");

    private final String value;
}