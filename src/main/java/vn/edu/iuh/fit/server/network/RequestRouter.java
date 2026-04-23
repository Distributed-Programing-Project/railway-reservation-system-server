package vn.edu.iuh.fit.server.network;

import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.common.dto.EmployeeDTO;
import vn.edu.iuh.fit.common.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleLifecycleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.common.dto.CreateCarriageDTO;
import vn.edu.iuh.fit.common.dto.CreateTrainDTO;
import vn.edu.iuh.fit.common.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.common.dto.TrainFilterDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainCarriagesDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainStatusDTO;
import vn.edu.iuh.fit.common.message.CommonMessages;
import vn.edu.iuh.fit.common.message.ScheduleMessages;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.service.StatisticsService;
import vn.edu.iuh.fit.server.service.TicketService;
import vn.edu.iuh.fit.server.service.TrainService;
import vn.edu.iuh.fit.server.service.impl.EmployeeServiceImpl;
import vn.edu.iuh.fit.server.service.impl.ScheduleServiceImpl;
import vn.edu.iuh.fit.server.service.impl.StatisticsServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TicketServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TrainServiceImpl;

public class RequestRouter {

    private final ScheduleService scheduleService = new ScheduleServiceImpl();
    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private final StatisticsService statisticsService = new StatisticsServiceImpl();
    private final TicketService ticketService = new TicketServiceImpl();
    private final TrainService trainService = new TrainServiceImpl();

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error(CommonMessages.INVALID_REQUEST);
        }

        return switch (request.getAction()) {
            case LOGIN -> Response.error(CommonMessages.NOT_IMPLEMENTED);

            case FILTER_SCHEDULE -> scheduleService.filterSchedules(castData(request, ScheduleFilterDTO.class));
            case CREATE_SCHEDULE -> scheduleService.createSchedule(castData(request, ScheduleCreateDTO.class));
            case UPDATE_SCHEDULE -> scheduleService.updateSchedule(castData(request, ScheduleUpdateDTO.class));

            case CREATE_EMPLOYEE -> employeeService.createEmployee(castData(request, EmployeeDTO.class));
            case CREATE_EMPLOYEE_ACCOUNT -> employeeService.createEmployeeAccount(castData(request, String.class));
            case DELETE_EMPLOYEE -> employeeService.softDeleteEmployee(castData(request, String.class));
            case FIND_ALL_EMPLOYEES -> employeeService.findAllEmployees(castData(request, EmployeeFilterDTO.class));
            case RESET_EMPLOYEE_PASSWORD -> employeeService.resetEmployeePassword(castData(request, String.class));

            case GET_STATISTICS -> statisticsService.getStatistics(castData(request, StatisticsRequestDTO.class));

            case SEARCH_TICKETS_FOR_RETURN ->
                ticketService.searchTicketsForReturn(castData(request, ReturnTicketSearchDTO.class));
            case PREVIEW_RETURN_TICKETS ->
                ticketService.previewReturnTickets(castData(request, ReturnTicketPreviewRequestDTO.class));
            case CONFIRM_RETURN_TICKETS ->
                ticketService.confirmReturnTickets(castData(request, ReturnTicketConfirmDTO.class));

            case FIND_ALL_TRAINS -> trainService.findAllTrains(castData(request, TrainFilterDTO.class));
            case FIND_TRAIN_BY_CODE -> trainService.findTrainsByCode(castData(request, String.class));
            case FIND_UNASSIGNED_CARRIAGES -> trainService.findUnassignedCarriages();
            case CREATE_TRAIN -> trainService.createTrain(castData(request, CreateTrainDTO.class));
            case CREATE_CARRIAGE -> trainService.createCarriage(castData(request, CreateCarriageDTO.class));
            case UPDATE_TRAIN_CARRIAGES ->
                trainService.updateTrainCarriages(castData(request, UpdateTrainCarriagesDTO.class));
            case UPDATE_TRAIN_STATUS -> trainService.updateTrainStatus(castData(request, UpdateTrainStatusDTO.class));

            case PUBLISH_OR_DISABLE_SCHEDULE -> {
                ScheduleLifecycleDTO dto = castData(request, ScheduleLifecycleDTO.class);
                String action = dto.getAction().trim().toUpperCase();
                if ("PUBLISH".equals(action)) {
                    yield scheduleService.publishSchedule(dto);
                } else if ("DISABLE".equals(action)) {
                    yield scheduleService.disableSchedule(dto);
                } else {
                    yield Response.error(ScheduleMessages.ACTION_INVALID);
                }
            }

            default -> Response.error(String.format(CommonMessages.UNKNOWN_ACTION, request.getAction()));
        };
    }

    private <T> T castData(Request request, Class<T> type) {
        Object data = request.getData();
        if (data == null) {
            throw new IllegalArgumentException(String.format(CommonMessages.INVALID_REQUEST, type.getSimpleName()));
        }
        try {
            return type.cast(data);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format(CommonMessages.CAST_DATA_ERROR, type.getSimpleName()));
        }
    }
}
