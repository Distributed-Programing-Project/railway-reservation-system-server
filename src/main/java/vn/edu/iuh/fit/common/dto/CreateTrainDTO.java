package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateTrainDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mác tàu là bắt buộc")
    @Pattern(regexp = "^[A-Za-z0-9]{1,10}$", message = "Mác tàu chỉ chứa chữ cái và số, tối đa 10 ký tự")
    private String trainCode;

    @NotNull(message = "Danh sách toa là bắt buộc")
    @Size(min = 3, max = 16, message = "Tàu phải có từ 3 đến 16 toa")
    private List<String> carriageIds;
}
