package org.openmrs.module.migrate.kakuma;

import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate1;
import org.openmrs.module.migrate.ReadExcelSheet;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 2/16/15.
 */
public class KakumaVisits {
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

    public KakumaVisits(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetKakuma readExcelSheetKakuma = new ReadExcelSheetKakuma(path, session, kenyaUi);
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
                    visits(patient,rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    visits(patient, rowData);

                }
            }
        }
    }

    private void visits(Patient patient, List<Object> rowData) throws ParseException {

        /*consultationEncounter*/
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(new Date());


        if (rowData.get(2) != "") {
            Obs nextAppointment = new Obs();//Last return to clinic obs
            String[] returnDate = String.valueOf(rowData.get(2)).replaceAll("\\s+", " ").split(" ");

            nextAppointment.setObsDatetime(convertStringToDate.convert(returnDate[0].toString()));
            nextAppointment.setPerson(patient);
            nextAppointment.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            nextAppointment.setValueDate(convertStringToDate.convert(returnDate[0].toString()));
            encounter.addObs(nextAppointment);
        }

        if (rowData.get(7) != "" && rowData.get(8) != "") {
            Obs cd4CountObs = new Obs();//CD4 count Obs
            String[] date = String.valueOf(rowData.get(7)).replaceAll("\\s+", " ").split(" ");

            cd4CountObs.setObsDatetime(convertStringToDate.convert(date[0].toString()));
            cd4CountObs.setPerson(patient);
            cd4CountObs.setConcept(conceptService.getConceptByUuid("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            cd4CountObs.setValueNumeric(Double.valueOf(rowData.get(8).toString()));
            encounter.addObs(cd4CountObs);

            Obs cd4CountDateObs = new Obs();//CD4 count Obs
            cd4CountDateObs.setObsDatetime(convertStringToDate.convert(date[0].toString()));
            cd4CountDateObs.setPerson(patient);
            cd4CountDateObs.setConcept(conceptService.getConceptByUuid("159376AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            cd4CountDateObs.setValueDate(convertStringToDate.convert(date[0].toString()));
            encounter.addObs(cd4CountDateObs);
        }

       /* if (rowData.get(25) != "" && rowData.get(26) != "") {
            Obs viralLoadObs = new Obs();
            String[] date = String.valueOf(rowData.get(25)).replaceAll("\\s+", " ").split(" ");

            viralLoadObs.setObsDatetime(convertStringToDate.convert(date[0].toString()));
            viralLoadObs.setPerson(patient);
            viralLoadObs.setConcept(conceptService.getConceptByUuid("856AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (rowData.get(26).toString() == "\\d+" ) {
                viralLoadObs.setValueNumeric(Double.valueOf(rowData.get(8).toString()));
            }
            encounter.addObs(viralLoadObs);

        }*/

        encounterService.saveEncounter(encounter);

    }


}
