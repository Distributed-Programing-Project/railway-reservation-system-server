package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnTicketSearchDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "idCard is required")
    private String idCard;
}

