package org.openmrs.module.migrate.page.controller;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by derric on 6/19/14.
 */

@AppPage(EmrConstants.APP_CLINICIAN)
public class MigrationHomePageController {

    @RequestMapping()
    public void controller(UiUtils ui,
                           @RequestParam(value = "file", required = false) String file,
                           PageModel model) throws Exception {
        String error;
        if (file != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361A/" + file;

            model.addAttribute("file", file);

            readExcelSheet(path);
        } else {
            model.addAttribute("file", "NO FILE CHOSEN");

        }

    }

    private void readExcelSheet(String path) throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        FileInputStream file = new FileInputStream(new File(path));

        HSSFWorkbook workbook = new HSSFWorkbook(file);

        HSSFSheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {

            Row row = rowIterator.next();
            if (row.getRowNum() == 0 || row.getRowNum() == 1 || row.getRowNum() == 2) {
                continue;
            }

            int expectedColumns = 24;
            List<Object> rowData = new ArrayList();

            for (int i = 0; i < expectedColumns; i++) {
                Cell cell = row.getCell(i);

                if (cell == null) {
                    rowData.add("");
                    continue;
                }

                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_BOOLEAN:
                        rowData.add(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        rowData.add(cell.getNumericCellValue());
                        break;
                    case Cell.CELL_TYPE_STRING:
                        rowData.add(cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_BLANK:
                        rowData.add(cell.getStringCellValue());
                        break;
                }
            }
            sheetData.add(rowData);
        }

        file.close();

        processExcelData(sheetData);
    }

    private void processExcelData(List<List<Object>> sheetData) throws ParseException {

        for (int i = 0; i < sheetData.size(); i++) {

            List<Object> rowData = sheetData.get(i);
            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = (String) rowData.get(9);
            Date dob = convertToDate((String) rowData.get(7));
            fullNames = String.valueOf(rowData.get(6)).replaceAll("\\s+", " ").split(" ");

            if (fullNames.length == 4) {
                fName = fullNames[0];
                mName = fullNames[1] + " " + fullNames[2];
                lName = fullNames[3];
            }

            if (fullNames.length == 3) {
                fName = fullNames[0];
                mName = fullNames[1];
                lName = fullNames[2];
            }
            if (fullNames.length == 2) {
                fName = fullNames[0];
                lName = fullNames[1];
            }

            Patient patient = new Patient();


            PatientService patientService = Context.getPatientService();
            LocationService locationService = Context.getLocationService();

            PersonName personName = new PersonName();
            personName.setFamilyName(lName);
            personName.setGivenName(fName);
            personName.setMiddleName(mName);

            patient.addName(personName);
            patient.setGender(gender);
            patient.setBirthdate(dob);

            PatientIdentifier openmrsId = new PatientIdentifier();
            //generating open mrs id
            PatientIdentifierType openmrsIdType = patientService.getPatientIdentifierTypeByUuid("dfacd928-0370-4315-99d7-6ec1c9f7ae76");
            String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "Migration");
            openmrsId.setIdentifierType(openmrsIdType);
            openmrsId.setDateCreated(new Date());
            openmrsId.setLocation(locationService.getDefaultLocation());
            openmrsId.setIdentifier(generated);
            openmrsId.setVoided(false);

            PatientIdentifier amrId = new PatientIdentifier();
            amrId.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("8d79403a-c2cc-11de-8d13-0010c6dffd0f"));
            amrId.setDateCreated(new Date());
            amrId.setLocation(locationService.getDefaultLocation());
            amrId.setIdentifier((String) rowData.get(5));
            amrId.setVoided(false);

            PatientIdentifier upn = null;
            if (rowData.get(4).toString() != "") {
                upn = new PatientIdentifier();
                upn.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a"));
                upn.setDateCreated(new Date());
                upn.setLocation(locationService.getDefaultLocation());
                upn.setIdentifier(rowData.get(4).toString().replaceAll("[^\\d]", ""));
                upn.setVoided(false);
                upn.setPreferred(true);

                patient.addIdentifiers(Arrays.asList(upn, openmrsId, amrId));
                patientService.savePatient(patient);//saving the patient
                savePatientObs(patient, rowData);
            }
            else{
                amrId.setPreferred(true);

                patient.addIdentifiers(Arrays.asList(openmrsId, amrId));
                patientService.savePatient(patient);//saving the patient
                savePatientObs(patient, rowData);
            }
        }
    }

    private void savePatientObs(Patient patient, List<Object> rowData) throws ParseException {

        Provider provider = new Provider();
        Concept concept = new Concept();

        ObsService obsService = Context.getObsService();
        EncounterService encounterService = Context.getEncounterService();
        LocationService locationService = Context.getLocationService();
        FormService formService = Context.getFormService();
        ProviderService providerService = Context.getProviderService();
        ConceptService conceptService = Context.getConceptService();
        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();

        /*Patient Provider*/


        /*Patient Program*/
        if (rowData.get(4).toString() != "") {
            PatientProgram hivProgram = new PatientProgram();
            hivProgram.setPatient(patient);//enroll in HIV Program
            hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
            hivProgram.setDateEnrolled(getDateTime(rowData.get(2).toString() + " 00:00:00"));
            workflowService.savePatientProgram(hivProgram);
        }

        String enrolledInTb = rowData.get(14).toString();//enroll in HIV Program
        PatientProgram tbProgram = new PatientProgram();
        if (enrolledInTb != "")
        {
            tbProgram.setPatient(patient);
            tbProgram.setProgram(workflowService.getProgramByUuid("9f144a34-3a4a-44a9-8486-6b7af6cc64f6"));
            enrollInToTBProgram(patient, tbProgram, enrolledInTb);
        }

        /*enrollmentEncounter*/
        Encounter enrollmentEncounter = new Encounter();
        enrollmentEncounter.setPatient(patient);
        enrollmentEncounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        enrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        enrollmentEncounter.setLocation(locationService.getLocationByUuid("f2904f27-f35f-41aa-aad1-eb7325cf72f6"));
        enrollmentEncounter.setDateCreated(new Date());
        enrollmentEncounter.setEncounterDatetime(getDateTime(rowData.get(3).toString() + " 00:00:00"));

        /*Patient Obs during enrollment*/
        Obs entryPointObs = new Obs();//entry point
        entryPointObs.setObsDatetime(getDateTime(rowData.get(21).toString() + " 00:00:00"));
        entryPointObs.setPerson(patient);
        entryPointObs.setConcept(conceptService.getConceptByUuid("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String entryPointAnswer = rowData.get(10).toString();
        checkForValueCodedForEntryPoint(entryPointObs, obsService, conceptService, entryPointAnswer);
        enrollmentEncounter.addObs(entryPointObs);

        Obs transferInObs = new Obs();//transfer in
        transferInObs.setObsDatetime(getDateTime(rowData.get(21).toString() + " 00:00:00"));
        transferInObs.setPerson(patient);
        transferInObs.setConcept(conceptService.getConceptByUuid("160563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String isTransferAnswer = rowData.get(1).toString();
        checkForValueCodedForIsTransferIn(transferInObs, obsService, conceptService, isTransferAnswer);
        enrollmentEncounter.addObs(transferInObs);

        Obs dateConfirmedHivObs = new Obs();//Date confirmed HIV+
        dateConfirmedHivObs.setObsDatetime(getDateTime(rowData.get(21).toString() + " 00:00:00"));
        dateConfirmedHivObs.setPerson(patient);
        dateConfirmedHivObs.setConcept(conceptService.getConceptByUuid("160554AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateConfirmedHivObs.setValueDate(convertToDate(rowData.get(11).toString()));
        enrollmentEncounter.addObs(dateConfirmedHivObs);

        encounterService.saveEncounter(enrollmentEncounter);//saving the enrollmentEncounter


         /*consultationEncounter*/
        Encounter consultationEncounter = new Encounter();
        consultationEncounter.setPatient(patient);
        consultationEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        consultationEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        consultationEncounter.setLocation(locationService.getLocationByUuid("f2904f27-f35f-41aa-aad1-eb7325cf72f6"));
        consultationEncounter.setDateCreated(new Date());
        consultationEncounter.setEncounterDatetime(getDateTime(rowData.get(3).toString() + " 00:00:00"));

        Obs dateArtStartedObs = new Obs();//date art started
        dateArtStartedObs.setObsDatetime(new Date());
        dateArtStartedObs.setPerson(patient);
        dateArtStartedObs.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateArtStartedObs.setValueDate(convertToDate(rowData.get(20).toString()));
        consultationEncounter.addObs(dateArtStartedObs);

        Obs dateEligibleForArvObs = new Obs();//date eligible for ARVs started
        dateEligibleForArvObs.setObsDatetime(new Date());
        dateEligibleForArvObs.setPerson(patient);
        dateEligibleForArvObs.setConcept(conceptService.getConceptByUuid("162227AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateEligibleForArvObs.setValueDate(convertToDate(rowData.get(18).toString()));
        consultationEncounter.addObs(dateEligibleForArvObs);

        Obs whoStageObs = new Obs();//World Health Organization HIV stage
        whoStageObs.setPerson(patient);
        whoStageObs.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String whoStageAnswer = rowData.get(17).toString();
        checkForValueCodedForWhoStage(whoStageObs, obsService, conceptService, whoStageAnswer);
        consultationEncounter.addObs(whoStageObs);

        Obs groupObs = new Obs();/*CTX Obs*/
        groupObs.setConcept(conceptService.getConceptByUuid("1442AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));        
        groupObs.setPerson(patient);
        groupObs.setObsDatetime(new Date());

        Obs ctxObs = new Obs();
        ctxObs.setConcept(conceptService.getConceptByUuid("1282AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        ctxObs.setValueCoded(conceptService.getConceptByUuid("105281AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        groupObs.addGroupMember(ctxObs);

        calculateDuration(groupObs, conceptService, rowData);
        consultationEncounter.addObs(groupObs);

        encounterService.saveEncounter(consultationEncounter);//saving the consultationEncounter
    }

    private void enrollInToTBProgram(Patient patient, PatientProgram tbProgram, String enrolledInTb) throws ParseException {
        String[] tbDates = enrolledInTb.toString().split("\\n");
        EncounterService encounterService = Context.getEncounterService();
        FormService formService = Context.getFormService();
        LocationService locationService = Context.getLocationService();
        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();

        Encounter tbEnrollmentEncounter = new Encounter();
        tbEnrollmentEncounter.setPatient(patient);
        tbEnrollmentEncounter.setForm(formService.getFormByUuid("89994550-9939-40f3-afa6-173bce445c79"));
        tbEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("9d8498a4-372d-4dc4-a809-513a2434621e"));
        tbEnrollmentEncounter.setLocation(locationService.getLocationByUuid("f2904f27-f35f-41aa-aad1-eb7325cf72f6"));
        tbEnrollmentEncounter.setDateCreated(new Date());

        String[] dates1 = tbDates[0].trim().split("-");
        tbProgram.setDateEnrolled(convertToDate(dates1[0]));
        tbEnrollmentEncounter.setEncounterDatetime(convertToDate(dates1[0]));
        workflowService.savePatientProgram(tbProgram);
        encounterService.saveEncounter(tbEnrollmentEncounter);

        Obs tbObs = new Obs();
        if (tbDates.length == 1) {
            String[] dates = tbDates[0].trim().split("-");


            if (dates.length == 2) {
                String startDate = dates[0];
                String stopDate = dates[1];
            }

        } else {

            for (int j = 0; j < tbDates.length; j++) {
                String[] dates = tbDates[j].trim().split("-");


                if (dates.length == 2) {
                    String startDate = dates[0];
                    String stopDate = dates[1];
                }
            }

        }

    }

    private void calculateDuration(Obs groupObs, ConceptService conceptService, List<Object> rowData) throws ParseException {

        if (rowData.get(12) != "") {
            String[] ctxDates = rowData.get(12).toString().split("\\n");
            if (ctxDates.length == 1) {
                String[] dates = ctxDates[0].trim().split("-");
                if (dates.length == 2) {
                    String startDate = dates[0];
                    String stopDate = dates[1];

                    Obs ctxDurationObs = new Obs();
                    ctxDurationObs.setConcept(conceptService.getConceptByUuid("159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    ctxDurationObs.setValueNumeric(getDateDifference(startDate, stopDate));//calculating the difference between dates
                    groupObs.addGroupMember(ctxDurationObs);

                    Obs ctxDurationUnitsObs = new Obs();
                    ctxDurationUnitsObs.setConcept(conceptService.getConceptByUuid("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    ctxDurationUnitsObs.setValueCoded(conceptService.getConceptByUuid("1072AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    groupObs.addGroupMember(ctxDurationUnitsObs);
                }

            } else {

                for (int j = 0; j < ctxDates.length; j++) {
                    String[] dates = ctxDates[j].trim().split("-");

                    if (dates.length == 2) {
                        String startDate = dates[0];
                        String stopDate = dates[1];

                        Obs ctxDurationObs = new Obs();
                        ctxDurationObs.setConcept(conceptService.getConceptByUuid("159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                        ctxDurationObs.setValueNumeric(getDateDifference(startDate, stopDate));//calculating the difference between dates
                        groupObs.addGroupMember(ctxDurationObs);

                        Obs ctxDurationUnitsObs = new Obs();
                        ctxDurationUnitsObs.setConcept(conceptService.getConceptByUuid("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                        ctxDurationUnitsObs.setValueCoded(conceptService.getConceptByUuid("1072AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                        groupObs.addGroupMember(ctxDurationUnitsObs);

                    }
                }

            }
        }
    }

    private Double getDateDifference(String startDate, String stopDate) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        long differenceInDays = 0;
        if (startDate.contains("/")) {
            long difference = formatter.parse(stopDate).getTime() - formatter.parse(startDate).getTime();
            differenceInDays = difference / (24 * 60 * 60 * 1000);
        }

        return Double.valueOf(differenceInDays);
    }

    private void checkForValueCodedForWhoStage(Obs obs, ObsService obsService, ConceptService conceptService, String whoStageAnswer) throws ParseException {

        if (whoStageAnswer != "") {
            String[] whoStage = whoStageAnswer.split("\\n");
            obs.setObsDatetime(convertToDate(whoStage[1]));

            if (whoStage[0].equals("WHO Stage 1")) {

                obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else if (whoStage[0].equals("WHO Stage 2")) {

                obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else if (whoStage[0].equals("WHO Stage 3")) {

                obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else if (whoStage[0].equals("WHO Stage 4")) {

                obs.setValueCoded(conceptService.getConceptByUuid("1207AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else {

                obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
    }

    private void checkForValueCodedForIsTransferIn(Obs obs, ObsService obsService, ConceptService conceptService, String isTransferAnswer) {

        if (isTransferAnswer.equals("Transfer In")) {
            obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else {
            obs.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
    }

    private void checkForValueCodedForEntryPoint(Obs obs, ObsService obsService, ConceptService conceptService, String entryPointAnswer) {

        if (entryPointAnswer.equals("VCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160539AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("PMTCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("MCH")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("MCH")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else {

            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }

    }

    private Date convertToDate(String dateInString) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        Date date = null;
        if (dateInString != "") {

            date = formatter.parse(dateInString);

        }
        return date;
    }

    private Date getDateTime(String dateInString) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        Date date = formatter.parse(dateInString);
        calendar.setTime(date);

        return date;
    }
}
