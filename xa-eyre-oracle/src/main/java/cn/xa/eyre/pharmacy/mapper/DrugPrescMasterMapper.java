package cn.xa.eyre.pharmacy.mapper;

import cn.xa.eyre.pharmacy.domain.DrugPrescMaster;

public interface DrugPrescMasterMapper {
    int insert(DrugPrescMaster record);

    int insertSelective(DrugPrescMaster record);

    DrugPrescMaster selectDrugByPrescNoAndPrescDate(Short prescNo, String prescDateStr);
}