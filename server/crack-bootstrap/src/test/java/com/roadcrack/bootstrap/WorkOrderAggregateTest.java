package com.roadcrack.bootstrap;

import com.roadcrack.api.enums.DamageType;
import com.roadcrack.api.enums.DepartmentCode;
import com.roadcrack.api.enums.SeverityLevel;
import com.roadcrack.api.enums.WorkOrderStatus;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.model.WorkOrderAggregate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkOrderAggregateTest {

    @Test
    void shouldCompleteStandardLifecycleAndRecordStatusLogs() {
        WorkOrderAggregate aggregate = createAggregate();

        aggregate.assign(DepartmentCode.ROAD_ADMIN, "zhangsan");
        aggregate.updateStatus(WorkOrderStatus.IN_PROGRESS, "arrived on site");
        aggregate.updateStatus(WorkOrderStatus.COMPLETED, "repair finished");
        aggregate.closeByReport("report submitted");

        assertEquals(WorkOrderStatus.CLOSED, aggregate.getStatus());
        assertEquals(4, aggregate.toResponse().statusLogs().size());
        assertEquals(WorkOrderStatus.PENDING_ASSIGNMENT, aggregate.toResponse().statusLogs().get(0).fromStatus());
        assertEquals(WorkOrderStatus.ASSIGNED, aggregate.toResponse().statusLogs().get(0).toStatus());
        assertEquals(WorkOrderStatus.COMPLETED, aggregate.toResponse().statusLogs().get(3).fromStatus());
        assertEquals(WorkOrderStatus.CLOSED, aggregate.toResponse().statusLogs().get(3).toStatus());
    }

    @Test
    void shouldRejectClosingWorkOrderDirectlyFromStatusUpdate() {
        WorkOrderAggregate aggregate = createAggregate();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aggregate.updateStatus(WorkOrderStatus.CLOSED, "close directly"));

        assertEquals(ResultCode.CONFLICT.code(), exception.getCode());
    }

    @Test
    void shouldRejectStatusUpdateToCancelledViaGeneralEndpoint() {
        WorkOrderAggregate aggregate = createAggregate();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aggregate.updateStatus(WorkOrderStatus.CANCELLED, "cancel directly"));

        assertEquals(ResultCode.CONFLICT.code(), exception.getCode());
    }

    @Test
    void shouldRejectCancellingClosedWorkOrder() {
        WorkOrderAggregate aggregate = createAggregate();
        aggregate.assign(DepartmentCode.ROAD_ADMIN, "lisi");
        aggregate.updateStatus(WorkOrderStatus.IN_PROGRESS, "started");
        aggregate.updateStatus(WorkOrderStatus.COMPLETED, "done");
        aggregate.closeByReport("report created");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aggregate.cancel("cancel after close"));

        assertEquals(ResultCode.CONFLICT.code(), exception.getCode());
    }

    private WorkOrderAggregate createAggregate() {
        LocalDateTime now = LocalDateTime.now();
        return new WorkOrderAggregate(
                1L,
                "WO-20260708-000001",
                101L,
                "aggregate test work order",
                DamageType.CRACK,
                SeverityLevel.HIGH,
                "Changsha test road",
                DepartmentCode.ROAD_ADMIN,
                "http://example.com/evidence.jpg",
                "aggregate test",
                now.plusDays(1),
                now
        );
    }
}
