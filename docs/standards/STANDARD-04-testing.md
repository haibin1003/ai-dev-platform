# 测试规范

**版本**: v1.0  
**适用范围**: 所有测试代码

---

## 1. 测试金字塔

```
        /\
       /  \     E2E 测试（5%）
      /____\         - 完整的用户场景
     /      \        - 慢，成本高
    /________\
   /          \   集成测试（15%）
  /            \      - 组件间交互
 /              \     - 数据库/外部服务
/________________\
/                  \ 单元测试（80%）
/                    \  - 快速，独立
/                      \ - 业务逻辑覆盖
```

---

## 2. 单元测试规范

### 2.1 测试框架

```xml
<!-- 依赖 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

### 2.2 测试类结构

```java
@DisplayName("工作流引擎测试")
class WorkflowEngineTest {
    
    // 被测对象
    private WorkflowEngine engine;
    
    // 依赖（Mock）
    private WorkflowRepository repository;
    private TaskExecutor taskExecutor;
    
    @BeforeEach
    void setUp() {
        repository = mock(WorkflowRepository.class);
        taskExecutor = mock(TaskExecutor.class);
        engine = new WorkflowEngine(repository, taskExecutor);
    }
    
    @Nested
    @DisplayName("拓扑排序")
    class TopologicalSortTests {
        
        @Test
        @DisplayName("应该正确排序简单依赖")
        void shouldSortSimpleDependencies() {
            // Given - 准备数据
            DAG dag = createSimpleDAG();
            
            // When - 执行操作
            List<Node> result = engine.topologicalSort(dag);
            
            // Then - 验证结果
            assertThat(result)
                .extracting(Node::getId)
                .containsExactly("A", "B", "C");
        }
        
        @Test
        @DisplayName("当存在循环依赖时应抛出异常")
        void shouldThrowExceptionWhenCycleExists() {
            // Given
            DAG cyclicDag = createCyclicDAG();
            
            // When & Then
            assertThatThrownBy(() -> engine.topologicalSort(cyclicDag))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Cycle detected");
        }
    }
    
    @Nested
    @DisplayName("工作流执行")
    class WorkflowExecutionTests {
        
        @Test
        @DisplayName("应该成功启动工作流")
        void shouldStartWorkflowSuccessfully() {
            // Given
            String workflowId = "wf-001";
            Map<String, String> inputs = Map.of("key", "value");
            Workflow workflow = createWorkflow();
            
            when(repository.findById(workflowId))
                .thenReturn(Optional.of(workflow));
            
            // When
            WorkflowExecution execution = engine.startWorkflow(workflowId, inputs);
            
            // Then
            assertThat(execution).isNotNull();
            assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
            verify(repository).save(any(WorkflowExecution.class));
        }
        
        @Test
        @DisplayName("当工作流不存在时应抛出异常")
        void shouldThrowExceptionWhenWorkflowNotFound() {
            // Given
            String workflowId = "non-existent";
            when(repository.findById(workflowId))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> engine.startWorkflow(workflowId, Map.of()))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessageContaining(workflowId);
        }
    }
}
```

### 2.3 测试命名规范

```java
// ✅ 正确：清晰描述行为和条件
shouldSortSimpleDependencies
shouldThrowExceptionWhenCycleExists
shouldReturnEmptyListWhenNoTasksFound
shouldUpdateStatusToRunning

// ❌ 错误：含义不清
test1
testTopologicalSort
testExecute
testWorkflow()
```

### 2.4 Given-When-Then 模式

```java
@Test
@DisplayName("完成任务后应该触发下游任务")
void shouldTriggerDownstreamTasksAfterCompletion() {
    // Given: 设置初始状态
    Task task = createTask(TaskStatus.RUNNING);
    Task downstreamTask = createTask(TaskStatus.PENDING);
    downstreamTask.addDependency(task.getId());
    
    when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
    when(taskRepository.findByExecutionId(any())).thenReturn(List.of(downstreamTask));
    
    // When: 执行操作
    taskService.completeTask(task.getId(), createSuccessResult());
    
    // Then: 验证结果
    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(downstreamTask.getStatus()).isEqualTo(TaskStatus.READY);
    verify(taskRepository).save(downstreamTask);
}
```

### 2.5 AssertJ 最佳实践

```java
// ✅ 正确：使用 AssertJ 的流式断言
assertThat(result)
    .isNotNull()
    .hasSize(3)
    .extracting(Node::getId)
    .containsExactly("A", "B", "C")
    .doesNotContain("D");

assertThat(task)
    .extracting(Task::getStatus, Task::getProgress)
    .containsExactly(TaskStatus.COMPLETED, 100);

// 异常断言
assertThatThrownBy(() -> engine.startWorkflow(null, inputs))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("workflowId");

// 集合断言
assertThat(tasks)
    .filteredOn(t -> t.getStatus() == TaskStatus.READY)
    .hasSize(2)
    .extracting(Task::getId)
    .contains("task-1", "task-2");

// 自定义条件
assertThat(task).satisfies(t -> {
    assertThat(t.getId()).isNotNull();
    assertThat(t.getStatus()).isIn(TaskStatus.PENDING, TaskStatus.READY);
});
```

---

## 3. Mock 使用规范

### 3.1 Mockito 最佳实践

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private AgentAdapter agentAdapter;
    
    @InjectMocks
    private TaskService taskService;
    
    @Test
    void shouldExecuteTaskWithAgent() {
        // Given
        Task task = createTask();
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(agentAdapter.execute(any())).thenReturn(createSuccessResult());
        
        // When
        taskService.executeTask("task-1");
        
        // Then
        verify(agentAdapter).execute(argThat(req -> 
            req.getPrompt().contains("实现")
        ));
        verify(taskRepository).save(task);
    }
    
    @Test
    void shouldRetryOnFailure() {
        // Given: 模拟失败两次后成功
        when(agentAdapter.execute(any()))
            .thenThrow(new RuntimeException("Error 1"))
            .thenThrow(new RuntimeException("Error 2"))
            .thenReturn(createSuccessResult());
        
        Task task = createTask();
        task.setMaxRetries(3);
        
        // When
        taskService.executeTask(task);
        
        // Then: 验证执行了 3 次
        verify(agentAdapter, times(3)).execute(any());
    }
}
```

### 3.2 Mock 原则

- ✅ Mock 外部依赖（Repository, Service, Adapter）
- ✅ 验证交互行为（verify）
- ❌ 不要 Mock 值对象
- ❌ 不要 Mock 被测类的方法

---

## 4. 集成测试

### 4.1 Spring Boot 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("工作流 API 集成测试")
class WorkflowApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @BeforeEach
    void setUp() {
        workflowRepository.deleteAll();
    }
    
    @Test
    @DisplayName("应该成功创建并执行工作流")
    void shouldCreateAndExecuteWorkflow() throws Exception {
        // Given
        WorkflowDefinition def = createWorkflowDefinition();
        
        // When: 创建工作流
        String workflowId = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(def)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // When: 执行工作流
        String executionId = mockMvc.perform(post("/api/v1/workflows/{id}/execute", workflowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        // Then: 验证执行状态
        await().atMost(Duration.ofSeconds(30))
            .until(() -> getExecutionStatus(executionId), equalTo("COMPLETED"));
    }
}
```

### 4.2 切片测试

```java
@DataJpaTest
@DisplayName("工作流 Repository 测试")
class WorkflowRepositoryTest {
    
    @Autowired
    private WorkflowRepository repository;
    
    @Test
    @DisplayName("应该根据状态查询工作流")
    void shouldFindByStatus() {
        // Given
        Workflow active = createWorkflow(WorkflowStatus.ACTIVE);
        Workflow draft = createWorkflow(WorkflowStatus.DRAFT);
        repository.saveAll(List.of(active, draft));
        
        // When
        List<Workflow> result = repository.findByStatus(WorkflowStatus.ACTIVE);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(WorkflowStatus.ACTIVE);
    }
}

@WebMvcTest(WorkflowController.class)
@DisplayName("工作流 Controller 测试")
class WorkflowControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private WorkflowService workflowService;
    
    @Test
    @DisplayName("应该返回 404 当工作流不存在")
    void shouldReturn404WhenWorkflowNotFound() throws Exception {
        when(workflowService.findById(any()))
            .thenThrow(new WorkflowNotFoundException("wf-001"));
        
        mockMvc.perform(get("/api/v1/workflows/wf-001"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Workflow not found"));
    }
}
```

---

## 5. 测试覆盖率

### 5.1 覆盖率目标

| 模块 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|---------|-----------|------|
| Domain | ≥ 95% | ≥ 90% | 核心业务逻辑必须全覆盖 |
| Application | ≥ 85% | ≥ 80% | 业务流程测试 |
| Infrastructure | ≥ 60% | ≥ 50% | 复杂逻辑测试 |
| **整体** | **≥ 80%** | **≥ 70%** | 项目平均 |

### 5.2 JaCoCo 配置

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/*Exception.class</exclude>
        </excludes>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

---

## 6. 测试数据管理

### 6.1 测试数据工厂

```java
public class TestDataFactory {
    
    public static Workflow createWorkflow() {
        Workflow workflow = new Workflow();
        workflow.setId("wf-001");
        workflow.setName("Test Workflow");
        workflow.setStatus(WorkflowStatus.ACTIVE);
        return workflow;
    }
    
    public static Workflow createWorkflow(String id, WorkflowStatus status) {
        Workflow workflow = createWorkflow();
        workflow.setId(id);
        workflow.setStatus(status);
        return workflow;
    }
    
    public static Task createTask() {
        return createTask(TaskStatus.PENDING);
    }
    
    public static Task createTask(TaskStatus status) {
        Task task = new Task();
        task.setId("task-001");
        task.setStatus(status);
        task.setPrompt("Test prompt");
        task.setAgentCode("claude-dev");
        return task;
    }
    
    public static DAG createSimpleDAG() {
        DAG dag = new DAG();
        dag.addNode(new Node("A"));
        dag.addNode(new Node("B"));
        dag.addNode(new Node("C"));
        dag.connect("A", "B");
        dag.connect("B", "C");
        return dag;
    }
    
    public static DAG createCyclicDAG() {
        DAG dag = new DAG();
        dag.addNode(new Node("A"));
        dag.addNode(new Node("B"));
        dag.addNode(new Node("C"));
        dag.connect("A", "B");
        dag.connect("B", "C");
        dag.connect("C", "A");  // 循环
        return dag;
    }
}
```

### 6.2 @Sql 注解

```java
@SpringBootTest
@Sql(scripts = "/sql/clear-tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/insert-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WorkflowIntegrationTest {
    // ...
}
```

---

## 7. TDD 实践

### 7.1 TDD 循环

```
1. 编写失败的测试（Red）
   ↓
2. 编写最简代码让测试通过（Green）
   ↓
3. 重构代码（Refactor）
   ↓
回到 1
```

### 7.2 TDD 示例

```java
// Step 1: 编写失败的测试
@Test
void shouldCalculateInDegree() {
    Node a = new Node("A");
    Node b = new Node("B");
    DAG dag = new DAG();
    dag.addNode(a);
    dag.addNode(b);
    dag.connect(a, b);
    
    int inDegree = dag.calculateInDegree(b);
    
    assertThat(inDegree).isEqualTo(1);
}

// Step 2: 最简实现
public int calculateInDegree(Node node) {
    return (int) edges.stream()
        .filter(e -> e.getTo().equals(node))
        .count();
}

// Step 3: 重构
public int calculateInDegree(Node node) {
    // 优化：缓存入度
    return inDegreeCache.getOrDefault(node.getId(), 0);
}
```

---

## 8. 测试检查清单

### 8.1 单元测试检查清单

- [ ] 测试名清晰描述行为和条件
- [ ] 使用 Given-When-Then 结构
- [ ] 一个测试只验证一个概念
- [ ] 使用 AssertJ 而非 JUnit 断言
- [ ] Mock 外部依赖
- [ ] 验证交互行为
- [ ] 测试边界条件
- [ ] 测试异常路径

### 8.2 集成测试检查清单

- [ ] 使用真实数据库（TestContainers）
- [ ] 测试事务回滚
- [ ] 测试并发场景
- [ ] 测试性能基线

---

**最后更新**: 2024年
**版本**: v1.0
