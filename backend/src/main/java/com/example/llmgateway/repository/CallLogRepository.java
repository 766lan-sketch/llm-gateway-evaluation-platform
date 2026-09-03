package com.example.llmgateway.repository;

import com.example.llmgateway.entity.CallLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository 专门负责操作数据库。
 * JpaRepository 已经帮我们写好了保存、查询和删除等常用操作。
 */
public interface CallLogRepository extends JpaRepository<CallLog, Long> {
    // Spring 会根据方法名自动生成 SQL：按时间倒序查询最近 20 条记录。
    List<CallLog> findTop20ByOrderByCreatedAtDesc();
}
