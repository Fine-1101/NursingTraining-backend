package org.example.nursingtrainingbackend.modules.course.dto;

import lombok.Data;

@Data
public class DepartmentDTO {
    private Long departmentId;
    private Boolean required;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
