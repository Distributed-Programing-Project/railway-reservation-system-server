package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleGenerateResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int createdCount;
    private int outboundCount;
    private int returnCount;
    private List<String> scheduleIds;
}
