package org.openmrs.module.migrate;

import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derric on 7/14/14.
 */
public class Moh361B {
    String path;
    HttpSession session;
    KenyaUiUtils kenyaUi;
    public Moh361B(String path,HttpSession session, KenyaUiUtils kenyaUi){
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void initMoh361B() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheet readExcelSheet = new ReadExcelSheet();
        sheetData = readExcelSheet.readExcelSheet(path);

        checkForPatient(sheetData);

    }
    private void checkForPatient(List<List<Object>> sheetData) {
        PatientService patientService = Context.getPatientService();

        for (int i = 1; i < sheetData.size(); i++) {
            List<Object> rowData = sheetData.get(i);
            String upn = rowData.get(2).toString().replaceAll("[^\\d]", "");
            String amrId = rowData.get(5).toString();

            List<Patient> patientList = patientService.getPatients(null, upn, null, true);
            List<Patient> patientList1 = patientService.getPatients(null, amrId, null, true);

            Patient patient = new Patient();

            if (patientList.size() > 0 || patientList1.size() > 0) {
                if (upn.isEmpty()){
                    patient = patientService.getPatient(patientList.get(0).getPatientId());

                }else {
                    patient = patientService.getPatient(patientList.get(0).getPatientId());

                }
            }
        }
    }
}
