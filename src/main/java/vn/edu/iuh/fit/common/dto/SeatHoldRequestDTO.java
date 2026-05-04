package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHoldRequestDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String scheduleId;
  private List<String> scheduleDetailIds;
  private String clientSessionId;
}

