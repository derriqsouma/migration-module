package org.openmrs.module.migrate.maragua;

import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;
import org.openmrs.module.migrate.ConvertStringToDate1;
import org.openmrs.module.migrate.kakuma.ReadExcelSheetKakuma;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 2/16/15.
 */
public class MaraguaVisits {
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
    ConvertStringToDate convertStringToDate = new ConvertStringToDate();
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public MaraguaVisits(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetMaragua readExcelSheetMaragua = new ReadExcelSheetMaragua(path, session, kenyaUi);
        sheetData = readExcelSheetMaragua.readExcelSheet();

        checkForPatient(sheetData);
    }

    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            patient = null;
            List<Object> rowData = sheetData.get(i);
            String[] upn1 = String.valueOf(rowData.get(3)).split("\\.");
            String upn = upn1[0];
//            String amrId = rowData.get(0).toString();

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);
//            List<Patient> patientListUsingAmrId = patientService.getPatients(null, amrId, null, true);

            if (patientListUsingUpn.size() > 0) {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    visits(patient, rowData);

            }
        }
    }

    private void visits(Patient patient, List<Object> rowData) throws ParseException {

        String[] visitDate = String.valueOf(rowData.get(9)).replaceAll("\\s+", " ").split(" ");
        /*consultationEncounter*/
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setDateCreated(convertStringToDate.convert(visitDate[0].toString()));
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(visitDate[0].toString()));


        if (rowData.get(10) != "") {
            Obs nextAppointment = new Obs();
            String[] returnDate = String.valueOf(rowData.get(10)).replaceAll("\\s+", " ").split(" ");

            nextAppointment.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            nextAppointment.setPerson(patient);
            nextAppointment.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            nextAppointment.setValueDate(convertStringToDate.convert(returnDate[0].toString()));
            encounter.addObs(nextAppointment);
        }

        Obs weight = new Obs();//weight
        weight.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        weight.setPerson(patient);
        weight.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(22) != "") {
            weight.setValueNumeric(Double.parseDouble(rowData.get(22).toString()));
        }
        encounter.addObs(weight);

        Obs height = new Obs();//height
        height.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        height.setPerson(patient);
        height.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(23) != "") {
            height.setValueNumeric(Double.parseDouble(rowData.get(23).toString()));
        }
        encounter.addObs(height);

        if (convertStringToDate.convert(visitDate[0]).before(new Date())) {
            encounterService.saveEncounter(encounter);
        }

    }


}
