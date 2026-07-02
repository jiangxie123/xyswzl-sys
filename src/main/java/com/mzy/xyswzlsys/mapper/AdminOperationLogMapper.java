package com.mzy.xyswzlsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzy.xyswzlsys.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员操作日志数据访问层
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {
}
