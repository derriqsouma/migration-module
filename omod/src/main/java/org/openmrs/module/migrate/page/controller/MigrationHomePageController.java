package org.openmrs.module.migrate.page.controller;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.migrate.MigrateConstant;
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

@AppPage(MigrateConstant.APP_MIGRATE)
public class MigrationHomePageController {

    @RequestMapping()
    public void controller(UiUtils ui,
                           @RequestParam(value = "file", required = false) String file,
                           PageModel model) throws Exception {

        if (file != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361A/" + file;
            model.addAttribute("file", file);

            readExcelSheet(path);
        } else {
            model.addAttribute("file", "");
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
            if (rowData.get(3).toString() == ""){
                hivProgram.setDateEnrolled(convertToDate(rowData.get(2).toString()));
            }
            else {
                hivProgram.setDateEnrolled(convertToDate(rowData.get(3).toString()));
            }
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
        enrollmentEncounter.setLocation(locationService.getDefaultLocation());
        enrollmentEncounter.setDateCreated(new Date());
        enrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"),providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        if (rowData.get(3).toString() == "") {
            enrollmentEncounter.setEncounterDatetime(convertToDate(rowData.get(2).toString()));
        }
        else{
            enrollmentEncounter.setEncounterDatetime(convertToDate(rowData.get(3).toString()));
        }

        /*Patient Obs during enrollment*/
        Obs entryPointObs = new Obs();//entry point
        entryPointObs.setObsDatetime(convertToDate(rowData.get(21).toString()));
        entryPointObs.setPerson(patient);
        entryPointObs.setConcept(conceptService.getConceptByUuid("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String entryPointAnswer = rowData.get(10).toString();
        checkForValueCodedForEntryPoint(entryPointObs, obsService, conceptService, entryPointAnswer);
        enrollmentEncounter.addObs(entryPointObs);

        Obs transferInObs = new Obs();//transfer in
        transferInObs.setObsDatetime(convertToDate(rowData.get(21).toString()));
        transferInObs.setPerson(patient);
        transferInObs.setConcept(conceptService.getConceptByUuid("160563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String isTransferAnswer = rowData.get(1).toString();
        checkForValueCodedForIsTransferIn(transferInObs, conceptService, isTransferAnswer, rowData, patient, enrollmentEncounter);
        enrollmentEncounter.addObs(transferInObs);

        Obs dateConfirmedHivObs = new Obs();//Date confirmed HIV+
        dateConfirmedHivObs.setObsDatetime(convertToDate(rowData.get(21).toString()));
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
        consultationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"),providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));

        if (rowData.get(3).toString() == "") {
            consultationEncounter.setEncounterDatetime(convertToDate(rowData.get(2).toString()));
        }
        else{
            consultationEncounter.setEncounterDatetime(convertToDate(rowData.get(3).toString()));
        }

        Obs dateArtStartedObs = new Obs();//date art started
        dateArtStartedObs.setObsDatetime(convertToDate(rowData.get(20).toString()));
        dateArtStartedObs.setPerson(patient);
        dateArtStartedObs.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateArtStartedObs.setValueDate(convertToDate(rowData.get(20).toString()));
        consultationEncounter.addObs(dateArtStartedObs);

        Obs dateEligibleForArvObs = new Obs();//date eligible for ARVs
        dateEligibleForArvObs.setObsDatetime(convertToDate(rowData.get(18).toString()));
        dateEligibleForArvObs.setPerson(patient);
        dateEligibleForArvObs.setConcept(conceptService.getConceptByUuid("162227AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateEligibleForArvObs.setValueDate(convertToDate(rowData.get(18).toString()));
        consultationEncounter.addObs(dateEligibleForArvObs);

        Obs whoStageObs = new Obs();//World Health Organization HIV stage
        whoStageObs.setPerson(patient);
        whoStageObs.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String whoStageAnswer = rowData.get(17).toString();
        checkForValueCodedForWhoStage(whoStageObs, obsService, conceptService, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
        consultationEncounter.addObs(whoStageObs);

        encounterService.saveEncounter(consultationEncounter);//saving the consultationEncounter

         /*hivLastClinicalEncounter*/
        Encounter hivLastClinicalEncounter = new Encounter();
        hivLastClinicalEncounter.setPatient(patient);
        hivLastClinicalEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        hivLastClinicalEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        computeLocation(hivLastClinicalEncounter, rowData);
        hivLastClinicalEncounter.setDateCreated(new Date());
        hivLastClinicalEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"),providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        hivLastClinicalEncounter.setEncounterDatetime(convertToDate(rowData.get(21).toString()));

        Obs lastReturnToClinicObs = new Obs();//Last return to clinic obs
        lastReturnToClinicObs.setObsDatetime(convertToDate(rowData.get(21).toString()));
        lastReturnToClinicObs.setPerson(patient);
        lastReturnToClinicObs.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        lastReturnToClinicObs.setValueDate(convertToDate(rowData.get(23).toString()));
        hivLastClinicalEncounter.addObs(lastReturnToClinicObs);

        encounterService.saveEncounter(hivLastClinicalEncounter);//saving hivLastClinicalEncounter

        getCtxObs(rowData, patient);

        checkIfPregnant(patient, rowData, concept, conceptService);
    }

    private void checkIfPregnant(Patient patient, List<Object> rowData, Concept concept, ConceptService conceptService) throws ParseException {
        String gender =rowData.get(9).toString();
        String isPregnant = rowData.get(15).toString();

        FormService formService = Context.getFormService();
        ProviderService providerService = Context.getProviderService();
        EncounterService encounterService = Context.getEncounterService();
        LocationService locationService = Context.getLocationService();

        if (gender.contains("F")){
            if (isPregnant != ""){
                String[] obsMade = isPregnant.split("\\n");
                String[] firstObs = obsMade[0].trim().split("\\|");
                Date edd = convertToDate(firstObs[0]);
                if (firstObs.length == 2) {
                    String referral = firstObs[1];
                }
                Encounter encounter = new Encounter();
                encounter.setPatient(patient);
                encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
                encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
                encounter.setLocation(locationService.getDefaultLocation());
                encounter.setDateCreated(new Date());
                encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
                encounter.setEncounterDatetime(edd);

                Obs pregnancyObs = new Obs();
                pregnancyObs.setPerson(patient);
                pregnancyObs.setObsDatetime(edd);
                pregnancyObs.setConcept(conceptService.getConceptByUuid("5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                pregnancyObs.setValueCoded(conceptService.getConceptByUuid("161033AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                encounter.addObs(pregnancyObs);

                Obs eddObs = new Obs();
                eddObs.setPerson(patient);
                eddObs.setObsDatetime(edd);
                eddObs.setConcept(conceptService.getConceptByUuid("5596AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                eddObs.setValueDate(edd);
                encounter.addObs(eddObs);

                encounterService.saveEncounter(encounter);
            }
        }
    }

    private void computeLocation(Encounter hivLastClinicalEncounter, List<Object> rowData) {
        LocationService locationService = Context.getLocationService();
        String location = rowData.get(22).toString();

        if (location.contains("Amase")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Amase Dispensary"));
        }else if (location.contains("LUKOLIS")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Lukolis Model Health Centre"));
        }else if (location.contains("Obekai")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Obekai Dispensary"));
        }else if (location.contains("Busia") || location.contains("Busia Module 1") || location.contains("Busia Module 2")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Busia District Hospital"));
        }else if (location.contains("Nambale")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Nambale Health Centre"));
        }else if (location.contains("Amukura")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Amukura Health Centre"));
        }else if (location.contains("Teso")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Teso District Hospital"));
        }else if (location.contains("MTRH Module 1") || location.contains("MTRH Module 2") || location.contains("MTRH Module 3") || location.contains("MTRH Module 4")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi Teaching Refferal Hospital"));
        }else if (location.contains("Kaptama (Friends) dispensary")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kaptama (Friends) Health Centre"));
        }else if (location.contains("Chulaimbo") || location.contains("Chulaimbo Module 1") || location.contains("Chulaimbo Module 2")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Chulaimbo Sub-District Hospital"));
        }else if (location.contains("Malaba")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Malaba Dispensary"));
        }else if (location.contains("Naitiri")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Naitiri Sub-District Hospital"));
        }else if (location.contains("Lupida")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Lupida Health Centre"));
        }else if (location.contains("Bumala A")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bumala A Health Centre"));
        }else if (location.contains("Bumala B")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bumala B Health Centre"));
        }else if (location.contains("Mukhobola")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Mukhobola Health Centre"));
        }else if (location.contains("Uasin Gishu District Hospital")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Uasin Gishu District Hospital"));
        }else if (location.contains("Iten")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Iten District Hospital"));
        }else if (location.contains("Burnt Forest")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Burnt Forest Rhdc (Eldoret East)"));
        }else if (location == "Kitale"){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kitale District Hospital"));
        }else if (location.contains("ANGURAI")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Angurai Health Centre"));
        }else if (location.contains("Port Victoria")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Port Victoria Hospital"));
        }else if (location.contains("Mois Bridge")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi's Bridge Health Centre"));
        }else if (location.contains("Mosoriot")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Mosoriot Clinic"));
        }else if (location.contains("Turbo")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Turbo Health Centre"));
        }else if (location.contains("Madende Health Center")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Madende Dispensary"));
        }else if (location.contains("Makutano")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Makutano Dispensary"));
        }else if (location.contains("Kapenguria")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kapenguria District Hospital"));
        }else if (location.contains("Webuye")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Webuye Health Centre"));
        }else if (location.contains("Osieko")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Osieko Dispensary"));
        }else if (location.contains("Moi University")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi University Health Centre"));
        }else if (location.contains("Milo")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Milo Health Centre"));
        }else if (location.contains("Sio Port")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Sio Port District Hospital"));
        }else if (location.contains("Huruma SDH")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Huruma District Hospital"));
        }else if (location.contains("BOKOLI")){
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bokoli Hospital"));
        }
    }

//    private Date getHiVDateoFEnrollment(String dateInString, Double age) throws ParseException {
//        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
//        Calendar calendar = Calendar.getInstance();
//        Date date = null;
//        if (dateInString != "") {
//            date = formatter.parse(dateInString);
//            int months = (int)Math.round(age);
//            calendar.setTime(date);
//            calendar.set(Calendar.MONTH, (calendar.get(Calendar.MONTH)+months));//incrementing the date by months
//            date = calendar.getTime();
//        }
//        return date;
//    }

    private void enrollInToTBProgram(Patient patient, PatientProgram tbProgram, String enrolledInTb) throws ParseException {
        String[] tbDates = enrolledInTb.toString().split("\\n");
        String[] dates1 = tbDates[0].trim().split("-");

        EncounterService encounterService = Context.getEncounterService();
        ConceptService conceptService = Context.getConceptService();
        FormService formService = Context.getFormService();
        LocationService locationService = Context.getLocationService();
        ProviderService providerService = Context.getProviderService();
        ProgramWorkflowService workflowService = Context.getProgramWorkflowService();

        Encounter tbEnrollmentEncounter = new Encounter();
        tbEnrollmentEncounter.setPatient(patient);
        tbEnrollmentEncounter.setForm(formService.getFormByUuid("89994550-9939-40f3-afa6-173bce445c79"));
        tbEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("9d8498a4-372d-4dc4-a809-513a2434621e"));
        tbEnrollmentEncounter.setLocation(locationService.getDefaultLocation());
        tbEnrollmentEncounter.setDateCreated(new Date());
        tbEnrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"),providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        tbEnrollmentEncounter.setEncounterDatetime(convertToDate(dates1[0]));

        tbProgram.setDateEnrolled(convertToDate(dates1[0]));
        workflowService.savePatientProgram(tbProgram);

        Obs tbTreatmentObs = new Obs();
        tbTreatmentObs.setObsDatetime(convertToDate(dates1[0]));
        tbTreatmentObs.setConcept(conceptService.getConceptByUuid("1113AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        tbTreatmentObs.setValueDate(convertToDate(dates1[0]));

        tbEnrollmentEncounter.addObs(tbTreatmentObs);
        encounterService.saveEncounter(tbEnrollmentEncounter);//saving tbEnrollmentEncounter

        for (int j = 0; j < tbDates.length; j++) {
            String[] dates = tbDates[j].trim().split("-");

            if (dates.length == 2) {
                String stopDate = dates[1];

                Encounter tbTreatmentDiscontinuationEncounter = new Encounter();
                tbTreatmentDiscontinuationEncounter.setPatient(patient);
                tbTreatmentDiscontinuationEncounter.setForm(formService.getFormByUuid("4b296dd0-f6be-4007-9eb8-d0fd4e94fb3a"));
                tbTreatmentDiscontinuationEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("d3e3d723-7458-4b4e-8998-408e8a551a84"));
                tbTreatmentDiscontinuationEncounter.setDateCreated(new Date());
                tbTreatmentDiscontinuationEncounter.setLocation(locationService.getDefaultLocation());
                tbTreatmentDiscontinuationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
                tbTreatmentDiscontinuationEncounter.setEncounterDatetime(convertToDate(stopDate));

                Obs tbTreatmentDiscontinuationObs = new Obs();
                tbTreatmentDiscontinuationObs.setObsDatetime(convertToDate(stopDate));
                tbTreatmentDiscontinuationObs.setConcept(conceptService.getConceptByUuid("159431AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                tbTreatmentDiscontinuationObs.setValueDate(convertToDate(stopDate));

                tbTreatmentDiscontinuationEncounter.addObs(tbTreatmentDiscontinuationObs);
                encounterService.saveEncounter(tbTreatmentDiscontinuationEncounter);
            }
        }

    }

    private void getCtxObs(List<Object> rowData, Patient patient) throws ParseException {
        ConceptService conceptService = Context.getConceptService();
        FormService formService = Context.getFormService();
        EncounterService encounterService = Context.getEncounterService();
        ProviderService providerService = Context.getProviderService();
        LocationService locationService = Context.getLocationService();

        if (rowData.get(12) != "") {
            String[] ctxDates = rowData.get(12).toString().split("\\n");

            Encounter ctxEncounter = new Encounter();
            ctxEncounter.setPatient(patient);
            ctxEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
            ctxEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
            ctxEncounter.setLocation(locationService.getDefaultLocation());
            ctxEncounter.setDateCreated(new Date());
            ctxEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));

            Obs groupObs = new Obs();/*CTX Obs*/
            groupObs.setConcept(conceptService.getConceptByUuid("1442AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            groupObs.setPerson(patient);

                for (int j = 0; j < ctxDates.length; j++) {
                    String[] dates = ctxDates[j].trim().split("-");
                    String startDate = dates[0];

                    if (startDate.contains("/")) {

                        ctxEncounter.setEncounterDatetime(convertToDate(startDate));
                        groupObs.setObsDatetime(convertToDate(startDate));

                        if (dates.length == 2) {
                            String stopDate = dates[1];

                            Obs ctxDrugObs = new Obs();
                            ctxDrugObs.setConcept(conceptService.getConceptByUuid("1282AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                            ctxDrugObs.setValueCoded(conceptService.getConceptByUuid("105281AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                            groupObs.addGroupMember(ctxDrugObs);

                            Obs ctxDurationObs = new Obs();
                            ctxDurationObs.setConcept(conceptService.getConceptByUuid("159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                            ctxDurationObs.setValueNumeric(getDateDifference(startDate, stopDate));//calculating the difference between dates
                            groupObs.addGroupMember(ctxDurationObs);

                            Obs ctxDurationUnitsObs = new Obs();
                            ctxDurationUnitsObs.setConcept(conceptService.getConceptByUuid("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                            ctxDurationUnitsObs.setValueCoded(conceptService.getConceptByUuid("1072AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                            groupObs.addGroupMember(ctxDurationUnitsObs);

                            ctxEncounter.addObs(groupObs);
                            encounterService.saveEncounter(ctxEncounter);
                        } else {
                            ctxEncounter.addObs(groupObs);
                            encounterService.saveEncounter(ctxEncounter);
                        }


                    }
                }

        }
    }

    private Double getDateDifference(String startDate, String stopDate) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        long differenceInDays = 0;
        if(startDate !="" && stopDate !="") {
            if (startDate.contains("/")) {
                long difference = formatter.parse(stopDate).getTime() - formatter.parse(startDate).getTime();
                differenceInDays = difference / (24 * 60 * 60 * 1000);
            }
        }
        return Double.valueOf(differenceInDays);
    }

    private void checkForValueCodedForWhoStage(Obs obs, ObsService obsService, ConceptService conceptService, String whoStageAnswer, Double age) throws ParseException {

        if (whoStageAnswer != "") {
            String[] whoStage = whoStageAnswer.split("\\n");
            obs.setObsDatetime(convertToDate(whoStage[1]));

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

    private void checkForValueCodedForIsTransferIn(Obs obs, ConceptService conceptService, String isTransferAnswer, List<Object> rowData, Patient patient, Encounter enrollmentEncounter) throws ParseException {

        if (isTransferAnswer.equals("Transfer In")) {
            obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            Obs transferInDateObs = new Obs();
            transferInDateObs.setObsDatetime(convertToDate(rowData.get(2).toString()));
            transferInDateObs.setPerson(patient);
            transferInDateObs.setConcept(conceptService.getConceptByUuid("160534AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInDateObs.setValueDate(convertToDate(rowData.get(2).toString()));

            enrollmentEncounter.addObs(transferInDateObs);
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
        } else if (entryPointAnswer.equals("TB")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160541AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }  else if (entryPointAnswer.equals("HCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159938AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (entryPointAnswer.equals("HCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159938AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else {

            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
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

//    private Date getDateTime(String dateInString) throws ParseException {
//
//        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//        Calendar calendar = Calendar.getInstance();
//        Date date = formatter.parse(dateInString);
//        calendar.setTime(date);
//
//        return date;
//    }
}
