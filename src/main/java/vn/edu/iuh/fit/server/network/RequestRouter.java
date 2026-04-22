package vn.edu.iuh.fit.server.network;

import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.EmployeeDTO;
import vn.edu.iuh.fit.server.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.server.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.dto.CreateCarriageDTO;
import vn.edu.iuh.fit.server.dto.CreateTrainDTO;
import vn.edu.iuh.fit.server.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.server.dto.TrainFilterDTO;
import vn.edu.iuh.fit.server.dto.UpdateTrainCarriagesDTO;
import vn.edu.iuh.fit.server.dto.UpdateTrainStatusDTO;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.service.StatisticsService;
import vn.edu.iuh.fit.server.service.TrainService;
import vn.edu.iuh.fit.server.service.impl.EmployeeServiceImpl;
import vn.edu.iuh.fit.server.service.impl.ScheduleServiceImpl;
import vn.edu.iuh.fit.server.service.impl.StatisticsServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TrainServiceImpl;

public class RequestRouter {

    private final ScheduleService scheduleService = new ScheduleServiceImpl();
    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private final StatisticsService statisticsService = new StatisticsServiceImpl();
    private final TrainService trainService = new TrainServiceImpl();

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error("Invalid request");
        }
        return switch (request.getAction()) {
            case LOGIN                   -> Response.error("Not implemented yet");

            case FILTER_SCHEDULE         -> scheduleService.filterSchedules(castData(request, ScheduleFilterDTO.class));
            case CREATE_SCHEDULE         -> scheduleService.createSchedule(castData(request, ScheduleCreateDTO.class));

            case CREATE_EMPLOYEE         -> employeeService.createEmployee(castData(request, EmployeeDTO.class));
            case CREATE_EMPLOYEE_ACCOUNT -> employeeService.createEmployeeAccount(castData(request, String.class));
            case DELETE_EMPLOYEE         -> employeeService.softDeleteEmployee(castData(request, String.class));
            case FIND_ALL_EMPLOYEES      -> employeeService.findAllEmployees(castData(request, EmployeeFilterDTO.class));
            case RESET_EMPLOYEE_PASSWORD -> employeeService.resetEmployeePassword(castData(request, String.class));

            case GET_STATISTICS          -> statisticsService.getStatistics(castData(request, StatisticsRequestDTO.class));

            case FIND_ALL_TRAINS         -> trainService.findAllTrains(castData(request, TrainFilterDTO.class));
            case FIND_TRAIN_BY_CODE      -> trainService.findTrainsByCode(castData(request, String.class));
            case FIND_UNASSIGNED_CARRIAGES -> trainService.findUnassignedCarriages();
            case CREATE_TRAIN            -> trainService.createTrain(castData(request, CreateTrainDTO.class));
            case CREATE_CARRIAGE         -> trainService.createCarriage(castData(request, CreateCarriageDTO.class));
            case UPDATE_TRAIN_CARRIAGES  -> trainService.updateTrainCarriages(castData(request, UpdateTrainCarriagesDTO.class));
            case UPDATE_TRAIN_STATUS     -> trainService.updateTrainStatus(castData(request, UpdateTrainStatusDTO.class));

            default                      -> Response.error("Unknown action: " + request.getAction());
        };
    }

    private <T> T castData(Request request, Class<T> type) {
        try {
            return type.cast(request.getData());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "Expected " + type.getSimpleName() + " for action " + request.getAction());
        }
    }
}