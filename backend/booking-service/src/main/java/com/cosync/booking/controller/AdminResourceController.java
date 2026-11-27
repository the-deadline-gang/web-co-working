package com.cosync.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.resource.ResourceRequest;
import com.cosync.booking.dto.resource.ResourceResponse;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.ResourceService;

import jakarta.validation.Valid;




@RestController 
@RequestMapping("/admin/resources")
public class AdminResourceController {
    public final ResourceService resourceService;

    public AdminResourceController( ResourceService resourceService){
        this.resourceService = resourceService;
    }

    @GetMapping
    @RequirePermission("resource.manage")
    public List<ResourceResponse> list(@RequestParam(required = false) ServiceType serviceType,
                                    @RequestParam(required = false) Long zoneId) {
        return resourceService.listResource(serviceType, zoneId);
    }

    @PostMapping()
    @RequirePermission("resource.manage")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createResource(  @Valid @RequestBody ResourceRequest req) {
        return resourceService.creatResource(req);
    }

    @PutMapping("/{resourceId}")
    @RequirePermission("resource.manage")
    public ResourceResponse updateResource(@PathVariable Long resourceId, @Valid @RequestBody ResourceRequest req) {
        return resourceService.update(resourceId, req);
    }
    

}
