# 🏠 Homie Finance - Personal Finance Management System

Hệ thống Backend quản lý tài chính cá nhân được xây dựng trên nền tảng **Java Spring Boot**, tích hợp các cơ chế bảo mật hiện đại và khả năng quản lý chi tiêu thông minh.

---

## 🛠 1. Công nghệ sử dụng (Tech Stack)

* **Core:** Java 17, Spring Boot 3.x
* **Database:** PostgreSQL / MySQL, Spring Data JPA
* **Security:** Spring Security, JSON Web Token (JWT) - Access & Refresh Token
* **Cloud Storage:** Cloudinary (Lưu trữ ảnh hóa đơn)
* **Reporting:** Apache POI (Xuất file Excel .xlsx)
* **Communication:** JavaMailSender (Gửi Email chào mừng & OTP)
* **Documentation:** SpringDoc OpenAPI (Swagger UI)

---

## ✨ 2. Tính năng nổi bật

### 🔒 Bảo mật và Người dùng
* **Xác thực đa phương thức:** Đăng nhập truyền thống (Email/Password) và Google Social Login.
* **Cơ chế Token kép:** Sử dụng **Access Token** (ngắn hạn) và **Refresh Token** (dài hạn) giúp duy trì phiên đăng nhập an toàn.
* **Logout an toàn:** Vô hiệu hóa Access Token bằng cơ chế **Blacklist** lưu trữ trong Database.
* **Khôi phục tài khoản:** Quy trình quên mật khẩu xác thực qua OTP 6 số gửi về Email, tích hợp **Rate Limiting** (giới hạn tần suất gửi mã).

### 💰 Quản lý Tài chính
* **Giao dịch:** Ghi chép thu chi, phân loại danh mục, tìm kiếm và phân trang lịch sử giao dịch.
* **Hệ thống Ngân sách (Budgeting):** Thiết lập hạn mức chi tiêu cho từng danh mục theo tháng. Tự động chặn nếu giao dịch vượt mức.
* **Thống kê & Báo cáo:** Thống kê tổng thu/chi theo thời gian và xuất file **Excel** chuyên nghiệp.
* **Quản lý hóa đơn:** Upload và quản lý ảnh hóa đơn trực tiếp lên Cloudinary.

---


## 🚀 3. Cài đặt và Chạy dự án

### Yêu cầu hệ thống
* **JDK:** 17 trở lên.
* **Maven:** 3.6 trở lên.
* **Database:** PostgreSQL hoặc MySQL.

### Các bước cài đặt chi tiết

1.  **Clone dự án:**
    ```bash
    git clone [https://github.com/lehai170504/Finance-Web.git](https://github.com/lehai170504/Finance-Web.git)
    cd Finance-Web
    ```

2.  **Cấu hình Cơ sở dữ liệu:**
    Tạo một database tên là `homie_finance` trong hệ quản trị CSDL của bạn.

3.  **Cấu hình Biến môi trường:**
    Mở file `src/main/resources/application.properties` và cập nhật thông tin:
    ```properties
    # Database Configuration
    spring.datasource.url=jdbc:mysql://localhost:3306/homie_finance
    spring.datasource.username=YOUR_DB_USERNAME
    spring.datasource.password=YOUR_DB_PASSWORD

    # Mail Configuration (Gmail App Password)
    spring.mail.host=smtp.gmail.com
    spring.mail.port=587
    spring.mail.username=your-email@gmail.com
    spring.mail.password=your-app-password

    # Cloudinary Configuration
    cloudinary.cloud_name=your_cloud_name
    cloudinary.api_key=your_api_key
    cloudinary.api_secret=your_api_secret

    # Google Login
    google.client-id=your-google-client-id
    ```

4.  **Build và Chạy dự án:**
    Sử dụng Terminal hoặc chạy trực tiếp từ IDE:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

5.  **Truy cập tài liệu API:**
    Sau khi ứng dụng khởi động thành công tại port 8080:
    * **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 4. Cấu trúc thư mục (Project Structure)
   Dự án tuân thủ cấu trúc thư mục chuẩn của Spring Boot để dễ dàng bảo trì và mở rộng:

config/: Cấu hình Security, CORS, Cloudinary, Swagger.

controller/: Nơi tiếp nhận và điều hướng các Request từ Client.

service/: Chứa các logic nghiệp vụ (Business Logic) cốt lõi.

repository/: Tầng giao tiếp với Database thông qua Spring Data JPA.

entity/: Khai báo các thực thể (Table) trong Database.

dto/: Data Transfer Object - Chứa các yêu cầu và phản hồi API.

util/: Các công cụ hỗ trợ (JwtUtil, CloudinaryUtil).

exception/: Xử lý lỗi tập trung (Global Exception Handling).

## 📦 5. Cấu trúc Response chuẩn

Hệ thống sử dụng cấu trúc phản hồi đồng nhất (Unified Response) giúp Frontend dễ dàng xử lý:

```json
{
  "status": 200,
  "message": "Thông báo thành công",
  "data": {
    "id": "e237be...",
    "amount": 500000,
    "description": "Mua sắm cuối tuần",
    "createdAt": "2026-03-19T10:30:00Z"
  }
}

