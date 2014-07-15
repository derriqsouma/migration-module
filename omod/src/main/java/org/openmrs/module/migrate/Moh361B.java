package org.openmrs.module.migrate;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 7/14/14.
 */
public class Moh361B {
    String path;
    HttpSession session;
    KenyaUiUtils kenyaUi;
    ObsService obsService = Context.getObsService();
    EncounterService encounterService = Context.getEncounterService();
    LocationService locationService = Context.getLocationService();
    FormService formService = Context.getFormService();
    ProviderService providerService = Context.getProviderService();
    ConceptService conceptService = Context.getConceptService();
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
    PatientService patientService = Context.getPatientService();

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
    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            List<Object> rowData = sheetData.get(i);
            String upn = rowData.get(2).toString().replaceAll("[^\\d]", "");
            String amrId = rowData.get(5).toString();

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);
            List<Patient> patientListUsingAmrId = patientService.getPatients(null, amrId, null, true);

            Patient patient = new Patient();

            if (patientListUsingUpn.size() > 0 || patientListUsingAmrId.size() > 0) {
                if (upn.isEmpty()){
                    patient = patientService.getPatient(patientListUsingAmrId.get(0).getPatientId());

                }else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());

                }
            }
            
            savePatientObsAtArvStart(patient,rowData);
        }
    }

    private void savePatientObsAtArvStart(Patient patient, List<Object> rowData) throws ParseException {

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
       /* encounter.setEncounterDatetime(convertToDate(rowData.get().toString()));
        encounter.setEncounterDatetime(convertToDate(rowData.get().toString()));*/

    }

    private void checkForValueCodedForWhoStage(Obs obs, ConceptService conceptService, String whoStageAnswer, Double age){

        if (whoStageAnswer != "") {
            String[] whoStage = whoStageAnswer.split("\\n");

            if (whoStage[0].equals("WHO Stage 1")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

            } else if (whoStage[0].equals("WHO Stage 2")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1221AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("WHO Stage 3")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1222AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("WHO Stage 4")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1223AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1207AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else {

                obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
    }

    private Date convertToDate(String dateInString) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        Date date = null;
        if (dateInString != "") {
            if (dateInString.contains("/")) {

                date = formatter.parse(dateInString);
            }

        }
        return date;
    }

}
