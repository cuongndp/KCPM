# Phân hoạch lớp tương đương và phân tích giá trị biên

## 1. Phạm vi và quy ước

Tài liệu áp dụng kỹ thuật **Phân hoạch lớp tương đương (EP)** và **Phân
tích giá trị biên (BVA)** cho năm API quản lý bác sĩ trong collection
Postman **Đức Cường**:

1.  `GET /api/admin/doctors`
2.  `GET /api/admin/users/doctor-candidates`
3.  `POST /api/admin/doctors`
4.  `DELETE /api/admin/doctors/{doctorId}/availability`
5.  `PATCH /api/admin/doctors/{doctorId}/availability`

Nội dung chỉ gồm bảng phân hoạch lớp tương đương và bảng phân tích giá
trị biên; không thiết kế test case và không viết mã kiểm thử.

### Quy ước

-   Giả định request đã có thông tin xác thực hợp lệ với vai trò
    `ADMIN`; không phân hoạch token, quyền truy cập hoặc lỗi hệ thống
    ngoài phạm vi đầu vào.
-   `doctorId`, `userId` là ID kiểu `Long`. Nếu mã nguồn không quy định
    miền min/max nghiệp vụ thì không áp dụng BVA số học cho các ID; phân
    hoạch theo khả năng chuyển đổi và sự tồn tại của bản ghi.
-   Các trường của `POST /api/admin/doctors` được trình bày theo cấu
    trúc request đã sử dụng trong collection: `userId`,
    `specialization`, `qualification`, `experience`. Cần đối chiếu
    DTO/validation thực tế nếu collection hoặc backend có thay đổi.
-   Với hai API availability, nội dung dưới đây phân hoạch chắc chắn
    theo `doctorId`. Phần dữ liệu body được ghi theo trường hợp endpoint
    PATCH nhận trạng thái khả dụng dạng boolean; nếu API thực tế không
    có body hoặc dùng tên/kiểu trường khác, cần thay bằng đúng DTO của
    backend.
-   Tag `V` biểu thị lớp hợp lệ/được API chấp nhận; tag `X` biểu thị lớp
    không hợp lệ hoặc bị nghiệp vụ từ chối. Tag được đánh số riêng trong
    từng API.
-   BVA chỉ áp dụng khi có ngưỡng min/max được quy định rõ. Không tự đặt
    giới hạn nghiệp vụ khi mã nguồn không khai báo.

------------------------------------------------------------------------

## 2. GET `/api/admin/doctors`

### 2.1. Bảng phân hoạch lớp tương đương (EP)

  --------------------------------------------------------------------------
  Đầu vào        Lớp tương      Tag            Lớp tương      Tag
                 đương hợp                     đương không    
                 lệ/được chấp                  hợp lệ/bị từ   
                 nhận                          chối           
  -------------- -------------- -------------- -------------- --------------
  Request (không Gửi request    V1             Không có lớp   ---
  có tham số đầu đúng method và                đầu vào không  
  vào được xác   URL; hệ thống                 hợp lệ được    
  định trong     truy vấn danh                 xác định từ    
  collection)    sách bác sĩ.                  URL hiện tại.  
                 Danh sách có                  Sai method     
                 dữ liệu hoặc                  hoặc sai URL   
                 rỗng đều là                   thuộc kiểm thử 
                 kết quả hợp                   định tuyến,    
                 lệ.                           không phải     
                                               phân hoạch dữ  
                                               liệu đầu vào   
                                               của API này.   

  --------------------------------------------------------------------------

### 2.2. Bảng phân tích giá trị biên (BVA)

  -----------------------------------------------------------------------
  Đầu vào                 Giá trị biên            Kết luận
  ----------------------- ----------------------- -----------------------
  Danh sách bác sĩ trả về Không có ngưỡng min/max Không áp dụng BVA cho
                          đầu vào được khai báo.  đầu vào; có thể dùng EP
                          Danh sách có 0, 1 hoặc  để phân biệt danh sách
                          nhiều phần tử đều là    rỗng và danh sách có dữ
                          kết quả truy vấn.       liệu.

  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 3. GET `/api/admin/users/doctor-candidates`

### 3.1. Bảng phân hoạch lớp tương đương (EP)

  --------------------------------------------------------------------------
  Đầu vào        Lớp tương      Tag            Lớp tương      Tag
                 đương hợp                     đương không    
                 lệ/được chấp                  hợp lệ/bị từ   
                 nhận                          chối           
  -------------- -------------- -------------- -------------- --------------
  Request (không Gửi request    V1             Không có lớp   ---
  có tham số đầu đúng method và                đầu vào không  
  vào được xác   URL; hệ thống                 hợp lệ được    
  định trong     trả về danh                   xác định từ    
  collection)    sách tài khoản                URL hiện tại.  
                 đủ điều kiện                  Sai method     
                 tạo hồ sơ bác                 hoặc sai URL   
                 sĩ. Danh sách                 thuộc kiểm thử 
                 rỗng vẫn là                   định tuyến.    
                 kết quả hợp                                  
                 lệ.                                          

  --------------------------------------------------------------------------

### 3.2. Bảng phân tích giá trị biên (BVA)

  -----------------------------------------------------------------------
  Đầu vào                 Giá trị biên            Kết luận
  ----------------------- ----------------------- -----------------------
  Danh sách ứng viên bác  Không có ngưỡng min/max Không áp dụng BVA số
  sĩ                      đầu vào được khai báo;  học. Phân biệt danh
                          số lượng ứng viên phụ   sách rỗng và danh sách
                          thuộc dữ liệu hiện có.  có dữ liệu bằng EP.

  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 4. POST `/api/admin/doctors`

### 4.1. Bảng phân hoạch lớp tương đương (EP)

  ------------------------------------------------------------------------------
  Đầu vào/điều kiện  Lớp tương      Tag            Lớp tương      Tag
                     đương hợp                     đương không    
                     lệ/được chấp                  hợp lệ/bị từ   
                     nhận                          chối           
  ------------------ -------------- -------------- -------------- --------------
  `userId` (body,    ID kiểu `Long` V1             Thiếu/null     X1
  bắt buộc)          của tài khoản                 hoặc giá trị   
                     tồn tại và đủ                 không chuyển   
                     điều kiện tạo                 đổi được sang  
                     hồ sơ bác sĩ.                 `Long`.        

  `userId` (body,    ---            ---            ID kiểu `Long` X2
  bắt buộc)                                        không có tài   
                                                   khoản tương    
                                                   ứng.           

  `userId` (body,    ---            ---            Tài khoản đã   X3
  bắt buộc)                                        có hồ sơ bác   
                                                   sĩ hoặc không  
                                                   thuộc nhóm tài 
                                                   khoản đủ điều  
                                                   kiện.          

  `specialization`   Chuỗi chuyên   V2             Thiếu/null nếu X4
  (body)             môn hợp lệ                    trường được    
                     theo quy định                 khai báo bắt   
                     của backend.                  buộc; sai kiểu 
                                                   dữ liệu nếu    
                                                   không phải     
                                                   chuỗi.         

  `qualification`    Chuỗi trình    V3             Thiếu/null nếu X5
  (body)             độ/chứng chỉ                  trường được    
                     hợp lệ theo                   khai báo bắt   
                     quy định của                  buộc; sai kiểu 
                     backend.                      dữ liệu nếu    
                                                   không phải     
                                                   chuỗi.         

  `experience`       Giá trị số     V4             Thiếu/null nếu X6
  (body)             nguyên không                  trường bắt     
                     âm và nằm                     buộc; sai kiểu 
                     trong miền                    dữ liệu hoặc   
                     backend cho                   giá trị âm.    
                     phép.                                        

  Toàn bộ body       Body JSON hợp  V5             Body rỗng,     X7
                     lệ, có đủ                     JSON sai cú    
                     trường bắt                    pháp hoặc      
                     buộc và các                   thiếu trường   
                     giá trị thỏa                  bắt buộc.      
                     mãn điều kiện                                
                     nghiệp vụ.                                   
  ------------------------------------------------------------------------------

> Lưu ý: Mã nguồn/DTO có thể quy định thêm `@NotNull`, `@NotBlank`,
> `@Min`, `@Max` hoặc giới hạn độ dài cho các trường. Chỉ sử dụng các
> lớp X tương ứng nếu những ràng buộc đó thực sự tồn tại. Trường nào
> không có validation bắt buộc thì không kết luận rằng thiếu trường chắc
> chắn bị từ chối.

### 4.2. Bảng phân tích giá trị biên (BVA)

  -------------------------------------------------------------------------------------
  Đầu vào             `min-` /   `min` /    `min+` /   Giá trị    `max-`,    Tag / kết
                      sát dưới   tại ngưỡng sát trên   đại diện   `max`,     luận
                      ngưỡng                ngưỡng     hợp lệ     `max+`     
  ------------------- ---------- ---------- ---------- ---------- ---------- ----------
  `experience`        `-1` ---   `0` ---    `1` ---    `2` ---    Không xác  BVA áp
                      không hợp  hợp lệ nếu hợp lệ nếu giá trị    định nếu   dụng tại
                      lệ nếu     mức kinh   các điều   đại diện   backend    ngưỡng 0;
                      backend    nghiệm tối kiện khác  hợp lệ     không khai không tự
                      yêu cầu    thiểu là   thỏa mãn.  theo dữ    báo giới   đặt max
                      không âm.  0.                    liệu       hạn trên.  khi chưa
                                                       request               có ràng
                                                       mẫu.                  buộc.

  `userId`            Không xác  Không xác  Không xác  ID tài     Không xác  Không áp
                      định.      định.      định.      khoản tồn  định.      dụng BVA
                                                       tại, đủ               số học: ID
                                                       điều kiện.            là khóa
                                                                             tra cứu.

  `specialization`,   Không xác  Không xác  Không xác  Chuỗi có   Không xác  Chỉ áp
  `qualification`     định nếu   định.      định.      nội dung   định nếu   dụng BVA
                      không có                         hợp lệ.    DTO không  độ dài khi
                      giới hạn                                    khai báo   có `@Size`
                      độ dài.                                     độ dài tối hoặc giới
                                                                  đa.        hạn tương
                                                                             ứng.
  -------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 5. DELETE `/api/admin/doctors/{doctorId}/availability`

### 5.1. Bảng phân hoạch lớp tương đương (EP)

  --------------------------------------------------------------------------
  Đầu vào        Lớp tương      Tag            Lớp tương      Tag
                 đương hợp                     đương không    
                 lệ/được chấp                  hợp lệ/bị từ   
                 nhận                          chối           
  -------------- -------------- -------------- -------------- --------------
  `doctorId`     ID kiểu `Long` V1             Giá trị path   X1
  (path, bắt     của bác sĩ tồn                không chuyển   
  buộc)          tại và có dữ                  đổi được sang  
                 liệu                          `Long` hoặc    
                 availability                  vượt miền biểu 
                 phù hợp để                    diễn `Long`.   
                 xóa/ngừng áp                                 
                 dụng.                                        

  `doctorId`     ---            ---            ID kiểu `Long` X2
  (path, bắt                                   không có bác   
  buộc)                                        sĩ tương ứng.  

  Availability   Bác sĩ có dữ   V2             Không có dữ    X3
  của bác sĩ     liệu                          liệu           
                 availability                  availability   
                 và thao tác                   tương ứng hoặc 
                 xóa được                      trạng thái     
                 backend chấp                  hiện tại không 
                 nhận.                         cho phép xóa,  
                                               nếu service có 
                                               kiểm tra điều  
                                               kiện này.      
  --------------------------------------------------------------------------

### 5.2. Bảng phân tích giá trị biên (BVA)

  -----------------------------------------------------------------------
  Đầu vào                 Giá trị biên            Kết luận
  ----------------------- ----------------------- -----------------------
  `doctorId`              Không có ngưỡng min/max Không áp dụng BVA số
                          nghiệp vụ; đây là ID    học; dùng EP để phân
                          dùng để tra cứu bác sĩ. biệt ID hợp lệ và không
                                                  tồn tại.

  Availability            Chưa có miền số hoặc    Không áp dụng BVA nếu
                          ngưỡng min/max được xác endpoint chỉ xóa dữ
                          định.                   liệu availability.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 6. PATCH `/api/admin/doctors/{doctorId}/availability`

### 6.1. Bảng phân hoạch lớp tương đương (EP)

  --------------------------------------------------------------------------
  Đầu vào        Lớp tương      Tag            Lớp tương      Tag
                 đương hợp                     đương không    
                 lệ/được chấp                  hợp lệ/bị từ   
                 nhận                          chối           
  -------------- -------------- -------------- -------------- --------------
  `doctorId`     ID kiểu `Long` V1             Giá trị path   X1
  (path, bắt     của bác sĩ tồn                không chuyển   
  buộc)          tại.                          đổi được sang  
                                               `Long` hoặc    
                                               vượt miền biểu 
                                               diễn `Long`.   

  `doctorId`     ---            ---            ID kiểu `Long` X2
  (path, bắt                                   không có bác   
  buộc)                                        sĩ tương ứng.  

  Trạng thái     Giá trị        V2             Thiếu/null nếu X3
  availability   boolean `true`                trường bắt     
  (body, nếu     hoặc `false`                  buộc; giá trị  
  endpoint nhận  theo đúng cấu                 không chuyển   
  boolean)       trúc request                  đổi được sang  
                 của API.                      boolean hoặc   
                                               sai kiểu JSON. 

  Body request   JSON đúng cú   V3             Body rỗng,     X4
  (nếu endpoint  pháp, đúng tên                JSON sai cú    
  yêu cầu body)  trường và kiểu                pháp hoặc      
                 dữ liệu theo                  tên/kiểu       
                 DTO.                          trường không   
                                               phù hợp DTO.   
  --------------------------------------------------------------------------

### 6.2. Bảng phân tích giá trị biên (BVA)

  -----------------------------------------------------------------------
  Đầu vào                 Giá trị biên            Kết luận
  ----------------------- ----------------------- -----------------------
  `doctorId`              Không có ngưỡng min/max Không áp dụng BVA số
                          nghiệp vụ; đây là ID    học; dùng EP để phân
                          tra cứu bác sĩ.         biệt ID tồn tại và
                                                  không tồn tại.

  Trạng thái availability Kiểu boolean chỉ có hai Không áp dụng BVA; dùng
                          giá trị `true` và       EP để phân hoạch hai
                          `false`, không có miền  giá trị boolean và dữ
                          số liên tục hoặc ngưỡng liệu sai kiểu.
                          min/max.                
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 7. Ghi chú cần đối chiếu với mã nguồn

Một số thông tin không thể xác định chỉ từ tên endpoint trong collection
Postman. Trước khi chốt tài liệu, cần đối chiếu controller, DTO và
service để xác nhận:

1.  `GET /api/admin/doctors` và `GET /api/admin/users/doctor-candidates`
    có query parameter/filter nào hay không.
2.  Các trường bắt buộc, kiểu dữ liệu và giới hạn của `specialization`,
    `qualification`, `experience` trong request tạo bác sĩ.
3.  `DELETE .../availability` xóa toàn bộ availability hay chỉ xóa một
    loại dữ liệu/lịch cụ thể; service có kiểm tra availability tồn tại
    hay không.
4.  `PATCH .../availability` có nhận body hay không; nếu có, xác nhận
    chính xác tên trường và kiểu dữ liệu.
5.  Cách backend phản hồi khi ID không tồn tại, dữ liệu trùng hoặc vi
    phạm nghiệp vụ.

Các bảng trên là bản phân hoạch ban đầu dựa trên năm endpoint và cấu
trúc request tạo bác sĩ đã biết. Không nên xem các giả định về
body/validation của hai API availability là đặc tả chắc chắn nếu chưa
đối chiếu mã nguồn.
