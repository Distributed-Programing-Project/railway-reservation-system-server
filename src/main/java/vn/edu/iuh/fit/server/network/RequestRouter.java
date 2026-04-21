package vn.edu.iuh.fit.server.network;

import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.EmployeeDTO;
import vn.edu.iuh.fit.server.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.server.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.service.StatisticsService;
import vn.edu.iuh.fit.server.service.TicketService;
import vn.edu.iuh.fit.server.service.impl.EmployeeServiceImpl;
import vn.edu.iuh.fit.server.service.impl.ScheduleServiceImpl;
import vn.edu.iuh.fit.server.service.impl.StatisticsServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TicketServiceImpl;
import vn.edu.iuh.fit.server.messages.CommonMessages;

public class RequestRouter {

    private final ScheduleService scheduleService = new ScheduleServiceImpl();
    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private final StatisticsService statisticsService = new StatisticsServiceImpl();
    private final TicketService ticketService = new TicketServiceImpl();

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error(CommonMessages.INVALID_REQUEST);
        }

        return switch (request.getAction()) {
            case LOGIN                   -> Response.error(CommonMessages.NOT_IMPLEMENTED);

            case FILTER_SCHEDULE         -> scheduleService.filterSchedules(castData(request, ScheduleFilterDTO.class));
            case CREATE_SCHEDULE         -> scheduleService.createSchedule(castData(request, ScheduleCreateDTO.class));

            case CREATE_EMPLOYEE         -> employeeService.createEmployee(castData(request, EmployeeDTO.class));
            case CREATE_EMPLOYEE_ACCOUNT -> employeeService.createEmployeeAccount(castData(request, String.class));
            case DELETE_EMPLOYEE         -> employeeService.softDeleteEmployee(castData(request, String.class));
            case FIND_ALL_EMPLOYEES      -> employeeService.findAllEmployees(castData(request, EmployeeFilterDTO.class));
            case RESET_EMPLOYEE_PASSWORD -> employeeService.resetEmployeePassword(castData(request, String.class));

            case GET_STATISTICS          -> statisticsService.getStatistics(castData(request, StatisticsRequestDTO.class));

            case SEARCH_TICKETS_FOR_RETURN -> ticketService.searchTicketsForReturn(castData(request, ReturnTicketSearchDTO.class));
            case PREVIEW_RETURN_TICKETS    -> ticketService.previewReturnTickets(castData(request, ReturnTicketPreviewRequestDTO.class));
            case CONFIRM_RETURN_TICKETS    -> ticketService.confirmReturnTickets(castData(request, ReturnTicketConfirmDTO.class));

            default                      -> Response.error(String.format(CommonMessages.UNKNOWN_ACTION, request.getAction()));
        };
    }

    private <T> T castData(Request request, Class<T> type) {
        try {
            return type.cast(request.getData());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format(CommonMessages.CAST_DATA_ERROR, type.getSimpleName()));
        }
    }
}
