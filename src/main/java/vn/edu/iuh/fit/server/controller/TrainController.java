package vn.edu.iuh.fit.server.controller;

public interface TrainController {
    void initialize();

    void handleSearch();

    void handleShowAll();

    void handleRefresh();

    void handleCreateTrain();

    void handleConfigureTrain();
}

