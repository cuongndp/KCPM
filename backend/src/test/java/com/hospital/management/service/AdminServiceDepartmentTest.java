package com.hospital.management.service;

import com.hospital.management.model.Department;
import com.hospital.management.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceDepartmentTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void WB_DEP_01_savesDepartmentWhenNameIsNew() {
        Department input = mock(Department.class);
        Department saved = mock(Department.class);

        when(input.getName()).thenReturn("Tim mach");
        when(departmentRepository.existsByName("Tim mach")).thenReturn(false);
        when(departmentRepository.save(input)).thenReturn(saved);

        Department result = adminService.createDepartment(input);

        assertSame(saved, result);
        verify(departmentRepository).existsByName("Tim mach");
        verify(departmentRepository).save(input);
    }

    @Test
    void WB_DEP_02_throwsWhenDepartmentNameAlreadyExists() {
        Department input = mock(Department.class);

        when(input.getName()).thenReturn("Tim mach");
        when(departmentRepository.existsByName("Tim mach")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adminService.createDepartment(input)
        );

        assertEquals(
                "Department with this name already exists",
                exception.getMessage()
        );
        verify(departmentRepository).existsByName("Tim mach");
        verify(departmentRepository, never()).save(input);
    }
}