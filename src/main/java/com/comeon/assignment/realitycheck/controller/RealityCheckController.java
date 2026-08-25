package com.comeon.assignment.realitycheck.controller;

import com.comeon.assignment.realitycheck.exception.RealityCheckException;
import com.comeon.assignment.realitycheck.model.RealityCheckSession;
import com.comeon.assignment.realitycheck.model.RealityCheckStatus;
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
    private final RealityCheckService realityCheckService;

    @GetMapping("/getStatus/{playerId}")
    @ResponseBody
    public RealityCheckStatus getStatus(@PathVariable long playerId) {
        return realityCheckService.getStatus(playerId);
    }

    @PatchMapping("/getOrStartCheck")
    @ResponseBody
    public RestResponse getOrStartCheck(@RequestBody GetOrStartRealityCheckRequest getOrStartRealityCheckRequest) {
        try {
            return new RestResponse(realityCheckService.getOrStartCheck(getOrStartRealityCheckRequest));
        } catch (RealityCheckException e) {
            log.error("getOrStartCheck failed for player {}", getOrStartRealityCheckRequest.playerId(), e);
            return new RestResponse(e.getMessage(), e);
        }
    }

    @PostMapping("/acknowledge/{playerId}")
    @ResponseBody
    public RestResponse acknowledge(@PathVariable long playerId) {
        try {
            RealityCheckSession realityCheckSession = realityCheckService.acknowledge(playerId);
            return new RestResponse(realityCheckSession);
        } catch (RealityCheckException e) {
            return new RestResponse(e.getMessage());
        }
    }
}
