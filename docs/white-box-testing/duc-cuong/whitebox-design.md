# Báo Cáo Thiết Kế Kiểm Thử Hộp Trắng (White-box Testing)
**Mã nhiệm vụ Jira:** KAN-87  
**Thành viên thực hiện:** Nguyễn Đức Cường  
**Đối tượng kiểm thử:** `AdminService.createDoctorWithDepartment(CreateDoctorRequest request, Long departmentId)`  
**Tệp mã nguồn:** `backend/src/main/java/com/hospital/management/service/AdminService.java`  
**Tệp Unit Test:** `backend/src/test/java/com/hospital/management/service/CreateDoctorTest.java`  

---

## 1. Phương thức mục tiêu (Target Method)

```java
public Doctor createDoctorWithDepartment(CreateDoctorRequest request, Long departmentId) {
    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
    if (user.getRole() != User.Role.DOCTOR) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản được chọn không có vai trò bác sĩ.");
    }
    if (doctorRepository.existsByUserId(user.getId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản này đã có hồ sơ bác sĩ.");
    }

    Department department = getDepartmentById(departmentId);
    Doctor doctor = new Doctor();
    doctor.setUser(user);
    doctor.setDepartment(department);
    doctor.setSpecialization(request.getSpecialization());
    doctor.setQualification(request.getQualification());
    doctor.setExperience(request.getExperience());
    doctor.setConsultationFee(0.0);
    doctor.setAvailable(true);
    return doctorRepository.save(doctor);
}
```

---

## 2. Đồ thị dòng điều khiển (Control Flow Graph - CFG)

```mermaid
flowchart TD
    N1["Node 1: Entry - userRepository.findById(userId)"] -->|User Not Found| N2["Node 2: throw ResourceNotFoundException"]
    N1 -->|User Found| N3{"Node 3: user.getRole() != DOCTOR?"}
    N3 -->|True (Role != DOCTOR)| N4["Node 4: throw ResponseStatusException(BAD_REQUEST)"]
    N3 -->|False (Role == DOCTOR)| N5{"Node 5: doctorRepository.existsByUserId()?"}
    N5 -->|True (Profile exists)| N6["Node 6: throw ResponseStatusException(CONFLICT)"]
    N5 -->|False (Not exists)| N7["Node 7: getDepartmentById(departmentId)"]
    N7 -->|Dept Not Found| N2B["throw ResourceNotFoundException('Department')"]
    N7 -->|Dept Found| N8["Node 8: doctorRepository.save(doctor)"]
    N8 --> N9["Node 9: Return Doctor (Exit)"]
```

---

## 3. Tính toán độ phức tạp chu trình Cyclomatic Complexity $V(G)$
* Số đỉnh (Nodes) $N = 8$
* Số cạnh (Edges) $E = 11$
* Số thành phần liên thông $P = 1$
* Số nút rẽ nhánh điều kiện $P_N = 4$

Áp dụng công thức:
$$V(G) = E - N + 2P = 11 - 8 + 2(1) = 5$$
hoặc:
$$V(G) = P_N + 1 = 4 + 1 = 5$$

$\rightarrow$ Cần **5 đường đi cơ sở (Basis Paths)** để đạt 100% độ bao phủ (Branch & Statement Coverage).

---

## 4. Tập đường đi cơ sở (Basis Paths) & Test Case Ma Trận

| Basis Path | Chuỗi đỉnh đi qua | Dữ liệu kiểm thử | Kết quả mong đợi | Tương ứng Test Method |
| :--- | :--- | :--- | :--- | :--- |
| **Path 1** | `1 -> 2` | `userId = 5L` (User không tồn tại) | Ném `ResourceNotFoundException("User", 5)` | `testCreateDoctor_UserNotFound()` |
| **Path 2** | `1 -> 3 -> 4` | User tồn tại nhưng có `role = PATIENT` | Ném `ResponseStatusException(BAD_REQUEST)` | `testCreateDoctor_RoleNotDoctor()` |
| **Path 3** | `1 -> 3 -> 5 -> 6` | User role DOCTOR nhưng `existsByUserId = true` | Ném `ResponseStatusException(CONFLICT)` | `testCreateDoctor_ProfileExists()` |
| **Path 4** | `1 -> 3 -> 5 -> 7 -> Dept NotFound` | User hợp lệ nhưng khoa không tồn tại | Ném `ResourceNotFoundException("Department", 1)` | `testCreateDoctor_DepartmentNotFound()` |
| **Path 5** | `1 -> 3 -> 5 -> 7 -> 8 -> 9` | Mọi điều kiện hợp lệ | Lưu và trả về đối tượng Doctor mới | `testCreateDoctor_Success()` |

---

## 5. Kết quả thực thi Unit Test & Minh chứng
* Tệp kiểm thử: `backend/src/test/java/com/hospital/management/service/CreateDoctorTest.java`
* Sử dụng: **JUnit 5 + Mockito**
* Độ bao phủ: **100% Instruction Coverage**, **100% Branch Coverage**.

### Hình ảnh minh chứng (Jira Tasks):
- **CFG Diagram (Jira KAN-90):** `docs/white-box-testing/duc-cuong/evidence/CFG_AdminService_createDoctorWithDepartment.png`
- **JUnit 5 Test Pass (Jira KAN-91):** `docs/white-box-testing/duc-cuong/evidence/JUnit5_CreateDoctorTest_Pass.png`
