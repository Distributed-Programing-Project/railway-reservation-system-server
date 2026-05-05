package vn.edu.iuh.fit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OriginalTicketInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String originalTicketId;
    private String originalTrainName;
    private String originalCarriageName;
    private String originalSeatCode;
    private String originalDepartureStation;
    private String originalArrivalStation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalDepartureTime;
}
