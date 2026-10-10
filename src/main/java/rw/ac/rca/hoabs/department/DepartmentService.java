package rw.ac.rca.hoabs.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.common.exception.ApiException;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getActiveDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(DepartmentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(DepartmentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(UUID id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department not found with id: " + id));
        return DepartmentDto.from(dept);
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.name())) {
            throw ApiException.conflict("Department with name '" + dto.name() + "' already exists.");
        }
        Department d = new Department();
        d.setName(dto.name().trim());
        d.setDescription(dto.description());
        d.setOpeningTime(dto.openingTime());
        d.setClosingTime(dto.closingTime());
        d.setActive(dto.active() != null ? dto.active() : true);
        return DepartmentDto.from(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentDto updateDepartment(UUID id, DepartmentDto dto) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department not found with id: " + id));

        if (!d.getName().equalsIgnoreCase(dto.name()) && departmentRepository.existsByNameIgnoreCase(dto.name())) {
            throw ApiException.conflict("Department with name '" + dto.name() + "' already exists.");
        }

        d.setName(dto.name().trim());
        d.setDescription(dto.description());
        d.setOpeningTime(dto.openingTime());
        d.setClosingTime(dto.closingTime());
        if (dto.active() != null) {
            d.setActive(dto.active());
        }
        return DepartmentDto.from(departmentRepository.save(d));
    }
}
