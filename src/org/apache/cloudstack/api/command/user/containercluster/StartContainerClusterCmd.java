// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.api.command.user.containercluster;

import com.cloud.containercluster.CcsEventTypes;
import com.cloud.containercluster.ContainerCluster;
import com.cloud.containercluster.ContainerClusterService;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ContainerClusterResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = StartContainerClusterCmd.APINAME, description = "Starts a stopped container cluster",
        responseObject = ContainerClusterResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {ContainerCluster.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class StartContainerClusterCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(StartContainerClusterCmd.class.getName());

    public static final String APINAME = "startContainerCluster";

    @Inject
    public ContainerClusterService containerClusterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ContainerClusterResponse.class,
            description = "the ID of the container cluster")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return CcsEventTypes.EVENT_CONTAINER_CLUSTER_START;
    }

    @Override
    public String getEventDescription() {
        return "Starting container cluster id: " + getId();
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public ContainerCluster validateRequest() {
        if (getId() == null || getId() < 1L) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid container cluster ID provided");
        }
        final ContainerCluster containerCluster = containerClusterService.findById(getId());
        if (containerCluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Given container cluster was not found");
        }
        return containerCluster;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final ContainerCluster containerCluster = validateRequest();
        try {
            containerClusterService.startContainerCluster(getId().longValue(), false);
            final ContainerClusterResponse response = containerClusterService.createContainerClusterResponse(getId());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InsufficientCapacityException | ResourceUnavailableException | ManagementServerException  ex) {
            s_logger.warn("Failed to start container cluster:" + containerCluster.getUuid() + " due to " + ex.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to start container cluster:" + containerCluster.getUuid(), ex);
        }
    }

}
