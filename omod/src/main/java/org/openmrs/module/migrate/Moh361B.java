package org.openmrs.module.migrate;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
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

        System.out.println("***\n"+sheetData.size()+"\n"+sheetData);
        kenyaUi.notifyError(session, "testing");
    }
    private void checkForPatient() {
        PatientService patientService = Context.getPatientService();
        Patient patient = new Patient();
        PatientIdentifierType upn = patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a");

        List<Patient> allPatients = patientService.getAllPatients();
//        for(Patient p: allPatients){
//            if(p.getPatientIdentifier(upn.getName()).getIdentifier().equals("1579700001")){
//                throw new APIException("Patient " + p.getNames()  +" already exists");
//            }
//        }

        List<Patient> patientList = patientService.getPatients(null, "1596800043", null, true);
        if(patientList.size() > 0) {
            throw new APIException("Patient " + patientList.get(0).getFamilyName()  +" already exists");

        }

    }
}
