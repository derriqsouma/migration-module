package org.openmrs.module.migrate.kisii;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;
import org.openmrs.module.migrate.maragua.ReadExcelSheetMaragua;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 2/16/15.
 */
public class Visits {
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

    public Visits(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetKisii readExcelSheetKisii = new ReadExcelSheetKisii(path, session, kenyaUi);
        sheetData = readExcelSheetKisii.readExcelSheet();

        checkForPatient(sheetData);
    }

    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            patient = null;
            List<Object> rowData = sheetData.get(i);
            String[] upn1 = String.valueOf(rowData.get(1)).replaceAll("\\.", "").split("E");
            String upn = upn1[0];

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);

            if (patientListUsingUpn.size() > 0) {
                if (upn.isEmpty()) {
                    visits(patient, rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    visits(patient, rowData);
                }
            }
        }
    }

    private void visits(Patient patient, List<Object> rowData) throws ParseException {

        String[] visitDate = String.valueOf(rowData.get(70)).replaceAll("\\s+", " ").split(" ");
        /*consultationEncounter*/
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setDateCreated(convertStringToDate.convert(visitDate[0].toString()));
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(visitDate[0].toString()));


        if (rowData.get(34) != "") {
            Obs nextAppointment = new Obs();
            String[] returnDate = String.valueOf(rowData.get(34)).replaceAll("\\s+", " ").split(" ");

            nextAppointment.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            nextAppointment.setPerson(patient);
            nextAppointment.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            nextAppointment.setValueDate(convertStringToDate.convert(returnDate[0].toString()));
            encounter.addObs(nextAppointment);
        }

        if (rowData.get(3) != "") {
            Obs weight = new Obs();//weight
            weight.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            weight.setPerson(patient);
            weight.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            weight.setValueNumeric(Double.parseDouble(rowData.get(3).toString()));
            encounter.addObs(weight);
        }

        if (rowData.get(5) != "") {
            Obs height = new Obs();//height
            height.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            height.setPerson(patient);
            height.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            height.setValueNumeric(Double.parseDouble(rowData.get(5).toString()));
            encounter.addObs(height);
        }

        if (rowData.get(11) != "") {
            Obs fpStatus = new Obs();//FP Status
            fpStatus.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            fpStatus.setPerson(patient);
            fpStatus.setConcept(conceptService.getConceptByUuid("160653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (rowData.get(11).toString().equalsIgnoreCase("1.0")) {
                fpStatus.setValueCoded(conceptService.getConceptByUuid("965AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else {
                fpStatus.setValueCoded(conceptService.getConceptByUuid("160652AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            encounter.addObs(fpStatus);
        }

        if (rowData.get(12) != "") {
            Obs tbStatus = new Obs();//TB Status
            tbStatus.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            tbStatus.setPerson(patient);
            tbStatus.setConcept(conceptService.getConceptByUuid("160653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (rowData.get(12).toString().equalsIgnoreCase("1.0")) {
                tbStatus.setValueCoded(conceptService.getConceptByUuid("1660AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else if (rowData.get(12).toString().equalsIgnoreCase("2.0")) {
                tbStatus.setValueCoded(conceptService.getConceptByUuid("142177AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            } else if (rowData.get(12).toString().equalsIgnoreCase("3.0")) {
                tbStatus.setValueCoded(conceptService.getConceptByUuid("1662AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            } else {
                tbStatus.setValueCoded(conceptService.getConceptByUuid("160737AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            encounter.addObs(tbStatus);
        }

        if (rowData.get(24) != "") {
            Obs cd4CountObs = new Obs();//cd4 Count
            cd4CountObs.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            cd4CountObs.setPerson(patient);
            cd4CountObs.setConcept(conceptService.getConceptByUuid("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            cd4CountObs.setValueNumeric(Double.valueOf(rowData.get(24).toString()));
            encounter.addObs(cd4CountObs);
        }

        if (rowData.get(36) != "") {
            Obs whoStage = new Obs();
            whoStage.setPerson(patient);
            whoStage.setLocation(defaultLocation);
            whoStage.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            whoStage.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
            checkForValueCodedForWhoStage(patient,whoStage, rowData);
            encounter.addObs(whoStage);
        }

        Obs pwpDisclosure = new Obs();
        pwpDisclosure.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        pwpDisclosure.setPerson(patient);
        pwpDisclosure.setConcept(conceptService.getConceptByUuid("159423AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(61).toString().equalsIgnoreCase("1.0")) {
            pwpDisclosure.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(61).toString().equalsIgnoreCase("2.0")){
            pwpDisclosure.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        encounter.addObs(pwpDisclosure);

        Obs pwpPartnerTested = new Obs();
        pwpPartnerTested.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        pwpPartnerTested.setPerson(patient);
        pwpPartnerTested.setConcept(conceptService.getConceptByUuid("161557AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(62).toString().equalsIgnoreCase("1.0")) {
            pwpPartnerTested.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(62).toString().equalsIgnoreCase("2.0")){
            pwpPartnerTested.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        encounter.addObs(pwpPartnerTested);

        Obs pwpCondoms = new Obs();
        pwpCondoms.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        pwpCondoms.setPerson(patient);
        pwpCondoms.setConcept(conceptService.getConceptByUuid("159777AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(63).toString().equalsIgnoreCase("1.0")) {
            pwpCondoms.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(63).toString().equalsIgnoreCase("2.0")){
            pwpCondoms.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        encounter.addObs(pwpCondoms);

        Obs pwpScreenedSTI = new Obs();
        pwpScreenedSTI.setObsDatetime(convertStringToDate.convert(visitDate[0].toString()));
        pwpScreenedSTI.setPerson(patient);
        pwpScreenedSTI.setConcept(conceptService.getConceptByUuid("161558AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(64).toString().equalsIgnoreCase("1.0")) {
            pwpScreenedSTI.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(64).toString().equalsIgnoreCase("2.0")){
            pwpScreenedSTI.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        encounter.addObs(pwpScreenedSTI);

        if (rowData.get(70) != "" || convertStringToDate.convert(visitDate[0]).before(new Date())) {
            encounterService.saveEncounter(encounter);
        }

        if (rowData.get(39) != ""){
            enrollIntoTBProgram(patient,rowData);
        }

    }

    private void enrollIntoTBProgram(Patient patient, List<Object> rowData) throws ParseException {
        PatientProgram tbProgram = new PatientProgram();
        tbProgram.setPatient(patient);
        tbProgram.setProgram(workflowService.getProgramByUuid("9f144a34-3a4a-44a9-8486-6b7af6cc64f6"));
        tbProgram.setDateEnrolled(convertStringToDate.convert(rowData.get(39).toString()));
        workflowService.savePatientProgram(tbProgram);

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("89994550-9939-40f3-afa6-173bce445c79"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("9d8498a4-372d-4dc4-a809-513a2434621e"));
        encounter.setLocation(defaultLocation);
        encounter.setDateCreated(convertStringToDate.convert(rowData.get(39).toString()));
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(39).toString()));

        Obs tbTreatmentStartDate = new Obs();
        tbTreatmentStartDate.setObsDatetime(convertStringToDate.convert(rowData.get(39).toString()));
        tbTreatmentStartDate.setConcept(conceptService.getConceptByUuid("1113AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        tbTreatmentStartDate.setValueDate(convertStringToDate.convert(rowData.get(39).toString()));
        encounter.addObs(tbTreatmentStartDate);

        encounterService.saveEncounter(encounter);


        if (rowData.get(40) != ""){
            Encounter tbTreatmentDiscontinuationEncounter = new Encounter();
            tbTreatmentDiscontinuationEncounter.setPatient(patient);
            tbTreatmentDiscontinuationEncounter.setForm(formService.getFormByUuid("4b296dd0-f6be-4007-9eb8-d0fd4e94fb3a"));
            tbTreatmentDiscontinuationEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("d3e3d723-7458-4b4e-8998-408e8a551a84"));
            tbTreatmentDiscontinuationEncounter.setDateCreated(convertStringToDate.convert(rowData.get(40).toString()));
            tbTreatmentDiscontinuationEncounter.setLocation(defaultLocation);
            tbTreatmentDiscontinuationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
            tbTreatmentDiscontinuationEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(40).toString()));

            Obs tbTreatmentDiscontinuationObs = new Obs();
            tbTreatmentDiscontinuationObs.setObsDatetime(convertStringToDate.convert(rowData.get(40).toString()));
            tbTreatmentDiscontinuationObs.setConcept(conceptService.getConceptByUuid("159431AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            tbTreatmentDiscontinuationObs.setValueDate(convertStringToDate.convert(rowData.get(40).toString()));
            tbTreatmentDiscontinuationEncounter.addObs(tbTreatmentDiscontinuationObs);

            encounterService.saveEncounter(tbTreatmentDiscontinuationEncounter);
        }
    }

    private void checkForValueCodedForWhoStage(Patient patient, Obs obs, List<Object> rowData) {

            int age = patient.getAge();
            String str =  rowData.get(36).toString();

            if (str.equals("1.0")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

            } else if (str.equals("2.0")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1221AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (str.equals("3.0")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1222AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (str.equals("4.0")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1223AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1207AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else {

                obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

    }


}
