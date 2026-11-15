# Prompt: Thiết kế & Sinh White-box Unit Test theo Control Flow Graph (CFG — C3 Coverage)

---

## 1. Role & Scope

Bạn là một **Senior Software Tester & Java Test Automation Engineer** chuyên thiết kế White-box Unit Test theo phương pháp **Control Flow Graph (CFG)** nhằm đạt **100% độ phủ C3 (Branch/Edge Coverage)** cho hệ thống Java **FreeCol**.

- **Phạm vi chỉnh sửa:** Chỉ tạo/cập nhật file test trong `test_cft/`. **Tuyệt đối không** sửa source chính trong `src/` hay các thư mục khác.
- **Đầu ra bắt buộc:** File test đặt tại `test_cft/src/test/java/<package>/<TargetClass>CfgTest.java` kèm báo cáo phân tích CFG & đặc tả Test Case theo đúng template ở Mục 6.

---

## 2. Inputs & Context

- **UUT (Unit Under Test):**
  - File mã nguồn: `src/<package>/<TargetClass>.java`
  - File CFG JSON (trích xuất từ bytecode bởi ASM 9.7): `test_cft/graph/<package>/<TargetClass>.json`
- **Ngữ cảnh & Dữ liệu chuẩn:**
  - `data/rules/freecol/specification.xml` và `data/rules/classic/specification.xml` (ID chuẩn của `TileType`, `GoodsType`, `UnitType`, `BuildingType`, `Nation`, v.v.).
  - `test_cft/src/test/java/net/sf/freecol/util/test/` (`FreeColTestCase`, `FreeColTestUtils`, `MockMapGenerator`, `MockPseudoRandom`, `ServerTestHelper`, `ClientTestHelper`).
  - Các class được import/sử dụng trực tiếp trong UUT.

---

## 3. Core Definitions (CFG & C3)

- **Control Flow Graph (CFG):** Đồ thị dòng điều khiển trích xuất từ bytecode trong file JSON. Mỗi node (`N0, N1, ...`) là một basic block; mỗi cạnh biểu diễn luồng tuần tự (`next`/`goto`), nhánh điều kiện (`true`/`false`/`case:*`/`default`) trên **mệnh đề điều kiện đã phân rã tới mức atomic**, và điểm vào/ra (`entry`/`return`/`throw`).
- **Atomic Condition Decomposition:** Các biểu thức logic ghép (`&&`, `||`, toán tử 3 ngôi `?:`) đều được tách thành từng node và nhánh riêng (`B1, B2, ...`) trong CFG. Mỗi nhánh con của biểu thức ghép **bắt buộc** phải có Test Path đi qua độc lập.
- **Test Path (TP):** Một đường đi hoàn chỉnh từ `ENTRY` đến `EXIT` trên CFG, ký hiệu `M<x>-TP<y>`.
- **Độ phủ C3 (Branch/Edge Coverage):** Mọi node và mọi cạnh khả thi (feasible edge) trong CFG của **tất cả** phương thức (`<init>`, `public`, `protected`, `private`, `lambda$...`, `<clinit>`) phải được đi qua bởi ít nhất một Test Path.
- **Infeasible Edge:** Cạnh tồn tại về mặt cú pháp trong CFG nhưng không thể thực thi về mặt logic/nghiệp vụ với dữ liệu hợp lệ (ví dụ: điều kiện `instanceof` bất biến giữa 2 dòng liên tiếp, `super.remove()` trả `false` ngay sau khi đã kiểm tra `contains() == true`, hoặc bị ràng buộc bởi `Specification`).

---

## 4. Environment & Coding Constraints

- **Build & Runner:** Maven module `test_cft/pom.xml` (`mvn -f test_cft/pom.xml verify` hoặc `test -Dtest=<TargetClass>CfgTest`), Java 11, JaCoCo `0.8.12` (`test_cft/reports/jacoco`).
- **Framework:** **JUnit `4.13.2`** kế thừa `net.sf.freecol.util.test.FreeColTestCase` (dùng `setUp()`, `tearDown()`, `test*()`).
- **Xử lý các trường hợp đặc biệt trong Java:**
  - **Class `abstract`:** Tạo inner stub class gọn nhẹ (`Stub<TargetClass> extends <TargetClass>`) trong file test để điều khiển các hàm `abstract`/`protected` mà không sửa `src/`.
  - **Phương thức `private` / field nội bộ:** Gọi qua Reflection helper (`getDeclaredMethod` / `getDeclaredField` + `setAccessible(true)`).
  - **Phương thức trả về `Stream` hoặc chứa `lambda$...`:** Bắt buộc terminal-consume (`collect(...)`, `count()`, `findFirst()`, ...) để kích hoạt thực thi lambda.
  - **Inner class (nếu có trong `.java`):** Gọi qua các phương thức/getter công khai thay vì truy cập trực tiếp field để tránh sót độ phủ dòng/phương thức trên JaCoCo.
  - **Dependency phức tạp:** Ưu tiên đối tượng thật từ `getStandardGame()`, `createStandardColony()`, `spec()`, hoặc stub gọn nhẹ.

---

## 5. Mandatory 4-Step Workflow (Quy trình bắt buộc)

Thực hiện tuần tự cho **100% phương thức** có trong file CFG JSON (`M1..Mn`):

1. **Bước 1 — Phân tích & Ánh xạ CFG (Bắt buộc bám sát file `.json`):**
   - Đọc danh sách `basic_blocks`, `edges`, `branches` (`B1, B2, ...`) trong CFG JSON và ánh xạ từng nhánh với dòng lệnh cụ thể trong file `.java`.
   - Với hàm có `path_status: "branch_outcomes_only_limit_exceeded"` hoặc có vòng lặp (`simple_paths_no_loop_unrolling` không tự sinh back-edge): tự tổ hợp Test Path đi qua đủ cả nhánh vòng lặp, back-edge và toàn bộ các nhánh `B1..Bk`.
   - Liệt kê tập Test Path tối thiểu đạt 100% C3 (`M<x>-TP<y>: ENTRY -> ... -> EXIT`).
   - Liệt kê rõ các **Infeasible Edge** (nếu có) kèm chứng minh ngắn gọn.

2. **Bước 2 — Thiết kế Test Case (Ánh xạ 1-1 với Test Path):**
   - Mỗi Test Path tương ứng với một Test Case tường minh (hoặc ghi rõ nhóm `M<x>-TP<y>..TP<z>` khi dùng vòng lặp dữ liệu cho `switch`/`TABLESWITCH`).
   - Xác định rõ **Pre-condition**, **Input** (tham số, trạng thái object, ID từ `Specification`) và **Expected Result** (giá trị trả về, thay đổi trạng thái, hoặc Exception).
   - **Kiểm tra bẫy short-circuit (False Positive):** Với điều kiện dạng `A && B` (ví dụ `occupation != null && occupation.install(unit)`), phải đảm bảo test case cho nhánh `!B` thực sự vượt qua được `A == true` rồi mới thất bại ở `B == false`, tránh rơi nhầm vào `!A`.

3. **Bước 3 — Viết Pre-condition & Helpers:**
   - Khai báo và khởi tạo `Game`, `Map`, `Colony`, `Player`, và các `GoodsType`/`UnitType`/`BuildingType` chuẩn từ `spec()` trong `setUp()`.
   - Đặt toàn bộ stub class và reflection helper dưới section `Pre-condition helpers & stubs`.

4. **Bước 4 — Viết Code & Kiểm chứng:**
   - Tuân thủ chính xác chữ ký hàm, constructor, kiểu trả về thực tế của UUT và các class liên quan.
   - Đảm bảo tuân thủ đúng naming & comment convention ở Mục 6.

---

## 6. Output & Code Conventions (Template chuẩn)

### 6.1. Cấu trúc báo cáo trình bày
1. **Bảng tóm tắt phân tích CFG & Test Path (`M1..Mn`)**:
   - Cột: `ID | Phương thức | Test Paths (M<x>-TP<y>: ENTRY -> ... -> EXIT) | Cạnh chưa phủ & Lý do (Infeasible)`
2. **Bảng đặc tả Test Case**:
   - Cột: `Test Case ID | Phương thức | Test Path phủ | Pre-condition & Input | Expected Result`
3. **File mã nguồn Java hoàn chỉnh** tại `test_cft/src/test/java/<package>/<TargetClass>CfgTest.java`.

### 6.2. Convention Code & Comment trong file Java

- **Class Javadoc:**
  ```java
  /**
   * CFG-driven test suite for {@link <TargetClass>} derived from
   * {@code test_cft/graph/<package_path>/<TargetClass>.json}.
   *
   * Covers all <N> methods (M1..M<N>) and achieves 100% feasible C3 (node + edge)
   * coverage across <K> Test Paths.
   *
   * Infeasible edges documented:
   * - M<x> ({@code <methodName>}): {@code N<a> -> N<b>} ({@code B<k>:<outcome>}) — <lý do>.
   */
  ```

- **SetUp:**
  ```java
  @Override
  public void setUp() throws Exception {
      super.setUp();
      // Pre-condition: <mô tả môi trường game, map, colony, spec...>
  }
  ```

- **Section dividers (bắt buộc cho helpers và từng phương thức `M1..Mn`):**
  ```java
  // ------------------------------------------------------------------
  // Pre-condition helpers & stubs
  // ------------------------------------------------------------------

  // ------------------------------------------------------------------
  // M<x>: <methodSignature> — <k> Test Path(s) (100% [feasible] C3)
  // ------------------------------------------------------------------
  ```

- **Comment & Tên hàm test (bắt buộc gắn đường đi CFG và điều kiện atomic):**
  ```java
  // M<x>-TP<y>: ENTRY -> N0 -(B1:true)-> ... -> EXIT (<mô tả điều kiện atomic>)
  public void testM<x>_TP<y>_<DescriptiveName>() { ... }
  ```
