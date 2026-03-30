-- ============================================================
-- Member 用户表改造脚本
-- 版本: V2.0
-- 日期: 2026-02-13
-- 说明: Member 用户表改造，与 PMS 保持一致的结构
-- ============================================================

USE ingot_member;

-- ============================================================
-- Step 1: 添加新字段
-- ============================================================
ALTER TABLE member_user
  -- 凭证管理
  ADD COLUMN must_change_pwd TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改密码（0-否 1-是）' AFTER password,
  ADD COLUMN password_changed_at DATETIME DEFAULT NULL COMMENT '密码最后修改时间' AFTER must_change_pwd,
  
  -- 状态控制
  ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用 1-启用）' AFTER avatar,
  ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定（冗余字段，详情见 account_lock_state）' AFTER enabled,
  
  -- 登录审计
  ADD COLUMN last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER locked,
  ADD COLUMN last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP' AFTER last_login_at,
  
  -- 并发控制
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER last_login_ip;

-- ============================================================
-- Step 2: 数据迁移
-- ============================================================
UPDATE member_user SET 
  must_change_pwd = COALESCE(init_pwd, 1),
  enabled = CASE WHEN status = '0' THEN 1 ELSE 0 END,
  locked = CASE WHEN status = '9' THEN 1 ELSE 0 END,
  password_changed_at = COALESCE(updated_at, created_at);

-- ============================================================
-- Step 3: 添加索引
-- ============================================================
ALTER TABLE member_user
  ADD INDEX idx_enabled (enabled) USING BTREE COMMENT '启用状态索引',
  ADD INDEX idx_locked (locked) USING BTREE COMMENT '锁定状态索引',
  ADD INDEX idx_last_login (last_login_at) USING BTREE COMMENT '最后登录时间索引';

-- ============================================================
-- Step 4: 调整唯一索引
-- ============================================================
ALTER TABLE member_user DROP INDEX idx_username;
ALTER TABLE member_user 
  ADD UNIQUE KEY uk_username (username, (COALESCE(deleted_at, 0))) COMMENT '用户名全局唯一（软删除友好）';

-- ============================================================
-- Step 5: 标记旧字段
-- ============================================================
ALTER TABLE member_user 
  MODIFY COLUMN init_pwd TINYINT(1) DEFAULT 1 COMMENT '【废弃 2026-05-13】请使用 must_change_pwd',
  MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '【废弃 2026-05-13】请使用 enabled 和 locked';

-- ============================================================
-- Step 6: 创建 Member 的 account_lock_state 表
-- ============================================================
CREATE TABLE IF NOT EXISTS account_lock_state (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  
  locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  lock_type VARCHAR(20) DEFAULT NULL COMMENT '锁定类型',
  lock_reason_code VARCHAR(50) DEFAULT NULL COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL COMMENT '锁定原因详情',
  
  locked_at DATETIME DEFAULT NULL COMMENT '锁定时间',
  locked_until DATETIME DEFAULT NULL COMMENT '锁定到期时间',
  
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  
  failed_login_count INT NOT NULL DEFAULT 0 COMMENT '连续失败次数',
  last_failed_at DATETIME DEFAULT NULL COMMENT '最后失败时间',
  
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_locked (locked, locked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';

-- ============================================================
-- 初始化 Member 锁定状态
-- ============================================================
INSERT INTO account_lock_state (user_id, locked, locked_at, lock_type, lock_reason_code)
SELECT 
  id,
  CASE WHEN status = '9' THEN 1 ELSE 0 END AS locked,
  CASE WHEN status = '9' THEN COALESCE(updated_at, created_at) ELSE NULL END AS locked_at,
  CASE WHEN status = '9' THEN 'MANUAL' ELSE NULL END AS lock_type,
  CASE WHEN status = '9' THEN 'LEGACY_DISABLED' ELSE NULL END AS lock_reason_code
FROM member_user
WHERE NOT EXISTS (
    SELECT 1 FROM account_lock_state WHERE account_lock_state.user_id = member_user.id
);

-- ============================================================
-- Step 7: 创建 Member 的 account_security_event 表
-- ============================================================
CREATE TABLE IF NOT EXISTS account_security_event (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  user_type VARCHAR(20) NOT NULL COMMENT '用户类型（PLATFORM/APP）',
  
  event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
  event_category VARCHAR(20) NOT NULL COMMENT '事件分类',
  
  reason_code VARCHAR(50) DEFAULT NULL COMMENT '原因代码',
  reason_detail VARCHAR(500) DEFAULT NULL COMMENT '详细描述',
  result VARCHAR(20) DEFAULT NULL COMMENT '结果',
  
  source VARCHAR(50) DEFAULT NULL COMMENT '来源',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  user_agent VARCHAR(500) DEFAULT NULL COMMENT '客户端信息',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
  
  extra_data JSON DEFAULT NULL COMMENT '扩展数据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  
  PRIMARY KEY (id),
  KEY idx_user_event (user_id, user_type, event_type, created_at),
  KEY idx_created_at (created_at),
  KEY idx_event_type (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表';

-- ============================================================
-- Step 8: 创建 Member 的 password_history 表（如果不存在）
-- ============================================================
CREATE TABLE IF NOT EXISTS password_history (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
  sequence_number INT NOT NULL COMMENT '序号（环形缓冲，从1开始）',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sequence (user_id, sequence_number),
  KEY idx_user_id (user_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）';

-- ============================================================
-- Step 9: 创建 Member 的 password_expiration 表（如果不存在）
-- ============================================================
CREATE TABLE IF NOT EXISTS password_expiration (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  
  last_changed_at DATETIME NOT NULL COMMENT '最后修改密码时间',
  expires_at DATETIME NOT NULL COMMENT '密码过期时间',
  force_change TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
  grace_login_remaining INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
  next_warning_at DATETIME DEFAULT NULL COMMENT '下次提醒时间',
  
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_expires_at (expires_at),
  KEY idx_next_warning_at (next_warning_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息';

-- ============================================================
-- 完成
-- ============================================================
SELECT '002_upgrade_member_user.sql 执行完成' AS message,
       (SELECT COUNT(*) FROM member_user) AS '会员用户数',
       (SELECT COUNT(*) FROM account_lock_state) AS '已初始化锁定状态数';
