package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.LoginRequestDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface LoginService {

    Response login(LoginRequestDTO loginRequestDTO);
}
