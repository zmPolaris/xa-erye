package cn.xa.eyre.exam.controller;

import cn.xa.eyre.common.core.domain.AjaxResult;
import cn.xa.eyre.exam.mapper.ExamReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamReportMapper examReportMapper;

    @GetMapping("/getExamReport/{examNo}")
    public AjaxResult getExamReport(@PathVariable("examNo") String examNo){
        return AjaxResult.success("接口调用成功", examReportMapper.selectByPrimaryKey(examNo));
    }
}
