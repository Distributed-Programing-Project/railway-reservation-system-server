package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateTrainCarriagesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID tàu là bắt buộc")
    private String trainId;

    @NotNull(message = "Danh sách toa là bắt buộc")
    @Size(min = 3, max = 16, message = "Tàu phải có từ 3 đến 16 toa")
    private List<String> carriageIds;
}
