package com.mzy.xyswzlsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzy.xyswzlsys.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 * 继承 BaseMapper<SysUser> 后，自动获得 CRUD 方法：
 *   insert(), deleteById(), updateById(), selectById(), selectList() 等
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
