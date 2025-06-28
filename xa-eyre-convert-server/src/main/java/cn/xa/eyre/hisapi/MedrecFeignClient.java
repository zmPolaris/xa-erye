package cn.xa.eyre.hisapi;


import cn.xa.eyre.common.constant.Constants;
import cn.xa.eyre.common.core.domain.R;
import cn.xa.eyre.medrec.domain.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
@FeignClient(name = "medrecService", url = Constants.HIS_URL)
public interface MedrecFeignClient {

    @GetMapping("/medrec/getPatMasterIndex/{patientId}")
    public R<PatMasterIndex> getPatMasterIndex(@PathVariable("patientId") String patientId);

    @GetMapping("/medrec/getPatMasterIndexList")
    public R<List<PatMasterIndex>> getPatMasterIndexList(@RequestParam("num") Integer num);

    @GetMapping("/medrec/getDiagnosticCategory")
    public R<DiagnosticCategory> getDiagnosticCategory(@RequestBody DiagnosticCategoryKey diagnosticCategoryKey);

    @GetMapping("/medrec/getDiagnosis")
    public R<Diagnosis> getDiagnosis(@RequestBody DiagnosisKey diagnosisKey);
}
