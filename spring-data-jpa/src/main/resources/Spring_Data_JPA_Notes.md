# Spring Data JPA
> Detailed Study Notes

---

## 1. What is Spring Data JPA?

**Spring Data JPA** is part of the larger Spring Data family. It makes it easy to implement JPA-based repositories with minimal boilerplate. It sits on top of **JPA (Java Persistence API)** and uses **Hibernate** as the default JPA provider.

### Layered Architecture

```
Your Code (Service Layer)
        ↓
Spring Data JPA (Repository Abstraction)
        ↓
JPA (Java Persistence API — specification)
        ↓
Hibernate (JPA Implementation — default)
        ↓
JDBC
        ↓
Database (MySQL, PostgreSQL, H2, etc.)
```

### Key Benefits

| Feature | Description |
|---------|-------------|
| No boilerplate | No need to write SQL for basic CRUD |
| Query derivation | Method names auto-generate queries |
| Custom queries | `@Query` for complex JPQL / native SQL |
| Pagination & Sorting | Built-in support |
| Auditing | Auto track `createdAt`, `updatedAt` |
| Transaction management | Declarative with `@Transactional` |

---

## 2. Setup & Dependencies

### Maven (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database driver — choose one -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 for testing/development -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### `application.properties` (MySQL)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### `ddl-auto` Options

| Value | Behavior |
|-------|----------|
| `none` | Do nothing to schema |
| `validate` | Validate schema matches entities — error if mismatch |
| `update` | Update schema to match entities (safe for dev) |
| `create` | Drop and recreate schema on startup |
| `create-drop` | Create on startup, drop on shutdown |

> ✅ Use `validate` or `none` in production. Use `update` during development.

---

## 3. Entity — `@Entity`

An **Entity** is a Java class mapped to a database table. Every entity must have a primary key.

```java
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "age")
    private Integer age;
}
```

### Core Entity Annotations

| Annotation | Description |
|------------|-------------|
| `@Entity` | Marks the class as a JPA entity |
| `@Table(name="...")` | Maps to a specific table name |
| `@Id` | Marks the primary key field |
| `@GeneratedValue` | Auto-generate primary key value |
| `@Column` | Customize column name, length, nullable, unique |
| `@Transient` | Field is NOT mapped to a column |
| `@Enumerated` | Maps an enum to a column |
| `@Lob` | Maps to a large object (BLOB, CLOB) |
| `@Temporal` | Maps `Date`/`Calendar` types |

### `@GeneratedValue` Strategies

| Strategy | Description |
|----------|-------------|
| `IDENTITY` | DB auto-increment (MySQL, PostgreSQL) |
| `SEQUENCE` | Uses a DB sequence (PostgreSQL preferred) |
| `TABLE` | Uses a separate table to track IDs (portable) |
| `AUTO` | Hibernate picks strategy based on DB |
| `UUID` | Generates UUID (Spring Boot 3+) |

---

## 4. Repository Interfaces

Spring Data JPA provides repository interfaces you extend — it generates implementations automatically at runtime.

### Repository Hierarchy

```
Repository<T, ID>                  ← marker interface
    └── CrudRepository<T, ID>      ← basic CRUD
            └── PagingAndSortingRepository<T, ID>  ← pagination + sorting
                    └── JpaRepository<T, ID>       ← JPA-specific (most used)
```

### `JpaRepository` — Most Commonly Used

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // nothing needed — CRUD is inherited!
}
```

### Built-in Methods from `JpaRepository`

| Method | Description |
|--------|-------------|
| `save(entity)` | Insert or update |
| `saveAll(list)` | Save multiple entities |
| `findById(id)` | Returns `Optional<T>` |
| `findAll()` | Returns all records |
| `findAllById(ids)` | Find by list of IDs |
| `existsById(id)` | Returns boolean |
| `count()` | Total record count |
| `deleteById(id)` | Delete by ID |
| `delete(entity)` | Delete entity object |
| `deleteAll()` | Delete all records |
| `findAll(Sort)` | Find all with sorting |
| `findAll(Pageable)` | Find with pagination |
| `flush()` | Sync persistence context to DB |
| `saveAndFlush(entity)` | Save and flush immediately |
| `getReferenceById(id)` | Lazy proxy reference |

---

## 5. Derived Query Methods

Spring Data JPA can auto-generate queries from method names. No `@Query` needed.

### Naming Convention

```
findBy + FieldName + Condition
```

```java
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Find by single field
    List<Student> findByName(String name);
    Optional<Student> findByEmail(String email);

    // Find by multiple fields (AND)
    List<Student> findByNameAndAge(String name, Integer age);

    // Find by multiple fields (OR)
    List<Student> findByNameOrEmail(String name, String email);

    // Comparison
    List<Student> findByAgeGreaterThan(Integer age);
    List<Student> findByAgeLessThanEqual(Integer age);
    List<Student> findByAgeBetween(Integer min, Integer max);

    // Like / Contains
    List<Student> findByNameContaining(String keyword);
    List<Student> findByNameStartingWith(String prefix);
    List<Student> findByNameEndingWith(String suffix);
    List<Student> findByNameLike(String pattern);   // use % manually

    // Null checks
    List<Student> findByEmailIsNull();
    List<Student> findByEmailIsNotNull();

    // Boolean
    List<Student> findByActiveTrue();
    List<Student> findByActiveFalse();

    // In / NotIn
    List<Student> findByAgeIn(List<Integer> ages);
    List<Student> findByAgeNotIn(List<Integer> ages);

    // Ordering
    List<Student> findByAgeGreaterThanOrderByNameAsc(Integer age);
    List<Student> findByAgeGreaterThanOrderByNameDesc(Integer age);

    // Count / Exists
    long countByAge(Integer age);
    boolean existsByEmail(String email);

    // Delete
    void deleteByEmail(String email);

    // Limit
    Student findFirstByOrderByAgeAsc();
    List<Student> findTop3ByOrderByAgeDesc();
}
```

### Keyword Reference

| Keyword | Example Method | JPQL Equivalent |
|---------|---------------|-----------------|
| `And` | `findByNameAndAge` | `WHERE name=? AND age=?` |
| `Or` | `findByNameOrEmail` | `WHERE name=? OR email=?` |
| `Between` | `findByAgeBetween` | `WHERE age BETWEEN ? AND ?` |
| `LessThan` | `findByAgeLessThan` | `WHERE age < ?` |
| `GreaterThan` | `findByAgeGreaterThan` | `WHERE age > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE %?%` |
| `StartingWith` | `findByNameStartingWith` | `WHERE name LIKE ?%` |
| `EndingWith` | `findByNameEndingWith` | `WHERE name LIKE %?` |
| `IsNull` | `findByEmailIsNull` | `WHERE email IS NULL` |
| `In` | `findByAgeIn` | `WHERE age IN (...)` |
| `True` / `False` | `findByActiveTrue` | `WHERE active = true` |
| `OrderBy` | `findByAgeOrderByNameAsc` | `ORDER BY name ASC` |
| `First` / `Top` | `findFirst3By...` | `LIMIT 3` |
| `Distinct` | `findDistinctByName` | `SELECT DISTINCT ...` |

---

## 6. `@Query` — Custom Queries

Use `@Query` when derived method names get too long or complex.

### JPQL (Entity-based)

JPQL uses **entity class names and field names**, not table/column names.

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Basic JPQL
@Query("SELECT s FROM Student s WHERE s.age > :age")
List<Student> findStudentsOlderThan(@Param("age") Integer age);

// Multiple conditions
@Query("SELECT s FROM Student s WHERE s.name = :name AND s.age >= :age")
List<Student> findByNameAndMinAge(@Param("name") String name, @Param("age") int age);

// Using positional parameters
@Query("SELECT s FROM Student s WHERE s.email = ?1")
Optional<Student> findByEmailQuery(String email);

// Order by
@Query("SELECT s FROM Student s ORDER BY s.name ASC")
List<Student> findAllSortedByName();

// Partial field projection
@Query("SELECT s.name, s.email FROM Student s WHERE s.age > :age")
List<Object[]> findNameAndEmail(@Param("age") int age);
```

### Native SQL Query

```java
@Query(value = "SELECT * FROM students WHERE age > :age", nativeQuery = true)
List<Student> findStudentsNative(@Param("age") int age);

@Query(value = "SELECT * FROM students WHERE name LIKE %:keyword%", nativeQuery = true)
List<Student> searchByName(@Param("keyword") String keyword);
```

### Modifying Queries (`@Modifying`)

For `UPDATE` and `DELETE` queries, add `@Modifying` and `@Transactional`.

```java
@Modifying
@Transactional
@Query("UPDATE Student s SET s.age = :age WHERE s.id = :id")
int updateAge(@Param("id") Long id, @Param("age") int age);

@Modifying
@Transactional
@Query("DELETE FROM Student s WHERE s.age < :age")
int deleteUnderAge(@Param("age") int age);
```

---

## 7. Pagination & Sorting

### Sorting

```java
import org.springframework.data.domain.Sort;

// Single field sort
List<Student> students = studentRepository.findAll(Sort.by("name"));

// Direction
List<Student> students = studentRepository.findAll(Sort.by(Sort.Direction.DESC, "age"));

// Multiple fields
List<Student> students = studentRepository.findAll(
    Sort.by("name").ascending().and(Sort.by("age").descending())
);
```

### Pagination

```java
import org.springframework.data.domain.*;

// Create a pageable request
Pageable pageable = PageRequest.of(0, 10);             // page 0, size 10
Pageable pageable = PageRequest.of(1, 5, Sort.by("name"));  // page 1, size 5, sorted

// Get a page
Page<Student> page = studentRepository.findAll(pageable);

// Useful Page methods
page.getContent();          // List<Student> for this page
page.getTotalElements();    // total records in DB
page.getTotalPages();       // total pages
page.getNumber();           // current page number (0-based)
page.getSize();             // page size
page.isFirst();             // is this the first page?
page.isLast();              // is this the last page?
page.hasNext();             // is there a next page?
```

### Paginated Query Methods

```java
// In repository
Page<Student> findByAgeGreaterThan(Integer age, Pageable pageable);
Slice<Student> findByName(String name, Pageable pageable);  // lighter than Page
```

> **`Page` vs `Slice`**: `Page` runs a `COUNT` query for total elements. `Slice` just checks if a next page exists — faster when total count is not needed.

---

## 8. Entity Relationships

### `@OneToOne`

```java
// Parent entity
@Entity
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Profile profile;
}

// Child entity (owns the FK column)
@Entity
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bio;

    @OneToOne
    @JoinColumn(name = "user_id")   // FK column in profile table
    private User user;
}
```

### `@OneToMany` / `@ManyToOne`

```java
// One Department → Many Employees
@Entity
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Employee> employees = new ArrayList<>();
}

@Entity
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id")   // FK in employee table
    private Department department;
}
```

> ⚠️ Always initialize collection fields (`= new ArrayList<>()`) to avoid `NullPointerException`.

### `@ManyToMany`

```java
@Entity
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses = new ArrayList<>();
}

@Entity
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @ManyToMany(mappedBy = "courses")
    private List<Student> students = new ArrayList<>();
}
```

### Cascade Types

| CascadeType | Description |
|-------------|-------------|
| `ALL` | Apply all cascades |
| `PERSIST` | Save child when parent is saved |
| `MERGE` | Update child when parent is merged |
| `REMOVE` | Delete child when parent is deleted |
| `REFRESH` | Refresh child when parent is refreshed |
| `DETACH` | Detach child when parent is detached |

### Fetch Types

| FetchType | Description | Default for |
|-----------|-------------|-------------|
| `EAGER` | Load related entity immediately | `@OneToOne`, `@ManyToOne` |
| `LAZY` | Load related entity only when accessed | `@OneToMany`, `@ManyToMany` |

```java
@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
```

> ✅ Prefer `LAZY` to avoid loading unnecessary data. Use `EAGER` only when you always need the related data.

---

## 9. Bidirectional Relationship — Common Pitfalls

### Problem 1: Infinite Recursion (JSON Serialization)

**Fix** — use `@JsonManagedReference` / `@JsonBackReference` or `@JsonIgnore`:

```java
// Parent side
@JsonManagedReference
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
private List<Employee> employees;

// Child side
@JsonBackReference
@ManyToOne
@JoinColumn(name = "department_id")
private Department department;
```

### Problem 2: Null Foreign Key (Unsaved relationship state)

In bidirectional `@OneToMany`/`@ManyToOne`, always **set both sides**:

```java
// ❌ Wrong — only sets one side
department.getEmployees().add(employee);

// ✅ Correct — set both sides
employee.setDepartment(department);
department.getEmployees().add(employee);
```

### Problem 3: `@Data` + Bidirectional → StackOverflowError

Lombok's `@Data` generates `toString()` and `hashCode()` using all fields, causing infinite loops in bidirectional relationships.

**Fix:**

```java
// Use @ToString.Exclude and @EqualsAndHashCode.Exclude on the collection side
@ToString.Exclude
@EqualsAndHashCode.Exclude
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

---

## 10. `@Transactional`

Ensures that a group of operations either all succeed or all fail (atomicity).

```java
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    @Transactional
    public void transferStudent(Long fromDeptId, Long toDeptId, Long studentId) {
        // Both operations happen in ONE transaction
        // If second fails, first is also rolled back
        departmentRepo.removeStudent(fromDeptId, studentId);
        departmentRepo.addStudent(toDeptId, studentId);
    }
}
```

### `@Transactional` Attributes

| Attribute | Description | Default |
|-----------|-------------|---------|
| `readOnly` | Hint for optimization (no writes) | `false` |
| `rollbackFor` | Which exceptions trigger rollback | Runtime exceptions |
| `noRollbackFor` | Which exceptions do NOT trigger rollback | — |
| `propagation` | How transaction interacts with existing one | `REQUIRED` |
| `isolation` | Transaction isolation level | DB default |
| `timeout` | Seconds before transaction times out | `-1` (no limit) |

### Propagation Types

| Propagation | Description |
|-------------|-------------|
| `REQUIRED` | Join existing, or create new (default) |
| `REQUIRES_NEW` | Always create a new transaction |
| `NESTED` | Nested transaction (savepoint) |
| `SUPPORTS` | Use existing if available, else non-transactional |
| `NOT_SUPPORTED` | Suspend current, run non-transactionally |
| `NEVER` | Throw exception if transaction exists |
| `MANDATORY` | Must have existing transaction, else throw |

---

## 11. Auditing

Automatically populate `createdAt`, `updatedAt`, `createdBy`, `modifiedBy` fields.

### Step 1 — Enable Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig { }
```

### Step 2 — Use Auditing Annotations

```java
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Student {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String modifiedBy;
}
```

### Step 3 — Provide `AuditorAware` (for `@CreatedBy`)

```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("system");   // or get from SecurityContext
}
```

---

## 12. Projections

Fetch only specific fields instead of the full entity — improves performance.

### Interface Projection

```java
public interface StudentSummary {
    String getName();
    String getEmail();
}

// In repository
List<StudentSummary> findByAgeGreaterThan(int age);
```

### Class-based Projection (DTO)

```java
public class StudentDTO {
    private String name;
    private String email;

    public StudentDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
}

// In repository using @Query
@Query("SELECT new com.example.dto.StudentDTO(s.name, s.email) FROM Student s WHERE s.age > :age")
List<StudentDTO> findDtoByAge(@Param("age") int age);
```

---

## 13. Named Queries

Define queries at the entity level using `@NamedQuery`.

```java
@Entity
@NamedQuery(
    name = "Student.findByCity",
    query = "SELECT s FROM Student s WHERE s.city = :city"
)
public class Student { ... }

// In repository
List<Student> findByCity(@Param("city") String city);
```

---

## 14. Specifications (Dynamic Queries)

`Specification<T>` allows building dynamic queries programmatically — useful for search filters.

### Enable in Repository

```java
public interface StudentRepository
    extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> { }
```

### Build Specifications

```java
import org.springframework.data.jpa.domain.Specification;

public class StudentSpec {

    public static Specification<Student> hasName(String name) {
        return (root, query, cb) ->
            name == null ? null : cb.equal(root.get("name"), name);
    }

    public static Specification<Student> olderThan(Integer age) {
        return (root, query, cb) ->
            age == null ? null : cb.greaterThan(root.get("age"), age);
    }
}
```

### Use Specifications

```java
Specification<Student> spec = Specification
    .where(StudentSpec.hasName("Ray"))
    .and(StudentSpec.olderThan(18));

List<Student> result = studentRepository.findAll(spec);
```

---

## 15. Common Patterns in Service Layer

```java
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    // Create / Update
    public Student save(Student student) {
        return studentRepository.save(student);
    }

    // Find by ID (throw if not found)
    public Student findById(Long id) {
        return studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found: " + id));
    }

    // Find all with pagination
    public Page<Student> findAll(int page, int size) {
        return studentRepository.findAll(PageRequest.of(page, size));
    }

    // Update
    @Transactional
    public Student update(Long id, Student updated) {
        Student existing = findById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        return studentRepository.save(existing);
    }

    // Delete
    public void delete(Long id) {
        studentRepository.deleteById(id);
    }
}
```

---

## 16. N+1 Problem & How to Fix It

### The Problem

When fetching a list of entities with `LAZY` relationships, Hibernate fires one extra query per entity to load the related data.

```java
// 1 query for all departments
List<Department> departments = departmentRepo.findAll();

// N queries — one per department to load employees!
departments.forEach(d -> d.getEmployees().size());
```

### Fix 1: `JOIN FETCH` in JPQL

```java
@Query("SELECT d FROM Department d JOIN FETCH d.employees")
List<Department> findAllWithEmployees();
```

### Fix 2: `@EntityGraph`

```java
@EntityGraph(attributePaths = {"employees"})
@Query("SELECT d FROM Department d")
List<Department> findAllWithEmployees();
```

### Fix 3: `@BatchSize`

```java
@BatchSize(size = 10)
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

---

## 17. Best Practices

- Use `Optional<T>` return type from `findById` — never return `null`
- Always put `@Transactional` on the **service layer**, not the repository
- Prefer `LAZY` fetch — only use `EAGER` when the related data is always needed
- Use DTOs / Projections to avoid fetching full entities for read-only operations
- Avoid `CascadeType.REMOVE` on `@ManyToMany` — it deletes the other entity
- Use `@Version` for optimistic locking in concurrent update scenarios
- Always set both sides of a bidirectional relationship
- Use `saveAndFlush()` when you need immediate DB sync in the same transaction
- Avoid `findAll()` without pagination on large tables
- Use `@Modifying(clearAutomatically = true)` to clear persistence context after bulk updates

---

## 18. Quick Reference Cheat Sheet

| Task | Code |
|------|------|
| Basic repository | `extends JpaRepository<Entity, Long>` |
| Save | `repo.save(entity)` |
| Find by ID | `repo.findById(id).orElseThrow(...)` |
| Find all | `repo.findAll()` |
| Delete | `repo.deleteById(id)` |
| Count | `repo.count()` |
| Exists | `repo.existsById(id)` |
| Sort | `repo.findAll(Sort.by("field").ascending())` |
| Paginate | `repo.findAll(PageRequest.of(page, size))` |
| Derived query | `findByNameAndAgeGreaterThan(name, age)` |
| Custom JPQL | `@Query("SELECT e FROM Entity e WHERE ...")` |
| Native SQL | `@Query(value="SELECT...", nativeQuery=true)` |
| Bulk update | `@Modifying @Transactional @Query("UPDATE...")` |
| Projection | Interface with `getField()` methods |
| DTO projection | `SELECT new pkg.Dto(e.field) FROM Entity e` |
| Auditing | `@CreatedDate`, `@LastModifiedDate` + `@EnableJpaAuditing` |
| N+1 fix | `JOIN FETCH` or `@EntityGraph` |
| Dynamic query | `Specification<T>` + `JpaSpecificationExecutor` |
| Transaction | `@Transactional` on service method |
| Optimistic lock | `@Version` field on entity |

---

*— End of Notes —*
