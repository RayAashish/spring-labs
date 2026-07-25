# Spring Data JPA — Entity Mapping
> Detailed Notes with Examples

---

## 1. What is Mapping?

**Mapping** is the process of linking a Java class (Entity) to a database table, and linking Java fields to database columns. When you have **relationships between tables** (foreign keys, join tables), you map them using relationship annotations.

### The Big Picture

```
Java World                          Database World
──────────────────────────────────────────────────
@Entity class          ──────→     Table
Field                  ──────→     Column
@Id field              ──────→     Primary Key
@OneToOne              ──────→     FK in one table
@OneToMany / ManyToOne ──────→     FK in child table
@ManyToMany            ──────→     Junction / Join Table
```

---

## 2. Types of Relationships

| Relationship | Real-World Example | DB Representation |
|--------------|-------------------|-------------------|
| `@OneToOne` | Person ↔ Passport | FK in one table |
| `@OneToMany` | Department → Employees | FK in child table |
| `@ManyToOne` | Employee → Department | FK in child table |
| `@ManyToMany` | Students ↔ Courses | Separate join table |

---

## 3. The Two Sides of a Relationship

Every relationship has two sides:

| Side | Name | Annotation Clue |
|------|------|-----------------|
| **Owning side** | Controls the FK column in DB | Has `@JoinColumn` |
| **Inverse side** | References the owning side | Has `mappedBy = "..."` |

> ✅ **Rule of thumb** — the side with `@JoinColumn` owns the FK.
> The `mappedBy` side just mirrors the relationship — it does NOT create any extra column.

---

## 4. Core Mapping Annotations

### Relationship Annotations

| Annotation | Usage |
|------------|-------|
| `@OneToOne` | One entity relates to exactly one other entity |
| `@OneToMany` | One entity relates to many entities |
| `@ManyToOne` | Many entities relate to one entity |
| `@ManyToMany` | Many entities relate to many entities |

### Join Annotations

| Annotation | Usage |
|------------|-------|
| `@JoinColumn` | Specifies the FK column name in the current table |
| `@JoinTable` | Specifies the join/junction table for `@ManyToMany` |
| `mappedBy` | Attribute (not annotation) that marks the inverse side |

### Behavior Annotations / Attributes

| Annotation / Attribute | Usage |
|------------------------|-------|
| `cascade` | Propagate operations (save, delete) to related entities |
| `fetch` | `LAZY` (load on demand) or `EAGER` (load immediately) |
| `orphanRemoval` | Auto-delete child when removed from parent's collection |
| `optional` | Whether the relationship is nullable (`@OneToOne`, `@ManyToOne`) |

### Serialization Annotations (avoid infinite loops)

| Annotation | Side | Effect |
|------------|------|--------|
| `@JsonManagedReference` | Parent / owning side | Serialized normally |
| `@JsonBackReference` | Child / inverse side | NOT serialized (breaks loop) |
| `@JsonIgnore` | Either side | Field is completely ignored in JSON |

### Lombok Safety

| Annotation | Usage |
|------------|-------|
| `@ToString.Exclude` | Exclude field from `toString()` — prevents StackOverflow |
| `@EqualsAndHashCode.Exclude` | Exclude field from `equals/hashCode` — prevents StackOverflow |

---

## 5. `@OneToOne` Mapping

One row in Table A relates to exactly one row in Table B.

**Real-world example: User ↔ Profile**
- Each user has exactly one profile
- Each profile belongs to exactly one user

### 5.1 Unidirectional `@OneToOne`

Only one side knows about the relationship.

```java
// User.java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    // ↑ FK column "profile_id" is added to the USERS table
    private Profile profile;
}
```

```java
// Profile.java
@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bio;
    private String website;
    // Profile has NO idea about User — unidirectional
}
```

**DB Tables:**
```
users    → id, username, profile_id (FK → profiles.id)
profiles → id, bio, website
```

---

### 5.2 Bidirectional `@OneToOne` ✅ (Most Common)

Both sides know about each other.

```java
// User.java — OWNING SIDE (has @JoinColumn)
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    // ↑ "profile_id" FK column lives in the USERS table
    @JsonManagedReference
    @ToString.Exclude
    private Profile profile;
}
```

```java
// Profile.java — INVERSE SIDE (has mappedBy)
@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bio;
    private String website;

    @OneToOne(mappedBy = "profile")
    // ↑ "profile" = field name in User.java — no FK here!
    @JsonBackReference
    @ToString.Exclude
    private User user;
}
```

**DB Tables:**
```
users    → id, username, email, profile_id (FK → profiles.id)
profiles → id, bio, website
```

### 5.3 Usage in Service

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUserWithProfile() {
        Profile profile = new Profile();
        profile.setBio("Java Developer");
        profile.setWebsite("https://raydev.com");

        User user = new User();
        user.setUsername("ray");
        user.setEmail("ray@example.com");
        user.setProfile(profile);   // set the relationship

        return userRepository.save(user);
        // CascadeType.ALL → saves Profile automatically!
    }
}
```

### `@OneToOne` — When to Use What

| Situation | What to do |
|-----------|-----------|
| Only one side needs to access the other | Unidirectional |
| Both sides need to access each other | Bidirectional |
| Deleting User should delete Profile | Add `cascade = CascadeType.ALL` |
| Profile can exist without User | Remove `cascade = CascadeType.REMOVE` |
| FK lives in User table | Put `@JoinColumn` in User |
| FK lives in Profile table | Put `@JoinColumn` in Profile |

---

## 6. `@OneToMany` / `@ManyToOne` Mapping

One entity relates to a collection of another entity.

**Real-world example: Department ↔ Employees**
- One department has many employees
- Each employee belongs to exactly one department
- FK (`department_id`) lives in the **Employee** table (the "many" side)

> ✅ **Always pair `@OneToMany` with `@ManyToOne`** for bidirectional relationships.
> The FK always lives on the `@ManyToOne` side (the "many" side / child table).

---

### 6.1 The Entities

```java
// Department.java — "One" side (parent)
@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    @OneToMany(
        mappedBy = "department",        // refers to field name in Employee.java
        cascade = CascadeType.ALL,      // save/delete employees with department
        orphanRemoval = true,           // delete employee if removed from list
        fetch = FetchType.LAZY          // don't load employees unless accessed
    )
    @JsonManagedReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Employee> employees = new ArrayList<>();

    // ── Helper method to keep BOTH sides in sync ──
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);   // set the FK side too!
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }
}
```

```java
// Employee.java — "Many" side (child) — OWNS the FK
@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "department_id",         // FK column in EMPLOYEES table
        nullable = false
    )
    @JsonBackReference
    @ToString.Exclude
    private Department department;
}
```

**DB Tables:**
```
departments → id, name, location
employees   → id, name, role, department_id (FK → departments.id)
```

---

### 6.2 Repository & Service

```java
// Repositories
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
}

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByRole(String role);
    List<Employee> findByDepartmentName(String departmentName);
}
```

```java
// DepartmentService.java
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // Create department with employees
    public Department createWithEmployees() {
        Department dept = new Department();
        dept.setName("Engineering");
        dept.setLocation("Kathmandu");

        Employee e1 = new Employee();
        e1.setName("Ray");
        e1.setRole("Backend Dev");

        Employee e2 = new Employee();
        e2.setName("Alice");
        e2.setRole("Frontend Dev");

        // Use helper method — sets BOTH sides
        dept.addEmployee(e1);
        dept.addEmployee(e2);

        return departmentRepository.save(dept);
        // CascadeType.ALL → saves employees automatically!
    }

    // Add employee to existing department
    @Transactional
    public Department addEmployee(Long deptId, Employee employee) {
        Department dept = departmentRepository.findById(deptId)
            .orElseThrow(() -> new RuntimeException("Department not found"));

        dept.addEmployee(employee);   // sets both sides
        return departmentRepository.save(dept);
    }

    // Remove employee from department
    @Transactional
    public void removeEmployee(Long deptId, Long employeeId) {
        Department dept = departmentRepository.findById(deptId)
            .orElseThrow(() -> new RuntimeException("Department not found"));

        dept.getEmployees().removeIf(e -> e.getId().equals(employeeId));
        // orphanRemoval = true → deleted from DB automatically!
    }
}
```

### `@OneToMany`/`@ManyToOne` — When to Use What

| Situation | What to do |
|-----------|-----------|
| Need to access employees from department | Add `@OneToMany` in Department |
| Need to access department from employee | Add `@ManyToOne` in Employee |
| Deleting department should delete employees | `cascade = CascadeType.ALL` + `orphanRemoval = true` |
| Removing from list should delete from DB | `orphanRemoval = true` |
| Don't load employees unless needed | `fetch = FetchType.LAZY` (default for `@OneToMany`) |
| FK is `department_id` in employees table | `@JoinColumn(name = "department_id")` in Employee |

---

## 7. `@ManyToMany` Mapping

Many rows in Table A relate to many rows in Table B — via a **junction/join table**.

**Real-world example: Students ↔ Courses**
- One student can enroll in many courses
- One course can have many students
- A separate join table `student_course` holds both FKs

---

### 7.1 The Entities

```java
// Student.java — OWNING SIDE (defines the @JoinTable)
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        // ↑ Do NOT use CascadeType.ALL or REMOVE in @ManyToMany
        //   It would delete the Course when a Student is deleted!
        fetch = FetchType.LAZY
    )
    @JoinTable(
        name = "student_course",                        // join table name
        joinColumns = @JoinColumn(name = "student_id"), // FK for THIS entity
        inverseJoinColumns = @JoinColumn(name = "course_id") // FK for OTHER entity
    )
    @JsonManagedReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Course> courses = new ArrayList<>();

    // ── Helper method ──
    public void enrollIn(Course course) {
        this.courses.add(course);
        course.getStudents().add(this);  // set both sides!
    }

    public void dropCourse(Course course) {
        this.courses.remove(course);
        course.getStudents().remove(this);
    }
}
```

```java
// Course.java — INVERSE SIDE (has mappedBy)
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;
    private Integer credits;

    @ManyToMany(mappedBy = "courses")
    // ↑ "courses" = field name in Student.java
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Student> students = new ArrayList<>();
}
```

**DB Tables:**
```
students       → id, name, email
courses        → id, title, description, credits
student_course → student_id (FK → students.id), course_id (FK → courses.id)
```

---

### 7.2 Repository & Service

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByCoursesTitle(String courseTitle);   // derived query through join
    Optional<Student> findByEmail(String email);
}

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStudentsName(String studentName);
    Optional<Course> findByTitle(String title);
}
```

```java
// StudentService.java
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    // Enroll student in a course
    @Transactional
    public Student enrollStudentInCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        student.enrollIn(course);   // helper — sets BOTH sides
        return studentRepository.save(student);
    }

    // Remove student from a course
    @Transactional
    public Student dropCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        student.dropCourse(course);   // removes from BOTH sides
        return studentRepository.save(student);
    }

    // Create student with courses at once
    @Transactional
    public Student createWithCourses(String name, String email, List<Long> courseIds) {
        Student student = new Student();
        student.setName(name);
        student.setEmail(email);

        List<Course> courses = courseRepository.findAllById(courseIds);
        courses.forEach(student::enrollIn);

        return studentRepository.save(student);
    }
}
```

### `@ManyToMany` — When to Use What

| Situation | What to do |
|-----------|-----------|
| Define the join table | Put `@JoinTable` on the owning side |
| Mirror the relationship | Use `mappedBy` on the inverse side |
| Cascade saves but not deletes | `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` |
| **Never** cascade removes | Avoid `CascadeType.ALL` and `CascadeType.REMOVE` |
| Need extra columns in join table | Create an explicit join entity (see Section 8) |

---

## 8. `@ManyToMany` with Extra Columns — Join Entity

When the join table needs **extra columns** (like `enrolledDate`, `grade`), you can't use plain `@ManyToMany`. Instead, model the join table as its own `@Entity`.

```java
// Enrollment.java — explicit join entity
@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // Extra columns in the join table
    private LocalDate enrolledDate;
    private String grade;
}
```

```java
// Student.java — using Enrollment instead of direct @ManyToMany
@Entity
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}

// Course.java
@Entity
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Enrollment> enrollments = new ArrayList<>();
}
```

**DB Tables:**
```
students    → id, name
courses     → id, title
enrollments → id, student_id, course_id, enrolled_date, grade
```

---

## 9. `cascade` — What It Means

Cascade controls **which operations on the parent automatically propagate to the child**.

```java
@OneToMany(cascade = CascadeType.ALL)
```

| CascadeType | What it does |
|-------------|-------------|
| `PERSIST` | Saving parent also saves new children |
| `MERGE` | Updating parent also updates children |
| `REMOVE` | Deleting parent also deletes children |
| `REFRESH` | Refreshing parent also refreshes children |
| `DETACH` | Detaching parent also detaches children |
| `ALL` | All of the above |

### When to Use Each

| Relationship | Recommended Cascade |
|--------------|-------------------|
| `@OneToOne` (owned entity) | `CascadeType.ALL` |
| `@OneToMany` (dependent children) | `CascadeType.ALL` + `orphanRemoval = true` |
| `@ManyToMany` | `{CascadeType.PERSIST, CascadeType.MERGE}` only |
| `@ManyToOne` | Usually none — child doesn't cascade to parent |

> ⚠️ **Never use `CascadeType.REMOVE` on `@ManyToMany`** — deleting a Student would delete the Courses, which are shared with other students!

---

## 10. `fetch` — LAZY vs EAGER

Controls **when** related data is loaded from the database.

```java
@OneToMany(fetch = FetchType.LAZY)   // load employees only when accessed
@ManyToOne(fetch = FetchType.EAGER)  // load department immediately
```

| FetchType | When loaded | Default for |
|-----------|-------------|-------------|
| `LAZY` | Only when the field is accessed | `@OneToMany`, `@ManyToMany` |
| `EAGER` | Immediately when parent is loaded | `@OneToOne`, `@ManyToOne` |

### Example

```java
Department dept = departmentRepo.findById(1L).get();
// ↑ Only loads Department row — employees NOT loaded yet (LAZY)

dept.getEmployees();
// ↑ NOW employees are fetched from DB (LAZY trigger)
```

> ✅ **Always use `LAZY` unless you always need the related data.**
> Using `EAGER` everywhere causes slow queries and the N+1 problem.

---

## 11. `orphanRemoval` vs `CascadeType.REMOVE`

Both remove child records from DB, but they behave differently.

| | `orphanRemoval = true` | `CascadeType.REMOVE` |
|---|------------------------|---------------------|
| Trigger | Child removed from parent's collection | Parent entity is deleted |
| Use case | Manage children via parent's list | Delete all children when parent deleted |

```java
// With orphanRemoval = true
dept.getEmployees().remove(employee);
departmentRepo.save(dept);
// ↑ Employee is deleted from DB — it became an "orphan"

// With CascadeType.REMOVE
departmentRepo.delete(dept);
// ↑ Deletes dept AND all its employees
```

> ✅ Use `orphanRemoval = true` when children have **no meaning without the parent** (e.g., order items without an order).

---

## 12. `mappedBy` — The Most Confusing Part

`mappedBy` tells JPA: *"I am the inverse side. The other entity controls the FK. Look at its field named X."*

```java
// In Department:
@OneToMany(mappedBy = "department")
//          ↑ This is the FIELD NAME in Employee.java
private List<Employee> employees;

// In Employee:
@ManyToOne
@JoinColumn(name = "department_id")
private Department department;  // ← THIS is what mappedBy points to
```

### `mappedBy` Rules

| Rule | Description |
|------|-------------|
| Only on inverse side | Never on the owning side (`@JoinColumn` side) |
| Value = field name | Must match exactly the field name in the other entity |
| No extra column | The inverse side does NOT create any extra column |
| Read-only sync | Changes on inverse side alone won't persist — set the owning side |

---

## 13. Full Cheat Sheet — What Goes Where

### `@OneToOne`

```
Entity A  ←──────────→  Entity B
  @OneToOne                @OneToOne
  @JoinColumn              mappedBy = "fieldInA"
  (FK in A's table)        (no extra column)
```

### `@OneToMany` / `@ManyToOne`

```
Department (One)           Employee (Many)
  @OneToMany                 @ManyToOne
  mappedBy = "department"    @JoinColumn(name = "department_id")
  (no FK here)               (FK is here — in employee table)
```

### `@ManyToMany`

```
Student (owning)           Course (inverse)
  @ManyToMany                @ManyToMany
  @JoinTable(...)            mappedBy = "courses"
  (defines join table)       (no join table definition)
```

---

## 14. Common Mistakes & Fixes

### ❌ Mistake 1 — Forgetting to set both sides

```java
// ❌ Wrong — only sets one side, FK stays null!
department.getEmployees().add(employee);
departmentRepo.save(department);

// ✅ Fix — set BOTH sides
employee.setDepartment(department);       // sets the FK (owning side)
department.getEmployees().add(employee);  // syncs the list (inverse side)
departmentRepo.save(department);
```

### ❌ Mistake 2 — StackOverflowError with `@Data`

```java
// ❌ Wrong — @Data generates toString() using all fields → infinite loop
@Data
@Entity
public class Department {
    @OneToMany(mappedBy = "department")
    private List<Employee> employees;  // Employee.toString() calls Department.toString() → loop!
}

// ✅ Fix — exclude bidirectional fields
@ToString.Exclude
@EqualsAndHashCode.Exclude
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

### ❌ Mistake 3 — Infinite JSON recursion

```java
// ❌ Wrong — serializing Department serializes Employees, which serialize Department again
@OneToMany private List<Employee> employees;  // no annotation
@ManyToOne private Department department;     // no annotation

// ✅ Fix — use @JsonManagedReference and @JsonBackReference
@JsonManagedReference
@OneToMany private List<Employee> employees;  // serialized

@JsonBackReference
@ManyToOne private Department department;     // NOT serialized (breaks loop)
```

### ❌ Mistake 4 — `CascadeType.ALL` on `@ManyToMany`

```java
// ❌ Wrong — deleting a student deletes the courses too!
@ManyToMany(cascade = CascadeType.ALL)
private List<Course> courses;

// ✅ Fix — only cascade PERSIST and MERGE
@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
private List<Course> courses;
```

### ❌ Mistake 5 — Uninitialized collection

```java
// ❌ Wrong — NullPointerException when adding to list
private List<Employee> employees;

// ✅ Fix — always initialize
private List<Employee> employees = new ArrayList<>();
```

### ❌ Mistake 6 — Wrong `mappedBy` value

```java
// ❌ Wrong — field name doesn't match
@OneToMany(mappedBy = "dept")        // field in Employee is "department", not "dept"
private List<Employee> employees;

// ✅ Fix — must match exactly
@OneToMany(mappedBy = "department")  // matches Employee's field name
private List<Employee> employees;
```

---

## 15. Quick Decision Guide

```
Do you have a relationship between two entities?
            │
            ▼
    How many on each side?
     ┌──────┴───────┐
  One-One         One-Many / Many-Many
     │                    │
@OneToOne          One-Many → @OneToMany + @ManyToOne
                   Many-Many → @ManyToMany (or join entity)
            │
            ▼
    Do both sides need to access each other?
     ┌──────┴───────┐
    Yes             No
Bidirectional    Unidirectional (simpler)
     │
     ▼
Who holds the FK column?
→ That side gets @JoinColumn  (owning side)
→ Other side gets mappedBy=   (inverse side)
            │
            ▼
    Should deleting parent delete children?
     ┌──────┴───────┐
    Yes             No
cascade=ALL       No cascade or
orphanRemoval     PERSIST+MERGE only
```

---

## 16. Summary Reference Table

| Annotation | Where | What it creates in DB |
|------------|-------|-----------------------|
| `@OneToOne` + `@JoinColumn` | Owning entity | FK column in this table |
| `@OneToOne` + `mappedBy` | Inverse entity | Nothing |
| `@ManyToOne` + `@JoinColumn` | Child entity | FK column in child table |
| `@OneToMany` + `mappedBy` | Parent entity | Nothing |
| `@ManyToMany` + `@JoinTable` | Owning entity | Creates join table |
| `@ManyToMany` + `mappedBy` | Inverse entity | Nothing |

---

*— End of Notes —*
