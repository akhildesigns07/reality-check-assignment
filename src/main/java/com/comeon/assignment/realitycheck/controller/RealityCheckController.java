package com.comeon.assignment.realitycheck.controller;

import com.comeon.assignment.realitycheck.exception.RealityCheckException;
import com.comeon.assignment.realitycheck.model.RealityCheckSession;
import com.comeon.assignment.realitycheck.model.RestResponse;
import com.comeon.assignment.realitycheck.service.RealityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/realitycheck")
public class RealityCheckController {
    private final RealityCheckService service;

    @GetMapping("/getStatus/{playerId}")
    @ResponseBody
    public String getStatus(@PathVariable long playerId) {
        return service.getStatus(playerId);
    }

    @GetMapping("/getOrStartCheck/{playerId}/{intervalMinutes}")
    @ResponseBody
    public RestResponse getOrStartCheck(@PathVariable long playerId, @PathVariable int intervalMinutes) {
        try {
            return new RestResponse(service.getOrStartCheck(playerId, intervalMinutes));
        } catch (RealityCheckException e) {
            log.error("getOrStartCheck failed for player {}", playerId, e);
            return new RestResponse(e.getMessage(), e);
        }
    }

    @PostMapping("/acknowledge/{playerId}")
    @ResponseBody
    public RestResponse acknowledge(@PathVariable long playerId) {
        try {
            RealityCheckSession s = service.acknowledge(playerId);
            return new RestResponse(s);
        } catch (RealityCheckException e) {
            return new RestResponse(e.getMessage(), e);
        }
    }
}
