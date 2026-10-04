# 数据模型和系统设计

- [完整 ER 矢量 PDF](complete-er.pdf)：A3 landscape，420×297 mm，同一页完整列出 13 实体、151 字段、34 外键。
- [可编辑原生 Visio](RMS_Presentation_Models.vsdx)：一页；字段为原生文字，实体为表框组合，34 条 1D 关系线两端粘连到实体。
- [完整 ER SVG](complete-er.svg)及 [预览](complete-er-preview.png)。
- [151 字段数据字典](data-dictionary.csv)：类型、含义、主外键、可空性、默认值、域与修改条件。
- [34 外键](foreign-keys.csv)：复合列、两端基数和标识关系。
- [状态转换表](state-transitions.md)、[需求状态](requirement-state.pdf)、[变更状态](change-request-state.pdf)。
- [系统架构](architecture.pdf)、[多智能体协作流程](multi-agent-workflow.pdf)。
- [完整接口定义](openapi.yaml)。

PK 表示主键，FK 表示外键，UK 编号表示**同一实体内的一组唯一键**；相同编号的多列共同唯一。`?` 表示可空，G 表示由数据库生成的约束字段。实体框中的字段名与原 151 字段逐一对应，不使用重复引用框扩充实体数。

外键是真实的结构约束；权限、自我评审、连续版本号和依赖环还需要业务服务保证。审计 `targetType/targetId` 只是逻辑目标定位，没有伪造到所有实体的外键。当前内容、正式版本和提交快照有意保存副本，副本对应不同时间和业务含义，不能宣称模型完全没有冗余。

当前负责人、标签和需求关系的历史通过审计追踪，正式版本只保存八项正式内容。批准变更与应用变更独立，应用新版本后需求回到 APPROVED，再重新确认实现和验证。
