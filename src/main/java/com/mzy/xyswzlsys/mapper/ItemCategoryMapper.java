package com.mzy.xyswzlsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzy.xyswzlsys.entity.ItemCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物品分类 Mapper
 * 继承 BaseMapper，获得基础 CRUD 方法（selectById、selectList、insert、updateById、deleteById 等）
 */
@Mapper
public interface ItemCategoryMapper extends BaseMapper<ItemCategory> {
}
