package com.hjq.CodeLess.model.dto.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppUpdateRequestPermissionTest {

    @Test
    void userUpdateAllowsCoverButCannotCarryPriority() {
        assertDoesNotThrow(() -> AppUpdateRequest.class.getDeclaredField("cover"));
        assertThrows(NoSuchFieldException.class,
                () -> AppUpdateRequest.class.getDeclaredField("priority"));
    }
}
