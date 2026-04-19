package vn.edu.iuh.fit.server.controller;

public interface ScheduleController {
    void initialize();

    void setParentController();

    void setAddMode();

    void setEditMode(String scheduleId);

    void handlePrevPage();

    void handleNextPage();

    void handleAddSchedule();

    void handleEditSchedule();

    void handleDeleteSchedule();

    void handleGenerateSchedules();

    void handleRefresh();

    void handleViewSeatsDetail();

    void handleSave();

    void handleGenerate();

    void handleCancel();
}

