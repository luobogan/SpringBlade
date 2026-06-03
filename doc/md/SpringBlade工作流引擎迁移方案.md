# SpringBlade 工作流引擎迁移方案

> **目标**: 基于泛微E9工作流引擎设计，将其完整迁移至SpringBlade后端与ant-design-pro前端项目中
> **要求**: 实现工作流引擎与表单建模功能的深度集成，使用Univer替换ExcelDesign
> **日期**: 2026-06-02

---

## 目录

1. [概述](#1-概述)
2. [后端架构设计](#2-后端架构设计)
3. [前端架构设计](#3-前端架构设计)
4. [数据库设计](#4-数据库设计)
5. [接口定义](#5-接口定义)
6. [关键集成代码](#6-关键集成代码)
7. [Univer替换ExcelDesign方案](#7-univer替换exceldesign方案)
8. [部署方案](#8-部署方案)

---

## 1. 概述

### 1.1 迁移目标

| 目标 | 说明 |
|------|------|
| **完整迁移** | 将泛微E9工作流引擎完整迁移到SpringBlade项目 |
| **深度集成** | 工作流引擎与表单建模功能深度集成 |
| **Univer替换** | 使用Univer替换ExcelDesign模块 |
| **JSON格式** | 采用JSON格式存储（与泛微E9实际格式一致） |
| **现代化技术栈** | Spring Boot 3.5.9 + React 18 + TypeScript |

### 1.2 技术栈

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 开发语言 |
| Spring Boot | 3.5.9 | 应用框架 |
| Spring Cloud | 2025.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2025.0.0.0 | 微服务生态 |
| MyBatis Plus | 3.5.19 | ORM框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存 |
| Nacos | 3.1.1 | 服务注册与配置中心 |
| Sentinel | 2.0+ | 服务保护 |
| Seata | 2.0+ | 分布式事务 |

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18+ | 前端框架 |
| TypeScript | 5+ | 类型系统 |
| Ant Design Pro | 5+ | UI组件库 |
| @univerjs/core | latest | Excel组件（替换SpreadJS） |
| @univerjs/sheets | latest | 表格核心 |
| @univerjs/sheets-ui | latest | 表格UI |
| @dnd-kit/core | latest | 拖拽功能 |
| Umi Max | 4+ | 前端框架 |
| Vite | 5+ | 构建工具 |

### 1.3 核心模块划分

```
SpringBlade 工作流引擎
├── blade-workflow/              # 工作流引擎模块
│   ├── src/main/java/org/springblade/workflow/
│   │   ├── controller/          # 控制器层
│   │   ├── entity/              # 实体类
│   │   ├── dto/                 # 数据传输对象
│   │   ├── vo/                  # 视图对象
│   │   ├── mapper/              # 数据访问层
│   │   ├── service/             # 服务接口
│   │   │   └── impl/            # 服务实现
│   │   ├── engine/              # 工作流引擎核心
│   │   ├── action/              # 工作流动作
│   │   ├── request/             # 流程请求处理
│   │   ├── node/                # 节点管理
│   │   ├── config/              # 配置类
│   │   └── utils/               # 工具类
│   └── src/main/resources/
│       ├── mapper/               # MyBatis XML映射文件
│       └── application.yml       # 配置文件
├── blade-formmode/              # 表单建模模块
│   └── src/main/java/org/springblade/formmode/
│       ├── controller/
│       ├── entity/
│       ├── service/
│       └── engine/               # 表单建模引擎
└── ant-design-pro/              # 前端项目
    └── src/pages/
        ├── WorkflowDesign/       # 工作流设计器
        ├── FormDesign/           # 表单设计器（Univer）
        └── WorkflowRequest/      # 流程请求处理
```

---

## 2. 后端架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Web 层（Controller）                       │
│  WorkflowController / NodeController / RequestController    │
│  FormLayoutController / ActionController                    │
├─────────────────────────────────────────────────────────────┤
│                  Service 层（Business Logic）                 │
│  IWorkflowService / INodeService / IRequestService         │
│  IFormLayoutService / IActionService                       │
│  WorkflowEngine / NodeEngine / RequestEngine               │
├─────────────────────────────────────────────────────────────┤
│                  Manager 层（业务管理器）                     │
│  WorkflowManager / RequestManager / NodeManager             │
│  ActionManager / FormLayoutManager                         │
├─────────────────────────────────────────────────────────────┤
│                  DAO 层（Data Access）                       │
│  WorkflowMapper / NodeMapper / RequestMapper               │
│  FormLayoutMapper / ActionMapper                           │
├─────────────────────────────────────────────────────────────┤
│                  数据库（MySQL）                              │
│  workflow_base / workflow_nodebase / workflow_requestbase   │
│  form_layout / workflow_action                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心类设计

#### 2.2.1 WorkflowManager（工作流管理器）

```java
package org.springblade.workflow.manager;

/**
 * 工作流管理器（对标泛微E9的WFManager）
 */
public class WorkflowManager {

    private Long workflowId;
    private Long formId;
    private String workflowName;
    private String workflowDesc;
    private Long workflowType;
    private Integer isBill;
    private Integer isValid;
    private Integer multiSubmit;
    private Integer needAffirmance;
    private Integer isRemarks;
    private Integer isAnnexUpload;
    private Integer isFree;
    private Integer isAutoApprove;
    private Integer isAutoCommit;
    private Integer version;
    private Long activeVersionId;

    /**
     * 保存工作流信息
     */
    public boolean saveWorkflowInfo() {
        // 实现逻辑
        return true;
    }

    /**
     * 删除工作流信息
     */
    public boolean deleteWorkflowInfo() {
        // 实现逻辑
        return true;
    }

    /**
     * 获取工作流信息
     */
    public WorkflowManager getWorkflowInfo(Long workflowId) {
        // 实现逻辑
        return null;
    }

    /**
     * 检查工作流是否有效
     */
    public boolean isValid() {
        // 实现逻辑
        return true;
    }
}
```

#### 2.2.2 RequestManager（流程请求管理器）

```java
package org.springblade.workflow.manager;

/**
 * 流程请求管理器（对标泛微E9的RequestManager）
 */
public class RequestManager {

    private String src;                    // 操作类型
    private String isCreate;               // 是否为创建节点
    private Long requestId;                // 请求ID
    private Long workflowId;               // 工作流ID
    private String workflowType;            // 工作流类型
    private Integer isRemark;              // 是否是批注提交
    private Long formId;                   // 表单或单据ID
    private Integer isBill;                // 是否单据
    private Long billId;                   // 单据表ID
    private Long nodeId;                   // 节点ID
    private String nodeType;               // 节点类型
    private String requestName;            // 请求名称
    private String requestMark;            // 请求编号
    private String requestLevel;           // 请求重要级别
    private String remark;                 // 签字批注

    private Long nextNodeId;               // 下一个节点ID
    private String nextNodeType;           // 下一个节点类型
    private Long lastNodeId;               // 前一个节点ID
    private String lastNodeType;           // 前一个节点类型

    private Long creater;                  // 请求创建者
    private String createDate;             // 请求创建日期
    private String createTime;              // 请求创建时间

    private Float nodePassTime;            // 节点超时时间
    private Float nodeLeftTime;            // 节点剩余时间

    private boolean isStart;               // 流程是否已开始流转
    private boolean isTrack;               // 是否记录修改日志
    private boolean isAgent;               // 是否代理的流程

    /**
     * 保存流程请求
     */
    public boolean saveRequest() {
        // 实现逻辑
        return true;
    }

    /**
     * 提交流程请求
     */
    public boolean submitRequest() {
        // 实现逻辑
        return true;
    }

    /**
     * 退回流程请求
     */
    public boolean rejectRequest() {
        // 实现逻辑
        return true;
    }

    /**
     * 转发流程请求
     */
    public boolean forwardRequest() {
        // 实现逻辑
        return true;
    }

    /**
     * 撤回流程请求
     */
    public boolean drawRequest() {
        // 实现逻辑
        return true;
    }
}
```

#### 2.2.3 WorkflowEngine（工作流引擎核心）

```java
package org.springblade.workflow.engine;

/**
 * 工作流引擎核心（对标泛微E9的工作流引擎）
 */
@Service
public class WorkflowEngine {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ActionService actionService;

    /**
     * 创建工作流实例
     */
    public Long createWorkflowInstance(Long workflowId, Long userId) {
        // 1. 创建工作流请求
        // 2. 初始化第一个节点
        // 3. 执行节点前操作
        // 4. 返回请求ID
        return null;
    }

    /**
     * 提交流程节点
     */
    public boolean submitNode(Long requestId, Long nodeId, String remark) {
        // 1. 验证当前节点
        // 2. 执行节点后操作
        // 3. 计算下一个节点
        // 4. 更新请求状态
        // 5. 执行下一个节点的节点前操作
        return true;
    }

    /**
     * 退回流程节点
     */
    public boolean rejectNode(Long requestId, Long nodeId, Long rejectNodeId) {
        // 1. 验证退回权限
        // 2. 执行节点后操作（退回）
        // 3. 更新请求状态
        // 4. 执行退回节点的节点前操作
        return true;
    }

    /**
     * 计算下一个节点
     */
    public Long calculateNextNode(Long requestId, Long currentNodeId) {
        // 1. 读取节点连接配置
        // 2. 判断条件
        // 3. 返回下一个节点ID
        return null;
    }

    /**
     * 执行节点动作
     */
    public boolean executeAction(Long requestId, Long nodeId, String actionType) {
        // 1. 读取节点动作配置
        // 2. 执行动作
        // 3. 记录动作日志
        return true;
    }
}
```

### 2.3 服务层设计

#### 2.3.1 IWorkflowService（工作流服务接口）

```java
package org.springblade.workflow.service;

/**
 * 工作流服务接口
 */
public interface IWorkflowService extends IService<WorkflowEntity> {

    /**
     * 创建工作流
     */
    Long createWorkflow(WorkflowDTO workflowDTO);

    /**
     * 更新工作流
     */
    boolean updateWorkflow(WorkflowDTO workflowDTO);

    /**
     * 删除工作流
     */
    boolean deleteWorkflow(Long workflowId);

    /**
     * 获取工作流详情
     */
    WorkflowVO getWorkflowDetail(Long workflowId);

    /**
     * 获取工作流列表
     */
    List<WorkflowVO> getWorkflowList(WorkflowQuery query);

    /**
     * 启用/禁用工作流
     */
    boolean enableWorkflow(Long workflowId, Integer status);
}
```

#### 2.3.2 IRequestService（流程请求服务接口）

```java
package org.springblade.workflow.service;

/**
 * 流程请求服务接口
 */
public interface IRequestService extends IService<RequestEntity> {

    /**
     * 创建流程请求
     */
    Long createRequest(RequestDTO requestDTO);

    /**
     * 提交流程请求
     */
    boolean submitRequest(Long requestId, String remark);

    /**
     * 退回流程请求
     */
    boolean rejectRequest(Long requestId, Long rejectNodeId, String remark);

    /**
     * 转发流程请求
     */
    boolean forwardRequest(Long requestId, Long forwardUserId, String remark);

    /**
     * 撤回流程请求
     */
    boolean drawRequest(Long requestId);

    /**
     * 获取流程请求详情
     */
    RequestVO getRequestDetail(Long requestId);

    /**
     * 获取流程请求列表
     */
    List<RequestVO> getRequestList(RequestQuery query);
}
```

### 2.4 控制器层设计

#### 2.4.1 WorkflowController（工作流控制器）

```java
package org.springblade.workflow.controller;

/**
 * 工作流控制器
 */
@RestController
@RequestMapping("/api/blade-workflow/workflow")
@Tag(name = "工作流管理", description = "工作流的增删改查")
public class WorkflowController {

    @Autowired
    private IWorkflowService workflowService;

    @PostMapping("/create")
    @Operation(summary = "创建工作流")
    public R<Long> createWorkflow(@RequestBody WorkflowDTO workflowDTO) {
        Long workflowId = workflowService.createWorkflow(workflowDTO);
        return R.data(workflowId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作流")
    public R<Boolean> updateWorkflow(@RequestBody WorkflowDTO workflowDTO) {
        boolean result = workflowService.updateWorkflow(workflowDTO);
        return R.data(result);
    }

    @DeleteMapping("/delete/{workflowId}")
    @Operation(summary = "删除工作流")
    public R<Boolean> deleteWorkflow(@PathVariable Long workflowId) {
        boolean result = workflowService.deleteWorkflow(workflowId);
        return R.data(result);
    }

    @GetMapping("/detail/{workflowId}")
    @Operation(summary = "获取工作流详情")
    public R<WorkflowVO> getWorkflowDetail(@PathVariable Long workflowId) {
        WorkflowVO workflowVO = workflowService.getWorkflowDetail(workflowId);
        return R.data(workflowVO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取工作流列表")
    public R<List<WorkflowVO>> getWorkflowList(WorkflowQuery query) {
        List<WorkflowVO> list = workflowService.getWorkflowList(query);
        return R.data(list);
    }
}
```

#### 2.4.2 RequestController（流程请求控制器）

```java
package org.springblade.workflow.controller;

/**
 * 流程请求控制器
 */
@RestController
@RequestMapping("/api/blade-workflow/request")
@Tag(name = "流程请求管理", description = "流程请求的增删改查和操作")
public class RequestController {

    @Autowired
    private IRequestService requestService;

    @PostMapping("/create")
    @Operation(summary = "创建流程请求")
    public R<Long> createRequest(@RequestBody RequestDTO requestDTO) {
        Long requestId = requestService.createRequest(requestDTO);
        return R.data(requestId);
    }

    @PostMapping("/submit/{requestId}")
    @Operation(summary = "提交流程请求")
    public R<Boolean> submitRequest(
            @PathVariable Long requestId,
            @RequestParam String remark) {
        boolean result = requestService.submitRequest(requestId, remark);
        return R.data(result);
    }

    @PostMapping("/reject")
    @Operation(summary = "退回流程请求")
    public R<Boolean> rejectRequest(
            @RequestParam Long requestId,
            @RequestParam Long rejectNodeId,
            @RequestParam String remark) {
        boolean result = requestService.rejectRequest(requestId, rejectNodeId, remark);
        return R.data(result);
    }

    @PostMapping("/forward")
    @Operation(summary = "转发流程请求")
    public R<Boolean> forwardRequest(
            @RequestParam Long requestId,
            @RequestParam Long forwardUserId,
            @RequestParam String remark) {
        boolean result = requestService.forwardRequest(requestId, forwardUserId, remark);
        return R.data(result);
    }

    @PostMapping("/draw/{requestId}")
    @Operation(summary = "撤回流程请求")
    public R<Boolean> drawRequest(@PathVariable Long requestId) {
        boolean result = requestService.drawRequest(requestId);
        return R.data(result);
    }
}
```

---

## 3. 前端架构设计

### 3.1 目录结构

```
ant-design-pro/src/pages/
├── WorkflowDesign/               # 工作流设计器
│   ├── components/
│   │   ├── WorkflowCanvas.tsx     # 工作流画布
│   │   ├── NodePalette.tsx        # 节点面板
│   │   ├── NodeConfig.tsx        # 节点配置
│   │   ├── LinkConfig.tsx        # 连接配置
│   │   └── Toolbar.tsx           # 工具栏
│   ├── services/
│   │   └── workflowService.ts     # 工作流API服务
│   ├── models/
│   │   └── workflowModel.ts       # 工作流数据模型
│   ├── WorkflowDesign.tsx         # 主页面
│   └── index.ts                   # 导出
├── FormDesign/                    # 表单设计器（Univer）
│   ├── components/
│   │   ├── UniverGrid.tsx         # Univer表格组件
│   │   ├── FieldPalette.tsx       # 字段面板
│   │   ├── TabManager.tsx         # Tab页签管理
│   │   ├── GroupBox.tsx           # 分组框组件
│   │   └── PropertyPanel.tsx      # 属性配置面板
│   ├── services/
│   │   └── formDesignService.ts   # 表单设计API服务
│   ├── models/
│   │   └── formDesignModel.ts      # 表单设计数据模型
│   ├── utils/
│   │   └── univerAdapter.ts        # Univer适配器
│   ├── FormDesign.tsx             # 主页面
│   └── index.ts                   # 导出
├── WorkflowRequest/               # 流程请求处理
│   ├── components/
│   │   ├── RequestForm.tsx        # 请求表单
│   │   ├── RequestList.tsx        # 请求列表
│   │   ├── RequestDetail.tsx      # 请求详情
│   │   └── RequestHistory.tsx     # 请求历史
│   ├── services/
│   │   └── requestService.ts      # 请求API服务
│   ├── WorkflowRequest.tsx        # 主页面
│   └── index.ts                   # 导出
└── WorkflowMonitor/              # 工作流程监控
    ├── components/
    │   ├── MonitorDashboard.tsx    # 监控仪表盘
    │   ├── InstanceList.tsx       # 实例列表
    │   └── Statistics.tsx         # 统计信息
    ├── WorkflowMonitor.tsx         # 主页面
    └── index.ts                   # 导出
```

### 3.2 核心组件设计

#### 3.2.1 WorkflowCanvas（工作流画布）

```typescript
// WorkflowDesign/components/WorkflowCanvas.tsx
import React, { useEffect, useRef } from 'react';
import { Graph } from '@antv/x6';
import { Stencil } from '@antv/x6-plugin-stencil';
import { graphConfig, nodeConfig, edgeConfig } from './config';

interface WorkflowCanvasProps {
  workflowId: string;
  readonly?: boolean;
}

export const WorkflowCanvas: React.FC<WorkflowCanvasProps> = ({
  workflowId,
  readonly = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph>();

  useEffect(() => {
    if (!containerRef.current) return;

    // 初始化画布
    const graph = new Graph({
      container: containerRef.current,
      ...graphConfig,
      disabled: readonly,
    });

    // 加载工作流数据
    loadWorkflowData(graph, workflowId);

    graphRef.current = graph;

    return () => {
      graph.dispose();
    };
  }, [workflowId, readonly]);

  return (
    <div className="workflow-canvas">
      <div ref={containerRef} className="canvas-container" />
    </div>
  );
};
```

#### 3.2.2 UniverGrid（Univer表格组件）

```typescript
// FormDesign/components/UniverGrid.tsx
import React, { useEffect, useRef } from 'react';
import { createUniver, UniverSheetsCorePreset } from '@univerjs/preset-sheets-core';
import '@univerjs/preset-sheets-core/lib/index.css';
import type { IUniverInstance } from '@univerjs/core';

interface UniverGridProps {
  layoutData?: any;
  readonly?: boolean;
  onCellClick?: (row: number, col: number) => void;
  onFieldDrop?: (fieldId: string, row: number, col: number) => void;
}

export const UniverGrid: React.FC<UniverGridProps> = ({
  layoutData,
  readonly = false,
  onCellClick,
  onFieldDrop,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const univerAPIRef = useRef<any>();

  useEffect(() => {
    if (!containerRef.current) return;

    // 初始化Univer
    const { univerAPI } = createUniver({
      presets: [
        UniverSheetsCorePreset({
          container: containerRef.current,
        }),
      ],
    });

    univerAPIRef.current = univerAPI;

    // 加载布局数据
    if (layoutData) {
      loadLayoutData(univerAPI, layoutData);
    }

    // 绑定事件
    bindEvents(univerAPI, { onCellClick, onFieldDrop });

    return () => {
      // 清理资源
    };
  }, [layoutData]);

  return (
    <div className="univer-grid">
      <div ref={containerRef} className="univer-container" />
    </div>
  );
};

/**
 * 加载布局数据
 */
const loadLayoutData = (univerAPI: any, layoutData: any) => {
  const workbook = univerAPI.getActiveWorkbook();
  // 解析JSON数据并加载到Univer
  // ...
};

/**
 * 绑定事件
 */
const bindEvents = (
  univerAPI: any,
  handlers: {
    onCellClick?: (row: number, col: number) => void;
    onFieldDrop?: (fieldId: string, row: number, col: number) => void;
  }
) => {
  const workbook = univerAPI.getActiveWorkbook();
  
  // 绑定单元格点击事件
  if (handlers.onCellClick) {
    workbook.on(FUniver.Events.EnterCell, (cell: any) => {
      handlers.onCellClick!(cell.row, cell.col);
    });
  }

  // 绑定拖拽事件
  if (handlers.onFieldDrop) {
    // 实现拖拽逻辑
    // ...
  }
};
```

#### 3.2.3 RequestForm（请求表单）

```typescript
// WorkflowRequest/components/RequestForm.tsx
import React, { useEffect, useState } from 'react';
import { Form, Button, message } from 'antd';
import { useRequest } from 'ahooks';
import { requestService } from '../services/requestService';

interface RequestFormProps {
  requestId?: string;
  workflowId: string;
  readonly?: boolean;
  onSuccess?: () => void;
}

export const RequestForm: React.FC<RequestFormProps> = ({
  requestId,
  workflowId,
  readonly = false,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [formData, setFormData] = useState<any>({});

  // 获取表单布局
  const { data: layoutData, loading: layoutLoading } = useRequest(
    () => requestService.getFormLayout(workflowId),
    { ready: !!workflowId }
  );

  // 获取请求数据
  const { data: requestData, loading: requestLoading } = useRequest(
    () => requestService.getRequestData(requestId),
    { ready: !!requestId }
  );

  // 提交表单
  const { run: submit, loading: submitLoading } = useRequest(
    (values: any) => requestService.submitRequest(requestId, values),
    {
      manual: true,
      onSuccess: () => {
        message.success('提交成功');
        onSuccess?.();
      },
    }
  );

  useEffect(() => {
    if (requestData) {
      form.setFieldsValue(requestData);
      setFormData(requestData);
    }
  }, [requestData]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    await submit(values);
  };

  if (layoutLoading || requestLoading) {
    return <Spin />;
  }

  return (
    <div className="request-form">
      <UniverGrid layoutData={layoutData} readonly={readonly} />
      <div className="form-actions">
        <Button type="primary" onClick={handleSubmit} loading={submitLoading}>
          提交
        </Button>
      </div>
    </div>
  );
};
```

### 3.3 服务层设计

#### 3.3.1 workflowService（工作流API服务）

```typescript
// WorkflowDesign/services/workflowService.ts
import { request } from '@umijs/max';

export interface WorkflowDTO {
  workflowName: string;
  workflowDesc: string;
  workflowType: string;
  formId: string;
  isBill: number;
  isValid: number;
  multiSubmit: number;
  needAffirmance: number;
  isRemarks: number;
  isAnnexUpload: number;
  isFree: number;
  isAutoApprove: number;
  isAutoCommit: number;
}

export interface WorkflowVO {
  id: string;
  workflowName: string;
  workflowDesc: string;
  workflowType: string;
  formId: string;
  isBill: number;
  isValid: number;
  version: number;
}

export const workflowService = {
  /**
   * 创建工作流
   */
  createWorkflow: (data: WorkflowDTO) => {
    return request('/api/blade-workflow/workflow/create', {
      method: 'POST',
      data,
    });
  },

  /**
   * 更新工作流
   */
  updateWorkflow: (data: WorkflowDTO) => {
    return request('/api/blade-workflow/workflow/update', {
      method: 'PUT',
      data,
    });
  },

  /**
   * 删除工作流
   */
  deleteWorkflow: (workflowId: string) => {
    return request(`/api/blade-workflow/workflow/delete/${workflowId}`, {
      method: 'DELETE',
    });
  },

  /**
   * 获取工作流详情
   */
  getWorkflowDetail: (workflowId: string) => {
    return request(`/api/blade-workflow/workflow/detail/${workflowId}`);
  },

  /**
   * 获取工作流列表
   */
  getWorkflowList: (params: any) => {
    return request('/api/blade-workflow/workflow/list', {
      params,
    });
  },

  /**
   * 保存工作流设计
   */
  saveWorkflowDesign: (workflowId: string, designData: any) => {
    return request(`/api/blade-workflow/workflow/save-design/${workflowId}`, {
      method: 'POST',
      data: designData,
    });
  },

  /**
   * 获取工作流设计
   */
  getWorkflowDesign: (workflowId: string) => {
    return request(`/api/blade-workflow/workflow/get-design/${workflowId}`);
  },
};
```

#### 3.3.2 requestService（请求API服务）

```typescript
// WorkflowRequest/services/requestService.ts
import { request } from '@umijs/max';

export interface RequestDTO {
  workflowId: string;
  requestName: string;
  requestLevel: string;
  formData: any;
}

export interface RequestVO {
  id: string;
  workflowId: string;
  workflowName: string;
  requestName: string;
  requestStatus: string;
  creater: string;
  createTime: string;
  currentNodeId: string;
  currentNodeName: string;
}

export const requestService = {
  /**
   * 创建流程请求
   */
  createRequest: (data: RequestDTO) => {
    return request('/api/blade-workflow/request/create', {
      method: 'POST',
      data,
    });
  },

  /**
   * 提交流程请求
   */
  submitRequest: (requestId: string, remark: string) => {
    return request(`/api/blade-workflow/request/submit/${requestId}`, {
      method: 'POST',
      params: { remark },
    });
  },

  /**
   * 退回流程请求
   */
  rejectRequest: (requestId: string, rejectNodeId: string, remark: string) => {
    return request('/api/blade-workflow/request/reject', {
      method: 'POST',
      params: { requestId, rejectNodeId, remark },
    });
  },

  /**
   * 转发流程请求
   */
  forwardRequest: (requestId: string, forwardUserId: string, remark: string) => {
    return request('/api/blade-workflow/request/forward', {
      method: 'POST',
      params: { requestId, forwardUserId, remark },
    });
  },

  /**
   * 撤回流程请求
   */
  drawRequest: (requestId: string) => {
    return request(`/api/blade-workflow/request/draw/${requestId}`, {
      method: 'POST',
    });
  },

  /**
   * 获取流程请求详情
   */
  getRequestDetail: (requestId: string) => {
    return request(`/api/blade-workflow/request/detail/${requestId}`);
  },

  /**
   * 获取流程请求列表
   */
  getRequestList: (params: any) => {
    return request('/api/blade-workflow/request/list', {
      params,
    });
  },

  /**
   * 获取表单布局
   */
  getFormLayout: (workflowId: string) => {
    return request(`/api/blade-workflow/request/form-layout/${workflowId}`);
  },

  /**
   * 获取请求数据
   */
  getRequestData: (requestId: string) => {
    return request(`/api/blade-workflow/request/data/${requestId}`);
  },
};
```

---

## 4. 数据库设计

### 4.1 核心数据表

#### 4.1.1 workflow_base（工作流基础表）

```sql
CREATE TABLE IF NOT EXISTS `workflow_base` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
    `workflow_name`        VARCHAR(200) DEFAULT NULL COMMENT '工作流名称',
    `workflow_desc`        VARCHAR(500) DEFAULT NULL COMMENT '工作流描述',
    `workflow_type`        BIGINT       DEFAULT NULL COMMENT '工作流类型ID',
    `secure_level`         VARCHAR(3)    DEFAULT NULL COMMENT '安全级别',
    `form_id`             BIGINT       DEFAULT NULL COMMENT '表单ID',
    `user_id`             BIGINT       DEFAULT NULL COMMENT '创建用户ID',
    `is_bill`             INT          DEFAULT 0 COMMENT '是否单据（0：表单 1：单据）',
    `is_cust`             INT          DEFAULT 0 COMMENT '是否自定义',
    `help_doc_id`         BIGINT       DEFAULT NULL COMMENT '帮助文档ID',
    `is_valid`            INT          DEFAULT 1 COMMENT '是否有效（0：无效 1：有效）',
    `need_mark`           INT          DEFAULT 0 COMMENT '是否需要标记',
    `message_type`        INT          DEFAULT 0 COMMENT '消息类型',
    `multi_submit`        INT          DEFAULT 0 COMMENT '是否允许批量提交',
    `default_name`        INT          DEFAULT 0 COMMENT '默认名称',
    `doc_path`            VARCHAR(500) DEFAULT NULL COMMENT '文档路径',
    `sub_company_id`      BIGINT       DEFAULT NULL COMMENT '分公司ID',
    `mail_message_type`    INT          DEFAULT 0 COMMENT '邮件消息类型',
    `doc_right_by_operator` INT          DEFAULT 0 COMMENT '文档权限按操作者',
    `doc_category`        VARCHAR(200) DEFAULT NULL COMMENT '文档分类',
    `is_template`         INT          DEFAULT 0 COMMENT '是否模板',
    `template_id`         BIGINT       DEFAULT NULL COMMENT '模板ID',
    `need_affirmance`     INT          DEFAULT 0 COMMENT '是否需要确认',
    `is_remarks`          INT          DEFAULT 0 COMMENT '是否显示批注',
    `is_annex_upload`     INT          DEFAULT 0 COMMENT '是否允许附件上传',
    `annex_doc_category`  VARCHAR(200) DEFAULT NULL COMMENT '附件文档分类',
    `doc_category_type`   INT          DEFAULT 0 COMMENT '文档分类类型',
    `selected_category`   INT          DEFAULT 0 COMMENT '选中的分类',
    `doc_right_by_hrm`   BIGINT       DEFAULT NULL COMMENT '文档权限按人力资源',
    `title_field_id`      BIGINT       DEFAULT NULL COMMENT '标题字段ID',
    `keyword_field_id`    BIGINT       DEFAULT NULL COMMENT '关键字字段ID',
    `nosyn_fields`        VARCHAR(1000) DEFAULT NULL COMMENT '不同步字段',
    `sap_source`         VARCHAR(100)  DEFAULT NULL COMMENT 'SAP源',
    `is_modify_log`       INT          DEFAULT 1 COMMENT '是否修改日志',
    `is_show_modify_log`  INT          DEFAULT 0 COMMENT '是否显示修改日志',
    `show_del_button`     INT          DEFAULT 0 COMMENT '显示删除按钮',
    `special_approval`    INT          DEFAULT 0 COMMENT '特殊审批',
    `frequency`           VARCHAR(10)   DEFAULT NULL COMMENT '频率',
    `cycle`               VARCHAR(10)   DEFAULT NULL COMMENT '周期',
    `is_import_wf`        INT          DEFAULT 0 COMMENT '是否导入工作流',
    `import_readonly_field` VARCHAR(1000) DEFAULT NULL COMMENT '导入只读字段',
    `field_not_import`    VARCHAR(1000) DEFAULT NULL COMMENT '字段不导入',
    `wf_doc_path`        VARCHAR(500)  DEFAULT NULL COMMENT '工作流文档路径',
    `wf_doc_owner`       VARCHAR(100)   DEFAULT NULL COMMENT '工作流文档所有者',
    `wf_doc_owner_type`  VARCHAR(100)   DEFAULT NULL COMMENT '工作流文档所有者类型',
    `wf_doc_owner_fieldid` VARCHAR(100)  DEFAULT NULL COMMENT '工作流文档所有者字段ID',
    `is_edit`             INT          DEFAULT 0 COMMENT '是否编辑',
    `editor`              BIGINT       DEFAULT NULL COMMENT '编辑者',
    `edit_date`           DATE         DEFAULT NULL COMMENT '编辑日期',
    `edit_time`           TIME         DEFAULT NULL COMMENT '编辑时间',
    `is_shared`           INT          DEFAULT 0 COMMENT '是否共享',
    `is_forward_rights`   INT          DEFAULT 0 COMMENT '是否转发权限',
    `can_del_acc`         INT          DEFAULT 0 COMMENT '可以删除附件',
    `is_reject_remind`    INT          DEFAULT 0 COMMENT '是否退回提醒',
    `is_chang_reject_node` INT          DEFAULT 0 COMMENT '是否改变退回节点',
    `new_doc_path`        VARCHAR(500) DEFAULT NULL COMMENT '新文档路径',
    `is_sign_view`        INT          DEFAULT 0 COMMENT '是否签批查看',
    `allow_view_em_share_log` INT          DEFAULT 0 COMMENT '允许查看EM共享日志',
    `is_select_reject_node` INT          DEFAULT 0 COMMENT '是否选择退回节点',
    `is_need_del_acc`     INT          DEFAULT 0 COMMENT '是否需要删除附件',
    `sms_alert_type`     INT          DEFAULT 0 COMMENT '短信提醒类型',
    `is_forward_receive_def` INT          DEFAULT 0 COMMENT '是否转发接收默认',
    `is_tri_diff_workflow` INT          DEFAULT 0 COMMENT '是否触发不同工作流',
    `dsp_order`           INT          DEFAULT 0 COMMENT '显示顺序',
    `is_free`             INT          DEFAULT 0 COMMENT '是否自由流程',
    `is_over_rb`          INT          DEFAULT 0 COMMENT '是否覆盖RB',
    `is_over_iv`          INT          DEFAULT 0 COMMENT '是否覆盖IV',
    `custom_page`         VARCHAR(500) DEFAULT NULL COMMENT '自定义页面',
    `is_auto_approve`     INT          DEFAULT 0 COMMENT '是否自动批准',
    `is_auto_commit`      INT          DEFAULT 0 COMMENT '是否自动提交',
    `auto_flow_request_log_trail` INT DEFAULT 1 COMMENT '自动流转请求日志轨迹',
    `is_import_detail`    INT          DEFAULT 0 COMMENT '是否导入明细',
    `action`              VARCHAR(100)  DEFAULT NULL COMMENT '动作',
    `chats_type`          VARCHAR(100)  DEFAULT NULL COMMENT '聊天类型',
    `chats_alert_type`    VARCHAR(100)  DEFAULT NULL COMMENT '聊天提醒类型',
    `not_remind_if_archived` INT          DEFAULT 0 COMMENT '归档后不提醒',
    `archive_no_msg_alert` INT          DEFAULT 0 COMMENT '归档无消息提醒',
    `archive_no_mail_alert` INT          DEFAULT 0 COMMENT '归档无邮件提醒',
    `forbid_att_download`  INT          DEFAULT 0 COMMENT '禁止附件下载',
    `doc_right_by_operator` INT          DEFAULT 0 COMMENT '文档权限按操作者',
    `is_save_check_form`  INT          DEFAULT 0 COMMENT '是否保存检查表单',
    `show_upload_tab`      INT          DEFAULT 0 COMMENT '显示上传Tab',
    `is_sign_doc`         INT          DEFAULT 0 COMMENT '是否签批文档',
    `show_doc_tab`        INT          DEFAULT 0 COMMENT '显示文档Tab',
    `is_sign_workflow`    INT          DEFAULT 0 COMMENT '是否签批工作流',
    `show_workflow_tab`    INT          DEFAULT 0 COMMENT '显示工作流Tab',
    `is_use`              INT          DEFAULT 1 COMMENT '是否使用',
    `u_type`              INT          DEFAULT 1 COMMENT 'U类型',
    `t_type`              INT          DEFAULT 1 COMMENT 'T类型',
    `order_by_type`       INT          DEFAULT 0 COMMENT '按类型排序',
    `is_show_chart`       INT          DEFAULT 0 COMMENT '是否显示图表',
    `show_chart_url`      VARCHAR(500) DEFAULT NULL COMMENT '显示图表URL',
    `is_show_on_report_input` INT          DEFAULT 0 COMMENT '是否在报表输入上显示',
    `version`             INT          DEFAULT 1 COMMENT '版本号',
    `active_version_id`   BIGINT       DEFAULT NULL COMMENT '活跃版本ID',
    `title_set`           INT          DEFAULT 0 COMMENT '标题设置',
    `is_template`         INT          DEFAULT 0 COMMENT '是否模板',
    `tenant_id`           VARCHAR(32)   DEFAULT '000000' COMMENT '租户ID',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_type` (`workflow_type`),
    KEY `idx_form_id` (`form_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_is_valid` (`is_valid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流基础表';
```

#### 4.1.2 workflow_nodebase（工作流节点表）

```sql
CREATE TABLE IF NOT EXISTS `workflow_nodebase` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
    `node_name`           VARCHAR(200) DEFAULT NULL COMMENT '节点名称',
    `workflow_id`         BIGINT       DEFAULT NULL COMMENT '工作流ID',
    `node_type`           INT          DEFAULT 0 COMMENT '节点类型（0：创建 1：审批 2：实现 3：归档）',
    `is_start`            INT          DEFAULT 0 COMMENT '是否开始节点',
    `is_reject`           INT          DEFAULT 0 COMMENT '是否允许退回',
    `is_forward`          INT          DEFAULT 0 COMMENT '是否允许转发',
    `is_draw`             INT          DEFAULT 0 COMMENT '是否允许撤回',
    `is_return`           INT          DEFAULT 0 COMMENT '是否允许返回',
    `is_add_approver`     INT          DEFAULT 0 COMMENT '是否允许添加审批人',
    `is_sign`             INT          DEFAULT 0 COMMENT '是否签批',
    `is_multi_sign`       INT          DEFAULT 0 COMMENT '是否多人签批',
    `is_import_detail`     INT          DEFAULT 0 COMMENT '是否导入明细',
    `node_pass_time`      DECIMAL(10,2) DEFAULT NULL COMMENT '节点超时时间',
    `node_left_time`      DECIMAL(10,2) DEFAULT NULL COMMENT '节点剩余时间',
    `pass_can_view`       INT          DEFAULT 0 COMMENT '通过可查看',
    `pass_can_cancel_view` INT          DEFAULT 0 COMMENT '通过可取消查看',
    `no_pass_can_view`    INT          DEFAULT 0 COMMENT '不通过可查看',
    `node_name_label`     BIGINT       DEFAULT NULL COMMENT '节点名称标签',
    `is_used`             BIGINT       DEFAULT NULL COMMENT '是否使用',
    `is_free_node`        INT          DEFAULT 0 COMMENT '是否自由节点',
    `free_node_field`      VARCHAR(1000) DEFAULT NULL COMMENT '自由节点字段',
    `is_close_auto_approve` INT          DEFAULT 0 COMMENT '是否关闭自动批准',
    `is_open_auto_commit`  INT          DEFAULT 0 COMMENT '是否打开自动提交',
    `is_out_used`         INT          DEFAULT 0 COMMENT '是否外部使用',
    `is_out_condition`     INT          DEFAULT 0 COMMENT '是否外部条件',
    `is_out_operator`     INT          DEFAULT 0 COMMENT '是否外部操作者',
    `is_out_sign`         INT          DEFAULT 0 COMMENT '是否外部签批',
    `is_out_import_detail` INT          DEFAULT 0 COMMENT '是否外部导入明细',
    `is_out_pass_time`    INT          DEFAULT 0 COMMENT '是否外部超时时间',
    `is_out_left_time`    INT          DEFAULT 0 COMMENT '是否外部剩余时间',
    `is_out_pass_can_view` INT          DEFAULT 0 COMMENT '是否外部通过可查看',
    `is_out_pass_can_cancel_view` INT   DEFAULT 0 COMMENT '是否外部通过可取消查看',
    `is_out_no_pass_can_view` INT       DEFAULT 0 COMMENT '是否外部不通过可查看',
    `is_out_name_label`    INT          DEFAULT 0 COMMENT '是否外部名称标签',
    `is_out_free_node_field` INT        DEFAULT 0 COMMENT '是否外部自由节点字段',
    `is_out_close_auto_approve` INT    DEFAULT 0 COMMENT '是否外部关闭自动批准',
    `is_out_open_auto_commit` INT       DEFAULT 0 COMMENT '是否外部打开自动提交',
    `tenant_id`           VARCHAR(32)   DEFAULT '000000' COMMENT '租户ID',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点表';
```

#### 4.1.3 workflow_requestbase（流程请求基础表）

```sql
CREATE TABLE IF NOT EXISTS `workflow_requestbase` (
    `request_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '请求ID',
    `workflow_id`         BIGINT       DEFAULT NULL COMMENT '工作流ID',
    `workflow_type`       VARCHAR(100)  DEFAULT NULL COMMENT '工作流类型',
    `status`              VARCHAR(100)  DEFAULT NULL COMMENT '状态',
    `request_name`        VARCHAR(200) DEFAULT NULL COMMENT '请求名称',
    `request_mark`        VARCHAR(100)  DEFAULT NULL COMMENT '请求编号',
    `request_level`       VARCHAR(10)   DEFAULT NULL COMMENT '请求重要级别',
    `creater`             BIGINT       DEFAULT NULL COMMENT '创建者',
    `creater_type`        INT          DEFAULT 0 COMMENT '创建者类型',
    `create_date`         DATE         DEFAULT NULL COMMENT '创建日期',
    `create_time`         TIME         DEFAULT NULL COMMENT '创建时间',
    `last_operator`       BIGINT       DEFAULT NULL COMMENT '最后操作者',
    `last_operator_type`  INT          DEFAULT 0 COMMENT '最后操作者类型',
    `last_operate_date`   DATE         DEFAULT NULL COMMENT '最后操作日期',
    `last_operate_time`   TIME         DEFAULT NULL COMMENT '最后操作时间',
    `request_status`      VARCHAR(10)   DEFAULT NULL COMMENT '请求状态',
    `is_deleted`          INT          DEFAULT 0 COMMENT '是否删除',
    `is_remark`           INT          DEFAULT 0 COMMENT '是否批注',
    `current_node_id`     BIGINT       DEFAULT NULL COMMENT '当前节点ID',
    `current_node_type`   INT          DEFAULT NULL COMMENT '当前节点类型',
    `current_status`      INT          DEFAULT NULL COMMENT '当前状态',
    `receipt_time`        DATE         DEFAULT NULL COMMENT '接收时间',
    `receiptor`           BIGINT       DEFAULT NULL COMMENT '接收者',
    `receiptor_type`      INT          DEFAULT 0 COMMENT '接收者类型',
    `last_recipt_time`    DATE         DEFAULT NULL COMMENT '最后接收时间',
    `last_reciptor`       BIGINT       DEFAULT NULL COMMENT '最后接收者',
    `last_reciptor_type`  INT          DEFAULT 0 COMMENT '最后接收者类型',
    `archiver`             BIGINT       DEFAULT NULL COMMENT '归档者',
    `archive_date`        DATE         DEFAULT NULL COMMENT '归档日期',
    `archive_time`        TIME         DEFAULT NULL COMMENT '归档时间',
    `current_status_name` VARCHAR(200) DEFAULT NULL COMMENT '当前状态名称',
    `is_need_review`      INT          DEFAULT 0 COMMENT '是否需要审核',
    `reviewed_time`       DATE         DEFAULT NULL COMMENT '审核时间',
    `reviewed`             BIGINT       DEFAULT NULL COMMENT '审核者',
    `reviewed_type`        INT          DEFAULT 0 COMMENT '审核者类型',
    `is_intervenor`       INT          DEFAULT 0 COMMENT '是否干预',
    `intervenor_date`     DATE         DEFAULT NULL COMMENT '干预日期',
    `intervenor_time`     TIME         DEFAULT NULL COMMENT '干预时间',
    `intervenor`           BIGINT       DEFAULT NULL COMMENT '干预者',
    `intervenor_type`      INT          DEFAULT 0 COMMENT '干预者类型',
    `intervenor_remark`   VARCHAR(1000) DEFAULT NULL COMMENT '干预备注',
    `is_forward`          INT          DEFAULT 0 COMMENT '是否转发',
    `forward_from`        BIGINT       DEFAULT NULL COMMENT '转发来自',
    `forward_from_type`    INT          DEFAULT 0 COMMENT '转发来自类型',
    `forward_to`          BIGINT       DEFAULT NULL COMMENT '转发到',
    `forward_to_type`      INT          DEFAULT 0 COMMENT '转发到类型',
    `forward_time`        TIME         DEFAULT NULL COMMENT '转发时间',
    `forward_date`        DATE         DEFAULT NULL COMMENT '转发日期',
    `is_pending`          INT          DEFAULT 0 COMMENT '是否挂起',
    `pending_time`        DATE         DEFAULT NULL COMMENT '挂起时间',
    `pending_date`        DATE         DEFAULT NULL COMMENT '挂起日期',
    `pending_user_id`     BIGINT       DEFAULT NULL COMMENT '挂起用户ID',
    `pending_user_type`    INT          DEFAULT 0 COMMENT '挂起用户类型',
    `pending_remark`      VARCHAR(1000) DEFAULT NULL COMMENT '挂起备注',
    `is_read`              INT          DEFAULT 0 COMMENT '是否读取',
    `read_time`           DATE         DEFAULT NULL COMMENT '读取时间',
    `read_date`           DATE         DEFAULT NULL COMMENT '读取日期',
    `read_user_id`        BIGINT       DEFAULT NULL COMMENT '读取用户ID',
    `read_user_type`       INT          DEFAULT 0 COMMENT '读取用户类型',
    `read_remark`         VARCHAR(1000) DEFAULT NULL COMMENT '读取备注',
    `form_data`           JSON         DEFAULT NULL COMMENT '表单数据（JSON格式）',
    `tenant_id`           VARCHAR(32)   DEFAULT '000000' COMMENT '租户ID',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`request_id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_creater` (`creater`),
    KEY `idx_current_node_id` (`current_node_id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_request_status` (`request_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程请求基础表';
```

#### 4.1.4 form_layout（表单布局表）

```sql
CREATE TABLE IF NOT EXISTS `form_layout` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '布局ID',
    `form_id`             BIGINT       NOT NULL COMMENT '表单ID（关联workflow_bill）',
    `layout_name`         VARCHAR(200) DEFAULT NULL COMMENT '布局名称',
    `layout_json`         LONGTEXT     DEFAULT NULL COMMENT '布局JSON（兼容泛微E9格式）',
    `layout_config`        TEXT         DEFAULT NULL COMMENT '布局配置JSON',
    `status`               INT          DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `tenant_id`            VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_form_id` (`form_id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单布局表（JSON格式，兼容泛微E9）';
```

### 4.2 数据库表关系图

```
workflow_base (工作流基础表)
    ↓ 1:N
workflow_nodebase (节点表)
    ↓ 1:N
workflow_nodelink (节点连接表)
    ↓ 1:N
workflow_requestbase (流程请求表)
    ↓ 1:N
workflow_requestlog (流程日志表)

workflow_formbase (表单基础表)
    ↓ 1:N
workflow_formfield (表单字段表)

workflow_bill (单据表)
    ↓ 1:N
workflow_billfield (单据字段表)

workflow_nodehtmllayout (节点布局表)
    ↓ 关联
workflow_nodebase (节点表)

form_layout (表单布局表)
    ↓ 关联
workflow_bill (单据表)
```

---

## 5. 接口定义

### 5.1 工作流管理接口

| 接口路径 | 方法 | 说明 | 请求体 | 响应体 |
|---------|------|------|--------|--------|
| `/api/blade-workflow/workflow/create` | POST | 创建工作流 | WorkflowDTO | R<Long> |
| `/api/blade-workflow/workflow/update` | PUT | 更新工作流 | WorkflowDTO | R<Boolean> |
| `/api/blade-workflow/workflow/delete/{workflowId}` | DELETE | 删除工作流 | - | R<Boolean> |
| `/api/blade-workflow/workflow/detail/{workflowId}` | GET | 获取工作流详情 | - | R<WorkflowVO> |
| `/api/blade-workflow/workflow/list` | GET | 获取工作流列表 | WorkflowQuery | R<List<WorkflowVO>> |
| `/api/blade-workflow/workflow/save-design/{workflowId}` | POST | 保存工作流设计 | DesignDTO | R<Boolean> |
| `/api/blade-workflow/workflow/get-design/{workflowId}` | GET | 获取工作流设计 | - | R<DesignVO> |
| `/api/blade-workflow/workflow/enable/{workflowId}` | POST | 启用/禁用工作流 | status | R<Boolean> |

### 5.2 流程请求接口

| 接口路径 | 方法 | 说明 | 请求体 | 响应体 |
|---------|------|------|--------|--------|
| `/api/blade-workflow/request/create` | POST | 创建流程请求 | RequestDTO | R<Long> |
| `/api/blade-workflow/request/submit/{requestId}` | POST | 提交流程请求 | remark | R<Boolean> |
| `/api/blade-workflow/request/reject` | POST | 退回流程请求 | requestId, rejectNodeId, remark | R<Boolean> |
| `/api/blade-workflow/request/forward` | POST | 转发流程请求 | requestId, forwardUserId, remark | R<Boolean> |
| `/api/blade-workflow/request/draw/{requestId}` | POST | 撤回流程请求 | - | R<Boolean> |
| `/api/blade-workflow/request/detail/{requestId}` | GET | 获取请求详情 | - | R<RequestVO> |
| `/api/blade-workflow/request/list` | GET | 获取请求列表 | RequestQuery | R<List<RequestVO>> |
| `/api/blade-workflow/request/form-layout/{workflowId}` | GET | 获取表单布局 | - | R<FormLayoutVO> |
| `/api/blade-workflow/request/data/{requestId}` | GET | 获取请求数据 | - | R<FormDataVO> |

### 5.3 表单设计接口

| 接口路径 | 方法 | 说明 | 请求体 | 响应体 |
|---------|------|------|--------|--------|
| `/api/blade-formmode/form-layout/save` | POST | 保存表单布局 | FormLayoutDTO | R<Boolean> |
| `/api/blade-formmode/form-layout/{formId}` | GET | 读取表单布局 | - | R<FormLayoutVO> |
| `/api/blade-formmode/form-layout/parse-json` | POST | 解析JSON布局 | json | R<LayoutDataVO> |
| `/api/blade-formmode/form-layout/{id}` | DELETE | 删除表单布局 | - | R<Boolean> |

### 5.4 JSON格式说明

#### 5.4.1 表单布局JSON格式（兼容泛微E9）

```json
{
  "eformdesign": {
    "etables": {
      "Sheet1": {
        "data": {
          "dataTable": {
            "0": {
              "0": {
                "v": "字段标签",
                "hv": "字段值",
                "t": "字段类型"
              }
            }
          }
        },
        "styles": {
          "styleTable": {
            "0": {
              "0": {
                "font": {
                  "name": "宋体",
                  "size": 12,
                  "bold": false,
                  "italic": false,
                  "underline": false
                },
                "backColor": "#FFFFFF",
                "foreColor": "#000000"
              }
            }
          }
        },
        "mergedCells": ["0:0:1:0"]
      }
    },
    "formula": {
      "A1": "SUM(B1:C1)"
    },
    "tabs": [
      {
        "id": "tab1",
        "name": "基本信息",
        "sheetName": "Sheet1"
      }
    ],
    "groupBoxes": [
      {
        "id": "group1",
        "name": "分组框1",
        "position": {
          "row": 0,
          "col": 0,
          "rowCount": 5,
          "colCount": 5
        }
      }
    ]
  }
}
```

---

## 6. 关键集成代码

### 6.1 工作流引擎与表单建模集成

#### 6.1.1 表单数据触发工作流

```java
// org.springblade.formmode.engine.ModeDataApproval
@Service
public class ModeDataApproval {

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private IRequestService requestService;

    /**
     * 判断触发条件
     */
    public boolean checkTriggerCondition(Long modeId, String operationType) {
        // 1. 读取触发设置 (triggerWorkflowSetId)
        // 2. 检查触发操作类型（新建/编辑/删除）
        // 3. 显示条件 (GetShowCondition)
        // 4. 返回是否触发
        return true;
    }

    /**
     * 触发工作流审批
     */
    public Long triggerWorkflow(Long modeId, Long dataId, String operationType) {
        // 1. 检查触发条件
        if (!checkTriggerCondition(modeId, operationType)) {
            return null;
        }

        // 2. 字段映射：流程字段 ←→ 模块字段
        Map<String, Object> fieldMapping = buildFieldMapping(modeId);

        // 3. 创建工作流实例
        Long requestId = workflowEngine.createWorkflowInstance(
            getWorkflowId(modeId),
            getUserId()
        );

        // 4. 填充表单数据
        fillFormData(requestId, dataId, fieldMapping);

        // 5. 提交工作流
        workflowEngine.submitNode(requestId, getStartNodeId(), "自动触发");

        // 6. 返回请求ID
        return requestId;
    }

    /**
     * 工作流审批完成后回写数据
     */
    public void writeBackData(Long requestId, String approveResult) {
        // 1. 读取字段映射配置
        Map<String, Object> fieldMapping = getFieldMapping(requestId);

        // 2. 成功回写 (successwriteback)
        if ("success".equals(approveResult)) {
            writeBackSuccess(fieldMapping);
        }
        // 3. 失败回写 (failwriteback)
        else {
            writeBackFail(fieldMapping);
        }
    }
}
```

#### 6.1.2 工作流数据转换为模块数据

```java
// org.springblade.workflow.action.WorkflowToMode
@Service
public class WorkflowToMode {

    @Autowired
    private IModeDataService modeDataService;

    /**
     * 工作流数据转换为模块数据
     */
    public Long convertWorkflowToMode(Long requestId, Long modeId) {
        // 1. 读取工作流表单数据
        Map<String, Object> workflowData = getWorkflowData(requestId);

        // 2. 字段映射：工作流字段 → 模块字段
        Map<String, Object> modeData = mappingFields(workflowData, modeId);

        // 3. 保存模块数据
        Long dataId = modeDataService.saveModeData(modeId, modeData);

        // 4. 记录转换日志
        logConversion(requestId, modeId, dataId);

        // 5. 返回数据ID
        return dataId;
    }

    /**
     * 字段映射
     */
    private Map<String, Object> mappingFields(
            Map<String, Object> workflowData,
            Long modeId) {
        // 1. 读取字段映射配置 (wf_mf_map)
        List<FieldMappingConfig> mappingConfig = getMappingConfig(modeId);

        // 2. 执行字段映射
        Map<String, Object> modeData = new HashMap<>();
        for (FieldMappingConfig config : mappingConfig) {
            Object value = workflowData.get(config.getWorkflowField());
            modeData.put(config.getModeField(), value);
        }

        // 3. 返回映射后的数据
        return modeData;
    }
}
```

### 6.2 前端与后端数据联动

#### 6.2.1 表单数据提交

```typescript
// WorkflowRequest/services/requestService.ts

/**
 * 提交表单数据
 */
submitFormData: (requestId: string, formData: any) => {
  return request(`/api/blade-workflow/request/submit-data/${requestId}`, {
    method: 'POST',
    data: {
      formData: JSON.stringify(formData),
    },
  });
},
```

#### 6.2.2 Univer数据同步到工作流

```typescript
// FormDesign/components/UniverGrid.tsx

/**
 * Univer数据变化事件
 */
useEffect(() => {
  if (!univerAPIRef.current) return;

  const workbook = univerAPIRef.current.getActiveWorkbook();

  // 监听单元格值变化
  workbook.on(FUniver.Events.CellValueChange, (cell: any, oldValue: any, newValue: any) => {
    // 1. 获取所有表单数据
    const formData = extractFormData(univerAPIRef.current);

    // 2. 同步到工作流引擎
    requestService.syncFormData(requestId, formData);

    // 3. 更新本地状态
    setFormData(formData);
  });
}, [requestId]);

/**
 * 从Univer提取表单数据
 */
const extractFormData = (univerAPI: any) => {
  const workbook = univerAPI.getActiveWorkbook();
  const sheet = workbook.getActiveSheet();
  const formData: any = {};

  // 遍历所有单元格，提取字段数据
  const cellData = sheet.getCellData();
  for (const [row, rowData] of Object.entries(cellData)) {
    for (const [col, cell] of Object.entries(rowData as any)) {
      if (cell.tag && cell.tag.fieldId) {
        formData[cell.tag.fieldId] = cell.v;
      }
    }
  }

  return formData;
};
```

---

## 7. Univer替换ExcelDesign方案

### 7.1 迁移策略

```
阶段一：基础设施准备
    ↓
阶段二：核心模块迁移（FormDesign）
    ↓
阶段三：工作流设计器迁移（WorkflowDesign）
    ↓
阶段四：全面替换与优化
```

### 7.2 核心API映射

| SpreadJS | Univer | 说明 |
|----------|--------|------|
| `new GC.Spread.Sheets.Workbook(container)` | `createUniver()` + `UniverSheetsCorePreset()` | 创建工作簿 |
| `wijspread({sheetCount:1})` | `{ sheet: { [sheetId]: {} } }` | 配置工作表 |
| `.getActiveSheet()` | `univerAPI.getActiveWorkbook().getActiveSheet()` | 获取活动表 |
| `.toJSON()` | `univerAPI.getActiveWorkbook().toJSON()` | 序列化 |
| `sheet.getCell(r, c)` | `sheet.getRange(r, c, 1, 1)` | 获取单元格 |
| `cell.value()` | `range.getValue()` | 获取值 |
| `cell.value(val)` | `range.setValue(val)` | 设置值 |
| `cell.text()` | `range.getText()` | 获取文本 |
| `cell.backColor(color)` | `range.getCell().setBackgroundColor(color)` | 背景色 |

### 7.3 Univer适配器

```typescript
// FormDesign/utils/univerAdapter.ts

/**
 * Univer适配器（兼容SpreadJS API）
 */
export class UniverAdapter {
  private univerAPI: any;

  constructor(univerAPI: any) {
    this.univerAPI = univerAPI;
  }

  /**
   * 获取活动工作表（兼容SpreadJS的getActiveSheet）
   */
  getActiveSheet(): any {
    return this.univerAPI.getActiveWorkbook().getActiveSheet();
  }

  /**
   * 获取单元格（兼容SpreadJS的getCell）
   */
  getCell(row: number, col: number): any {
    const sheet = this.getActiveSheet();
    const range = sheet.getRange(row, col, 1, 1);
    return {
      value: () => range.getValue(),
      value: (val: any) => range.setValue(val),
      text: () => range.getText(),
      backColor: (color: string) => range.getCell().setBackgroundColor(color),
    };
  }

  /**
   * 序列化为JSON（兼容SpreadJS的toJSON）
   */
  toJSON(): any {
    return this.univerAPI.getActiveWorkbook().toJSON();
  }

  /**
   * 从JSON加载（兼容SpreadJS的fromJSON）
   */
  fromJSON(json: any): void {
    const workbook = this.univerAPI.getActiveWorkbook();
    workbook.load(json);
  }
}
```

### 7.4 字段拖拽到Univer单元格

```typescript
// FormDesign/components/FieldPalette.tsx

/**
 * 字段面板（可拖拽）
 */
export const FieldPalette: React.FC = () => {
  const { attributes, listeners, setNodeRef, transform, transition } = useDraggable({
    id: 'field-palette',
    data: { type: 'field' },
  });

  const style = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
    transition,
  } : undefined;

  return (
    <div ref={setNodeRef} style={style} {...listeners} {...attributes}>
      <div className="field-palette">
        <h3>字段面板</h3>
        <div className="field-list">
          <DraggableField fieldType="text" label="单行文本" />
          <DraggableField fieldType="textarea" label="多行文本" />
          <DraggableField fieldType="number" label="数字" />
          <DraggableField fieldType="date" label="日期" />
          <DraggableField fieldType="select" label="下拉框" />
          <DraggableField fieldType="checkbox" label="复选框" />
          <DraggableField fieldType="radio" label="单选按钮" />
          <DraggableField fieldType="browser" label="浏览框" />
          <DraggableField fieldType="attachment" label="附件上传" />
        </div>
      </div>
    </div>
  );
};

/**
 * 可拖拽字段
 */
const DraggableField: React.FC<{ fieldType: string; label: string }> = ({
  fieldType,
  label,
}) => {
  const { attributes, listeners, setNodeRef, transform, transition } = useDraggable({
    id: `field-${fieldType}`,
    data: { type: 'field', fieldType },
  });

  const style = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
    transition,
  } : undefined;

  return (
    <div ref={setNodeRef} style={style} {...listeners} {...attributes}>
      <div className="field-item">
        <span className="field-icon">{getFieldIcon(fieldType)}</span>
        <span className="field-label">{label}</span>
      </div>
    </div>
  );
};
```

```typescript
// FormDesign/components/UniverGrid.tsx

/**
 * 字段拖拽放置区域（Univer单元格）
 */
useEffect(() => {
  if (!univerAPIRef.current) return;

  const workbook = univerAPIRef.current.getActiveWorkbook();

  // 监听拖拽事件
  workbook.on(FUniver.Events.Drop, (event: any) => {
    const { row, col } = event;
    const fieldType = event.dataTransfer.getData('fieldType');
    const fieldId = event.dataTransfer.getData('fieldId');

    // 放置字段到单元格
    handleFieldDrop(fieldId, fieldType, row, col);
  });
}, []);

/**
 * 处理字段拖拽放置
 */
const handleFieldDrop = (fieldId: string, fieldType: string, row: number, col: number) => {
  const sheet = univerAPIRef.current.getActiveWorkbook().getActiveSheet();
  const cell = sheet.getRange(row, col, 1, 1);

  // 设置字段标签
  cell.setValue(getFieldLabel(fieldType));

  // 设置字段标记（用于存储字段ID和类型）
  cell.setTag({
    fieldId,
    fieldType,
  });

  // 设置单元格样式
  cell.getCell().setBackgroundColor('#F0F0F0');
  cell.getCell().setBorderType('thin');
};
```

---

## 8. 部署方案

### 8.1 后端部署

#### 8.1.1 模块打包

```bash
# 1. 打包blade-workflow模块
cd d:/workproject/springbladeandreact/springBlade/blade-service/blade-workflow
mvn clean package -DskipTests

# 2. 打包blade-formmode模块
cd d:/workproject/springbladeandreact/springBlade/blade-service/blade-formmode
mvn clean package -DskipTests
```

#### 8.1.2 Docker部署

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/blade-workflow.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 8.2 前端部署

#### 8.2.1 构建

```bash
# 1. 安装依赖
cd d:/workproject/springbladeandreact/ant-design-pro
pnpm install

# 2. 构建生产版本
pnpm build

# 3. 输出目录：dist/
```

#### 8.2.2 Nginx部署

```nginx
# nginx.conf
server {
    listen 80;
    server_name workflow.example.com;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 9. 总结

### 9.1 核心特点

| 特点 | 说明 |
|------|------|
| **完整迁移** | 基于泛微E9工作流引擎设计，完整迁移所有核心功能 |
| **深度集成** | 工作流引擎与表单建模功能深度集成 |
| **Univer替换** | 使用Univer替换ExcelDesign，开源免费 |
| **JSON格式** | 采用JSON格式存储，兼容泛微E9实际格式 |
| **现代化技术栈** | Spring Boot 3.5.9 + React 18 + TypeScript |

### 9.2 后续工作

| 工作项 | 说明 | 优先级 |
|--------|------|--------|
| **后端模块开发** | 实现blade-workflow和blade-formmode模块 | 高 |
| **前端页面开发** | 实现WorkflowDesign、FormDesign、WorkflowRequest页面 | 高 |
| **Univer集成** | 完成Univer与表单设计器的集成 | 高 |
| **接口联调** | 前后端接口联调 | 中 |
| **功能测试** | 功能测试、性能测试、安全测试 | 中 |
| **部署上线** | 生产环境部署 | 低 |

---

**文档版本**: v1.0
**生成时间**: 2026-06-02
**适用项目**: SpringBlade + ant-design-pro
**目标**: 完整迁移泛微E9工作流引擎，实现与表单建模的深度集成
