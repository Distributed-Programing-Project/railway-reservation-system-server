package vn.edu.iuh.fit.server.network;

import vn.edu.iuh.fit.common.dto.CreateCarriageDTO;
import vn.edu.iuh.fit.common.dto.CreateTrainDTO;
import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.common.dto.CustomerDeleteRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerSearchDTO;
import vn.edu.iuh.fit.common.dto.EmployeeDTO;
import vn.edu.iuh.fit.common.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.common.dto.ExchangeEligibleTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.common.dto.LoginRequestDTO;
import vn.edu.iuh.fit.common.dto.PaymentCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.PaymentStatusRequestDTO;
import vn.edu.iuh.fit.common.dto.RefundReceiptRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.RouteActionDTO;
import vn.edu.iuh.fit.common.dto.RouteDTO;
import vn.edu.iuh.fit.common.dto.RouteFilterDTO;
import vn.edu.iuh.fit.common.dto.RouteStopDTO;
import vn.edu.iuh.fit.common.dto.SaleCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchDTO;
import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDetailPriceUpdateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleGenerateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleLifecycleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.common.dto.SeatHoldRequestDTO;
import vn.edu.iuh.fit.common.dto.SeatMapRequestDTO;
import vn.edu.iuh.fit.common.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.common.dto.TrainFilterDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainCarriagesDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainStatusDTO;
import vn.edu.iuh.fit.common.message.CommonMessages;
import vn.edu.iuh.fit.common.message.ScheduleMessages;
import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.service.CustomerService;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.service.LoginService;
import vn.edu.iuh.fit.server.service.PaymentOrderService;
import vn.edu.iuh.fit.server.service.RouteService;
import vn.edu.iuh.fit.server.service.RouteStopService;
import vn.edu.iuh.fit.server.service.SaleService;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.service.StationService;
import vn.edu.iuh.fit.server.service.StatisticsService;
import vn.edu.iuh.fit.server.service.TicketService;
import vn.edu.iuh.fit.server.service.TrainService;
import vn.edu.iuh.fit.server.service.impl.CustomerServiceImpl;
import vn.edu.iuh.fit.server.service.impl.EmployeeServiceImpl;
import vn.edu.iuh.fit.server.service.impl.LoginServiceImpl;
import vn.edu.iuh.fit.server.service.impl.PaymentOrderServiceImpl;
import vn.edu.iuh.fit.server.service.impl.RouteServiceImpl;
import vn.edu.iuh.fit.server.service.impl.RouteStopServiceImpl;
import vn.edu.iuh.fit.server.service.impl.SaleServiceImpl;
import vn.edu.iuh.fit.server.service.impl.ScheduleServiceImpl;
import vn.edu.iuh.fit.server.service.impl.StationServiceImpl;
import vn.edu.iuh.fit.server.service.impl.StatisticsServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TicketServiceImpl;
import vn.edu.iuh.fit.server.service.impl.TrainServiceImpl;

public class RequestRouter {
    private final LoginService loginService = new LoginServiceImpl();
    private final ScheduleService scheduleService = new ScheduleServiceImpl();
    private final EmployeeService employeeService = new EmployeeServiceImpl();
    private final StatisticsService statisticsService = new StatisticsServiceImpl();
    private final TicketService ticketService = new TicketServiceImpl();
    private final TrainService trainService = new TrainServiceImpl();
    private final CustomerService customerService = new CustomerServiceImpl();
    private final SaleService saleService = new SaleServiceImpl();
    private final PaymentOrderService paymentOrderService = new PaymentOrderServiceImpl();
    private final StationService stationService = new StationServiceImpl();
    private final RouteService routeService = new RouteServiceImpl();
    private final RouteStopService routeStopService = new RouteStopServiceImpl();

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error(CommonMessages.INVALID_REQUEST);
        }

        return switch (request.getAction()) {
            case LOGIN -> loginService.login(castData(request, LoginRequestDTO.class));

            case FILTER_SCHEDULE -> scheduleService.filterSchedules(castData(request, ScheduleFilterDTO.class));
            case CREATE_SCHEDULE -> scheduleService.createSchedule(castData(request, ScheduleCreateDTO.class));
            case UPDATE_SCHEDULE -> scheduleService.updateSchedule(castData(request, ScheduleUpdateDTO.class));
            case GENERATE_SCHEDULES -> scheduleService.generateSchedules(castData(request, ScheduleGenerateDTO.class));
            case FIND_SCHEDULE_DETAILS ->
                scheduleService.findScheduleDetails(castData(request, ScheduleDetailPriceUpdateDTO.class));
            case UPDATE_SCHEDULE_DETAIL_PRICES ->
                scheduleService.updateScheduleDetailPrices(castData(request, ScheduleDetailPriceUpdateDTO.class));

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
            case GET_REFUND_RECEIPT ->
                ticketService.getRefundReceipt(castData(request, RefundReceiptRequestDTO.class));
            case SEARCH_TICKETS_FOR_EXCHANGE ->
                ticketService.searchTicketsForExchange(castData(request, ExchangeEligibleTicketSearchDTO.class));
            case PREVIEW_EXCHANGE_TICKETS ->
                ticketService.previewExchangeTickets(castData(request, ExchangeTicketPreviewRequestDTO.class));
            case EXCHANGE_TICKET ->
                ticketService.exchangeTickets(castData(request, ExchangeTicketRequestDTO.class));

            // case FIND_ALL_STATIONS_ST -> stationService.getAllStations();
            case FIND_ALL_ROUTES -> routeService.getAllRoutes();
            case SEARCH_ROUTES -> routeService.searchRoutes(castData(request, RouteFilterDTO.class));
            case FIND_ROUTE_BY_ID -> routeService.findRouteById(castData(request, String.class));
            case CREATE_ROUTE -> routeService.createRoute(castData(request, RouteDTO.class));
            case UPDATE_ROUTE -> routeService.updateRoute(castData(request, RouteDTO.class));
            case DELETE_ROUTE -> routeService.deleteRoute(castData(request, RouteActionDTO.class));
            case PROMOTE_ROUTE -> routeService.promoteRoute(castData(request, RouteActionDTO.class));
            case DISABLE_ROUTE -> routeService.disableRoute(castData(request, RouteActionDTO.class));
            case FIND_ROUTE_STOPS_BY_ROUTE ->
                routeStopService.findRouteStopsByRouteId(castData(request, RouteActionDTO.class));
            case CREATE_ROUTE_STOP -> routeStopService.createRouteStop(castData(request, RouteStopDTO.class));
            case UPDATE_ROUTE_STOP -> routeStopService.updateRouteStop(castData(request, RouteStopDTO.class));
            case DELETE_ROUTE_STOP -> routeStopService.deleteRouteStop(castData(request, RouteStopDTO.class));

            case FIND_ALL_TRAINS -> trainService.findAllTrains(castData(request, TrainFilterDTO.class));
            case FIND_TRAIN_BY_CODE -> trainService.findTrainsByCode(castData(request, String.class));
            case FIND_UNASSIGNED_CARRIAGES -> trainService.findUnassignedCarriages();
            case CREATE_TRAIN -> trainService.createTrain(castData(request, CreateTrainDTO.class));
            case CREATE_CARRIAGE -> trainService.createCarriage(castData(request, CreateCarriageDTO.class));
            case UPDATE_TRAIN_CARRIAGES ->
                trainService.updateTrainCarriages(castData(request, UpdateTrainCarriagesDTO.class));
            case UPDATE_TRAIN_STATUS -> trainService.updateTrainStatus(castData(request, UpdateTrainStatusDTO.class));

            case SEARCH_CUSTOMERS -> customerService.searchCustomers(castData(request, CustomerSearchDTO.class));
            case CREATE_CUSTOMER -> customerService.createCustomer(castData(request, CustomerDTO.class));
            case UPDATE_CUSTOMER -> customerService.updateCustomer(castData(request, CustomerDTO.class));
            case DELETE_CUSTOMER -> customerService.deleteCustomer(castData(request, CustomerDeleteRequestDTO.class));
            case GET_CUSTOMER_HISTORY ->
                customerService.getCustomerHistory(castData(request, CustomerHistoryRequestDTO.class));

            case FIND_ALL_STATIONS -> saleService.findAllStations();
            case SEARCH_SCHEDULES_FOR_SALE ->
                saleService.searchSchedulesForSale(castData(request, SaleScheduleSearchDTO.class));
            case GET_SEATMAP_FOR_SCHEDULE ->
                saleService.getSeatMapForSchedule(castData(request, SeatMapRequestDTO.class));
            case HOLD_SEATS_FOR_SALE -> saleService.holdSeatsForSale(castData(request, SeatHoldRequestDTO.class));
            case RELEASE_HELD_SEATS_FOR_SALE ->
                saleService.releaseHeldSeatsForSale(castData(request, SeatHoldRequestDTO.class));
            case CREATE_PAYMENT_ORDER ->
                paymentOrderService.createPaymentOrder(castData(request, PaymentCreateRequestDTO.class));
            case GET_PAYMENT_ORDER_STATUS ->
                paymentOrderService.getPaymentOrderStatus(castData(request, PaymentStatusRequestDTO.class));
            case CONFIRM_PAYMENT_ORDER ->
                paymentOrderService.confirmPaymentOrder(castData(request, PaymentStatusRequestDTO.class));
            case CONFIRM_INTERNAL_PAYMENT ->
                paymentOrderService.confirmPaymentOrder(castData(request, PaymentStatusRequestDTO.class));
            case CREATE_SALE_TRANSACTION ->
                saleService.createSaleTransaction(castData(request, SaleCreateRequestDTO.class));

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
