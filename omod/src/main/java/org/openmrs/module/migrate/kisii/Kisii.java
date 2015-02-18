package org.openmrs.module.migrate.kisii;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate1;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 2/16/15.
 */
public class Kisii {
    String path;
    int counter = 0;
    HttpSession session;
    KenyaUiUtils kenyaUi;
    ObsService obsService = Context.getObsService();
    EncounterService encounterService = Context.getEncounterService();
    LocationService locationService = Context.getLocationService();
    FormService formService = Context.getFormService();
    ProviderService providerService = Context.getProviderService();
    ConceptService conceptService = Context.getConceptService();
    Patient patient = new Patient();
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
    ConvertStringToDate1 convertStringToDate = new ConvertStringToDate1();
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public Kisii(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetKisii readExcelSheetKakuma = new ReadExcelSheetKisii(path, session, kenyaUi);
        sheetData = readExcelSheetKakuma.readExcelSheet();

        checkForPatient(sheetData);
    }

    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            patient = null;
            List<Object> rowData = sheetData.get(i);
            String upn = rowData.get(0).toString();
//            String amrId = rowData.get(0).toString();

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);
//            List<Patient> patientListUsingAmrId = patientService.getPatients(null, amrId, null, true);

            if (patientListUsingUpn.size() > 0) {
                if (upn.isEmpty()) {
//                    patient = patientService.getPatient(patientListUsingAmrId.get(0).getPatientId());
                    enrollIntoHiv(patient, rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    enrollIntoHiv(patient, rowData);

                }
            }
        }
    }


    private void enrollIntoHiv(Patient patient, List<Object> rowData) throws ParseException {
        String[]encounterDate = String.valueOf(rowData.get(50)).replaceAll("\\s+", " ").split(" ");
        String[] enrollmentDate = String.valueOf(rowData.get(17)).replaceAll("\\s+", " ").split(" ");

        //enroll into hiv
        PatientProgram hivProgram = new PatientProgram();
        Encounter encounter = new Encounter();

        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        encounter.setLocation(defaultLocation);
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));

        if (rowData.get(50) != "") {
            encounter.setDateCreated(convertStringToDate.convert(encounterDate[0].toString()));
            encounter.setEncounterDatetime(convertStringToDate.convert(encounterDate[0].toString()));
            encounterService.saveEncounter(encounter);
        }


        hivProgram.setPatient(patient);
        hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
        if (rowData.get(17) != "") {

            hivProgram.setDateEnrolled(convertStringToDate.convert(enrollmentDate[0].toString()));
            workflowService.savePatientProgram(hivProgram);
        }
    }


}
