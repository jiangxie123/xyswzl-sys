package com.mzy.xyswzlsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzy.xyswzlsys.entity.ItemComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 留言数据访问层
 */
@Mapper
public interface ItemCommentMapper extends BaseMapper<ItemComment> {
}
