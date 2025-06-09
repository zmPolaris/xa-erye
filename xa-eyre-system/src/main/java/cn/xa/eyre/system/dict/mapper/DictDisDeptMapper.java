package cn.xa.eyre.system.dict.mapper;

import cn.xa.eyre.system.dict.domain.DictDisDept;

import java.util.List;

public interface DictDisDeptMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(DictDisDept record);

    int insertSelective(DictDisDept record);

    DictDisDept selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(DictDisDept record);

    int updateByPrimaryKey(DictDisDept record);

    List<DictDisDept> selectAll();
}