package com.usst.superai.service;

import com.usst.superai.model.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

public interface TravelPlanService {

    TravelPlanResponse generatePlan(TravelPlanRequest request);

    TravelPlanResponse chat(TravelPlanRequest request);

    TravelPlanResponse chatWithRag(TravelPlanRequest request);

    Flux<String> streamPlan(TravelPlanRequest request);

    TravelExportResponse generateAndExport(TravelPlanRequest request);

    void downloadFile(String filename, HttpServletResponse response);
}
