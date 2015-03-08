package org.openmrs.module.migrate.kisii;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 3/3/15.
 */
public class FamilyInfo {
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

    public FamilyInfo(String path, HttpSession session, KenyaUiUtils kenyaUi) {
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
            String[] upn1 = String.valueOf(rowData.get(1)).replaceAll("\\.", "").split("E");
            String upn = upn1[0];

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);

            if (patientListUsingUpn.size() > 0) {
                if (upn.isEmpty()) {
                    saveFamilyInfo(patient, rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    saveFamilyInfo(patient, rowData);
                }
            }
        }
    }

    private void saveFamilyInfo(Patient patient, List<Object> rowData){
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("7efa0ee0-6617-4cd7-8310-9f95dfee7a82"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de1f9d67-b73e-4e1b-90d0-036166fc6995"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setDateCreated(new Date());
        encounter.setEncounterDatetime(new Date());

        Obs groupObs = new Obs();
        groupObs.setConcept(conceptService.getConceptByUuid("160593AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        groupObs.setPerson(patient);
        groupObs.setObsDatetime(new Date());

        Obs memberName = new Obs();
        memberName.setConcept(conceptService.getConceptByUuid("160750AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        memberName.setValueText(rowData.get(2).toString());
        groupObs.addGroupMember(memberName);

        Obs memberAge = new Obs();
        memberAge.setConcept(conceptService.getConceptByUuid("160617AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(3) != "") {
            memberAge.setValueNumeric(Double.valueOf(rowData.get(3).toString()));
            groupObs.addGroupMember(memberAge);
        }

        Obs memberAgeIn = new Obs();
        memberAgeIn.setConcept(conceptService.getConceptByUuid("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        memberAgeIn.setValueCoded(conceptService.getConceptByUuid("1734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        groupObs.addGroupMember(memberAgeIn);

        Obs memberRelation = new Obs();
        memberRelation.setConcept(conceptService.getConceptByUuid("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(4) != "") {
            getRelation(memberRelation, rowData);
            groupObs.addGroupMember(memberRelation);
        }

        Obs memberInHiv = new Obs();
        memberInHiv.setConcept(conceptService.getConceptByUuid("1729AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(5) != "") {
            getInHiv(memberInHiv, rowData);
            groupObs.addGroupMember(memberInHiv);
        }

        Obs memberInCare = new Obs();
        memberInCare.setConcept(conceptService.getConceptByUuid("159811AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(6) != "") {
            getInCare(memberInCare, rowData);
            groupObs.addGroupMember(memberInCare);
        }

        Obs memberCccNumber = new Obs();
        memberCccNumber.setConcept(conceptService.getConceptByUuid("160752AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(7) != "") {
            memberCccNumber.setValueText(rowData.get(7).toString());
            groupObs.addGroupMember(memberCccNumber);
        }

        encounter.addObs(groupObs);
        encounterService.saveEncounter(encounter);

    }

    private void getInCare(Obs obs, List<Object> rowData) {
        if (rowData.get(6).toString().equalsIgnoreCase("1.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(6).toString().equalsIgnoreCase("2.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else {
            obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
    }

    private void getInHiv(Obs obs, List<Object> rowData) {
        if (rowData.get(5).toString().equalsIgnoreCase("1.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(5).toString().equalsIgnoreCase("2.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (rowData.get(5).toString().equalsIgnoreCase("3.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
    }

    private void getRelation(Obs obs, List<Object> rowData) {
        String relation = rowData.get(4).toString();
        if (relation.equalsIgnoreCase("1.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("971AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("2.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("970AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("3.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1528AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("4.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("1528AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("5.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("972AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("6.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("972AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("7.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("8.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("9.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("10.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("975AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("11.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("974AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("12.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (relation.equalsIgnoreCase("13.0")){
            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
    }

}
