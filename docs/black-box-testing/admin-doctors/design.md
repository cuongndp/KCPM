# Báo Cáo Thiết Kế Kiểm Thử Black-box & White-box
**Mã nhiệm vụ Jira:** KAN-87  
**Thành viên thực hiện:** Nguyễn Đức Cường  
**Đối tượng kiểm thử:** Chức năng Tạo Bác Sĩ Mới (`createDoctorWithDepartment`)  
**Tệp mã nguồn:** `backend/src/main/java/com/hospital/management/service/AdminService.java`  
**Tệp Unit Test:** `backend/src/test/java/com/hospital/management/service/AdminServiceTest.java`  

---

## PHẦN 1: THIẾT KẾ KIỂM THỬ HỘP ĐEN (BLACK-BOX TESTING)

### 1.1. Phân hoạch tương đương (Equivalence Partitioning - EP)
Dữ liệu đầu vào của API `POST /api/admin/doctors?departmentId={departmentId}`:
* **departmentId:**
  * EP1 (Hợp lệ): ID khoa tồn tại trong CSDL ($departmentId > 0$).
  * EP2 (Không hợp lệ): ID khoa không tồn tại trong CSDL ($departmentId = 99999$).
* **userId (trong Body):**
  * EP3 (Hợp lệ): User tồn tại, Role = DOCTOR, chưa có hồ sơ bác sĩ.
  * EP4 (Không hợp lệ): User không tồn tại.
  * EP5 (Không hợp lệ): User có Role khác DOCTOR (PATIENT / ADMIN).
  * EP6 (Không hợp lệ): User đã có hồ sơ bác sĩ trong bảng `doctors`.
* **specialization (Chuyên môn):**
  * EP7 (Hợp lệ): Chuỗi ký tự độ dài từ 1 đến 255.
  * EP8 (Không hợp lệ): Rỗng hoặc null.

$\rightarrow$ **Số lượng test case tối thiểu để phủ 100% các lớp tương đương:** **6 test cases**.

---

### 1.2. Bảng quyết định (Decision Table)

| Điều kiện (Conditions) | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| :--- | :---: | :---: | :---: | :---: | :---: |
| C1: User tồn tại? | **F** | **T** | **T** | **T** | **T** |
| C2: User có Role DOCTOR? | - | **F** | **T** | **T** | **T** |
| C3: User chưa có Doctor Profile? | - | - | **F** | **T** | **T** |
| C4: Department tồn tại? | - | - | - | **F** | **T** |
| C5: specialization hợp lệ? | - | - | - | - | **T** |
| **Hành động (Actions)** | | | | | |
| A1: Ném ResourceNotFoundException (User 404) | **X** | | | | |
| A2: Ném ResponseStatusException (Role 400) | | **X** | | | |
| A3: Ném ResponseStatusException (Profile Exist 409) | | | **X** | | |
| A4: Ném ResourceNotFoundException (Dept 404) | | | | **X** | |
| A5: Lưu bác sĩ thành công (HTTP 200 OK) | | | | | **X** |

$\rightarrow$ **Số lượng test case tối thiểu để phủ 100% Rules:** **5 test cases**.

---

## PHẦN 2: THIẾT KẾ KIỂM THỬ HỘP TRẮNG (WHITE-BOX TESTING)

### 2.1. Đồ thị dòng điều khiển (Control Flow Graph - CFG)
Phương thức kiểm thử: `AdminService.createDoctorWithDepartment(CreateDoctorRequest request, Long departmentId)`

```mermaid
flowchart TD
    N1["Node 1: Bắt đầu & userRepository.findById(userId)"] -->|User không tìm thấy| N2["Node 2: throw ResourceNotFoundException('User')"]
    N1 -->|User tìm thấy| N3{"Node 3: user.getRole() != DOCTOR?"}
    N3 -->|Đúng (Role khác)| N4["Node 4: throw ResponseStatusException(BAD_REQUEST)"]
    N3 -->|Sai (Role DOCTOR)| N5{"Node 5: doctorRepository.existsByUserId()?"}
    N5 -->|Đúng (Đã có profile)| N6["Node 6: throw ResponseStatusException(CONFLICT)"]
    N5 -->|Sai (Chưa có)| N7["Node 7: getDepartmentById(departmentId)"]
    N7 -->|Dept không tìm thấy| N2B["throw ResourceNotFoundException('Department')"]
    N7 -->|Dept tìm thấy| N8["Node 8: Khởi tạo Doctor & doctorRepository.save()"]
    N8 --> N9["Node 9: Return Doctor (Kết thúc)"]
```

---

### 2.2. Tính toán độ phức tạp chu trình Cyclomatic Complexity $V(G)$
* Số đỉnh (Nodes) $N = 8$
* Số cạnh (Edges) $E = 11$
* Số thành phần liên thông $P = 1$
* Số vị từ rẽ nhánh (Predicate Nodes) $P_N = 4$ (User null?, Role != DOCTOR?, Exists profile?, Dept null?)

$$V(G) = E - N + 2P = 11 - 8 + 2(1) = 5$$
hoặc:
$$V(G) = P_N + 1 = 4 + 1 = 5$$

$\rightarrow$ Hệ thống có **5 đường đi cơ sở (Basis Paths)** cần bao phủ.

---

### 2.3. Tập đường đi cơ sở (Basis Paths Set)

* **Path 1:** `Node 1 -> Node 2`  
  * *Điều kiện:* `userRepository.findById()` trả về rỗng.
  * *Kỳ vọng:* Ném `ResourceNotFoundException`.
* **Path 2:** `Node 1 -> Node 3 -> Node 4`  
  * *Điều kiện:* User tồn tại nhưng có `role = PATIENT`.
  * *Kỳ vọng:* Ném `ResponseStatusException(HttpStatus.BAD_REQUEST)`.
* **Path 3:** `Node 1 -> Node 3 -> Node 5 -> Node 6`  
  * *Điều kiện:* User hợp lệ (DOCTOR) nhưng `doctorRepository.existsByUserId() = true`.
  * *Kỳ vọng:* Ném `ResponseStatusException(HttpStatus.CONFLICT)`.
* **Path 4:** `Node 1 -> Node 3 -> Node 5 -> Node 7 -> Dept NotFound`  
  * *Điều kiện:* User hợp lệ, chưa có profile nhưng `departmentRepository.findById()` rỗng.
  * *Kỳ vọng:* Ném `ResourceNotFoundException`.
* **Path 5 (Đường đi thành công):** `Node 1 -> Node 3 -> Node 5 -> Node 7 -> Node 8 -> Node 9`  
  * *Điều kiện:* User hợp lệ, chưa có profile, khoa hợp lệ.
  * *Kỳ vọng:* Lưu vào `doctorRepository` và trả về đối tượng Doctor mới.

---

### 2.4. Triển khai Unit Test JUnit 5 + Mockito
* Tệp kiểm thử: `src/test/java/com/hospital/management/service/AdminServiceTest.java`
* Kết quả thực thi 5/5 test cases tương ứng với 5 Basis Paths đạt **100% Instruction Coverage** và **100% Branch Coverage** cho phương thức mục tiêu.
