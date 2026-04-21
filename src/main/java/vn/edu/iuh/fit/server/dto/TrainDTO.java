package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.TrainStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainDTO implements Serializable {
  private String id;
  private TrainStatus status;
}
