package org.openmrs.module.migrate;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by derric on 7/14/14.
 */
public class Moh408 {
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
    ConvertStringToDate convertStringToDate = new ConvertStringToDate();
    PatientService patientService = Context.getPatientService();
    PersonService personService = Context.getPersonService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public Moh408(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void initMoh408() throws Exception {
        List<List<Object>> sheetData = new ArrayList();
        sheetData = readExcelSheet(path);

        saveInfantInfo(sheetData);
    }

    private void saveInfantInfo(List<List<Object>> sheetData) throws ParseException {
        int counter =0;
        for (int i = 0; i < sheetData.size(); i++) {
            List<Object> rowData = sheetData.get(i);

            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = (String) rowData.get(7);
            Date dob = convertStringToDate.convert((String) rowData.get(6));
            fullNames = String.valueOf(rowData.get(4)).replaceAll("\\s+", " ").split(" ");

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
            if (rowData.get(4).toString() != "") {
                Patient patient = new Patient();

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
                openmrsId.setLocation(defaultLocation);
                openmrsId.setIdentifier(generated);
                openmrsId.setVoided(false);

                PatientIdentifier amrId = new PatientIdentifier();
                amrId.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("8d79403a-c2cc-11de-8d13-0010c6dffd0f"));
                amrId.setDateCreated(new Date());
                amrId.setLocation(defaultLocation);
                amrId.setIdentifier((String) rowData.get(2));
                amrId.setVoided(false);
                amrId.setPreferred(true);

                patient.addIdentifiers(Arrays.asList(openmrsId, amrId));
                if (!patientService.isIdentifierInUseByAnotherPatient(amrId)) {
                    patientService.savePatient(patient);//saving the patient
                    enrollInToMch_csProgram(rowData,patient);
                    counter += 1;

                } else {
                    kenyaUi.notifyError(session, "the patient identifier #" + amrId + " already in use by another patient");
                    System.out.println("\n\n the patient identifier #" + amrId + " already in use by another patient");
                    continue;
                }
            }
        }
        System.out.println(counter + " infant(s) added");
    }

    private void enrollInToMch_csProgram(List<Object> rowData, Patient patient) throws ParseException {

        PatientProgram mch_csProgram = new PatientProgram();//enroll into MCHCS Program
        mch_csProgram.setPatient(patient);
        mch_csProgram.setProgram(workflowService.getProgramByUuid("c2ecdf11-97cd-432a-a971-cfd9bd296b83"));
        mch_csProgram.setDateEnrolled(convertStringToDate.convert(rowData.get(1).toString()));
        workflowService.savePatientProgram(mch_csProgram);

        addRelationship(rowData,patient);

        Encounter mch_csEnrollmentEncounter = new Encounter();
        mch_csEnrollmentEncounter.setPatient(patient);
        mch_csEnrollmentEncounter.setForm(formService.getFormByUuid("8553d869-bdc8-4287-8505-910c7c998aff"));
        mch_csEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("415f5136-ca4a-49a8-8db3-f994187c3af6"));

        String location = rowData.get(13).toString();
        getLocation(mch_csEnrollmentEncounter, location);

        mch_csEnrollmentEncounter.setDateCreated(new Date());
        mch_csEnrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        mch_csEnrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(1).toString()));

        //enrollment obs
        Obs obs = new Obs();
        obs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        obs.setPerson(patient);
        obs.setConcept(conceptService.getConceptByUuid("5303AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(56) !=""){
            if (rowData.get(56).toString().contains("negative")) {
                obs.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            } else {
                obs.setValueCoded(conceptService.getConceptByUuid("822AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
        mch_csEnrollmentEncounter.addObs(obs);

        saveInfantObs(patient, rowData);
        encounterService.saveEncounter(mch_csEnrollmentEncounter);
    }

    private void saveInfantObs(Patient patient, List<Object> rowData) throws ParseException {

        /*First DNA PCR Test at 6 weeks or First Contact*/

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("755b59e6-acbb-4853-abaf-be302039f902"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("bcc6da85-72f2-4291-b206-789b8186a021"));
        encounter.setLocation(defaultLocation);
//        String location = rowData.get(13).toString();
//        getLocation(encounter, location);
        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(new Date());

        if (rowData.get(14) !="") {
            Obs LabTestsGroupObs = new Obs();/* Group Obs*/
            LabTestsGroupObs.setConcept(conceptService.getConceptByUuid("162085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            LabTestsGroupObs.setPerson(patient);

            Obs sampleCollectionDate = new Obs();// sample collection dates
            sampleCollectionDate.setObsDatetime(convertStringToDate.convert(rowData.get(17).toString()));
            sampleCollectionDate.setPerson(patient);
            sampleCollectionDate.setConcept(conceptService.getConceptByUuid("159951AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            sampleCollectionDate.setValueDate(convertStringToDate.convert(rowData.get(17).toString()));
            LabTestsGroupObs.addGroupMember(sampleCollectionDate);

            Obs testContextStatusObs = new Obs();// sample collection dates
            testContextStatusObs.setObsDatetime(convertStringToDate.convert(rowData.get(17).toString()));
            testContextStatusObs.setPerson(patient);
            testContextStatusObs.setConcept(conceptService.getConceptByUuid("162084AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            testContextStatusObs.setValueCoded(conceptService.getConceptByUuid("162080AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            LabTestsGroupObs.addGroupMember(testContextStatusObs);

            Obs resultsCollectionDate = new Obs();// results collection dates
            resultsCollectionDate.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
            resultsCollectionDate.setPerson(patient);
            resultsCollectionDate.setConcept(conceptService.getConceptByUuid("160082AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            resultsCollectionDate.setValueDate(convertStringToDate.convert(rowData.get(18).toString()));
            LabTestsGroupObs.addGroupMember(resultsCollectionDate);

            Obs resultsObs = new Obs();// results
            resultsObs.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
            resultsObs.setPerson(patient);
            resultsObs.setConcept(conceptService.getConceptByUuid("844AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (rowData.get(19).toString().equals("negative")) {
                resultsObs.setValueCoded(conceptService.getConceptByUuid("1302AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }else{
                resultsObs.setValueCoded(conceptService.getConceptByUuid("1301AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            LabTestsGroupObs.addGroupMember(resultsObs);

            encounter.addObs(LabTestsGroupObs);
        }

        Obs infantFeedingObs = new Obs();// infant feeding
        infantFeedingObs.setObsDatetime(new Date());
        infantFeedingObs.setPerson(patient);
        infantFeedingObs.setConcept(conceptService.getConceptByUuid("1151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        checkForValueCodedForInfantFeedingMethod(infantFeedingObs,rowData);
        encounter.addObs(infantFeedingObs);

        Obs npvMedicationObs = new Obs();// NPV Medication
        npvMedicationObs.setObsDatetime(new Date());
        npvMedicationObs.setPerson(patient);
        npvMedicationObs.setConcept(conceptService.getConceptByUuid("1276AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(21).toString().equals("Yes")) {
            npvMedicationObs.setValueCoded(conceptService.getConceptByUuid("80586AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            npvMedicationObs.setValueBoolean(true);
        }
        encounter.addObs(npvMedicationObs);

        Obs ctxMedicationObs = new Obs();// CTX Medication
        ctxMedicationObs.setObsDatetime(new Date());
        ctxMedicationObs.setPerson(patient);
        ctxMedicationObs.setConcept(conceptService.getConceptByUuid("1276AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(22).toString().equals("Yes")) {
            ctxMedicationObs.setValueCoded(conceptService.getConceptByUuid("105281AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            ctxMedicationObs.setValueBoolean(true);
        }
        encounter.addObs(ctxMedicationObs);

        encounterService.saveEncounter(encounter);
    }

    private void checkForValueCodedForInfantFeedingMethod(Obs obs, List<Object> rowData) {

        if (rowData.get(20) != "") {
            if (rowData.get(20).toString().equals("EBF")) {
                obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            } else if (rowData.get(20).toString().equals("MF")) {
                obs.setValueCoded(conceptService.getConceptByUuid("6046AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            }
        }
    }

    private void addRelationship(List<Object> rowData, Patient patient) {
        String name = rowData.get(10).toString().replaceAll("\\s+", " ");
        Patient patient1 = new Patient();
        List<Patient> patientList = patientService.getPatients(name, null, null, true);

        if (patientList.size() > 0){
            Relationship relationship = new Relationship();
            patient1 = patientService.getPatient(patientList.get(0).getPatientId());

            relationship.setPersonA(patient1);
            relationship.setPersonB(patient);
            relationship.setRelationshipType(personService.getRelationshipTypeByUuid("8d91a210-c2cc-11de-8d13-0010c6dffd0f"));
            if (patient.getNames() != patient1.getNames()) {
                personService.saveRelationship(relationship);
            }
        }
    }

    private void getLocation(Encounter encounter, String location) {

        if (location.contains("AMASE DISP")){
            encounter.setLocation(locationService.getLocation("Amase Dispensary"));
        }else if (location.contains("LUKOLIS DISP")){
            encounter.setLocation(locationService.getLocation("Lukolis Model Health Centre"));
        }else if (location.contains("OBEKAI DIS")){
            encounter.setLocation(locationService.getLocation("Obekai Dispensary"));
        }else if (location.contains("BUSIA DH")){
            encounter.setLocation(locationService.getLocation("Busia District Hospital"));
        }else if (location.contains("NAMBALE HC")){
            encounter.setLocation(locationService.getLocation("Nambale Health Centre"));
        }else if (location.contains("AMUKURA HC")){
            encounter.setLocation(locationService.getLocation("Amukura Health Centre"));
        }else if (location.contains("TESO DH")){
            encounter.setLocation(locationService.getLocation("Teso District Hospital"));
        }else if (location.contains("MTRH")){
            encounter.setLocation(locationService.getLocation("Moi Teaching Refferal Hospital"));
        }else if (location.contains("Kaptama (Friends) dispensary")){
            encounter.setLocation(locationService.getLocation("Kaptama (Friends) Health Centre"));
        }else if (location.contains("CHULAIMBO SDH")){
            encounter.setLocation(locationService.getLocation("Chulaimbo Sub-District Hospital"));
        }else if (location.contains("Malaba")){
            encounter.setLocation(locationService.getLocation("Malaba Dispensary"));
        }else if (location.contains("NAITIRI SDH")){
            encounter.setLocation(locationService.getLocation("Naitiri Sub-District Hospital"));
        }else if (location.contains("LUPIDA HC")){
            encounter.setLocation(locationService.getLocation("Lupida Health Centre"));
        }else if (location.contains("BUMALA A HC")){
            encounter.setLocation(locationService.getLocation("Bumala A Health Centre"));
        }else if (location.contains("BUMALA B HC")){
            encounter.setLocation(locationService.getLocation("Bumala B Health Centre"));
        }else if (location.contains("MUKHOBOLA HC")){
            encounter.setLocation(locationService.getLocation("Mukhobola Health Centre"));
        }else if (location.contains("Uasin Gishu District Hospital")){
            encounter.setLocation(locationService.getLocation("Uasin Gishu District Hospital"));
        }else if (location.contains("ITEN DH")){
            encounter.setLocation(locationService.getLocation("Iten District Hospital"));
        }else if (location.contains("BURNT FOREST SDH")){
            encounter.setLocation(locationService.getLocation("Burnt Forest Rhdc (Eldoret East)"));
        }else if (location.contains("KITALE DH")){
            encounter.setLocation(locationService.getLocation("Kitale District Hospital"));
        }else if (location.contains("ANGURAI HC")){
            encounter.setLocation(locationService.getLocation("Angurai Health Centre"));
        }else if (location.contains("PORT VICTORIA HOS")){
            encounter.setLocation(locationService.getLocation("Port Victoria Hospital"));
        }else if (location.contains("Mois Bridge")){
            encounter.setLocation(locationService.getLocation("Moi's Bridge Health Centre"));
        }else if (location.contains("MOSORIOT HC")){
            encounter.setLocation(locationService.getLocation("Mosoriot Clinic"));
        }else if (location.contains("TURBO HC")){
            encounter.setLocation(locationService.getLocation("Turbo Health Centre"));
        }else if (location.contains("MADENDE HC")){
            encounter.setLocation(locationService.getLocation("Madende Dispensary"));
        }else if (location.contains("MAKUTANO DISP")){
            encounter.setLocation(locationService.getLocation("Makutano Dispensary"));
        }else if (location.contains("KAPENGURIA DH")){
            encounter.setLocation(locationService.getLocation("Kapenguria District Hospital"));
        }else if (location.contains("WEBUYE DH")){
            encounter.setLocation(locationService.getLocation("Webuye Health Centre"));
        }else if (location.contains("Osieko")){
            encounter.setLocation(locationService.getLocation("Osieko Dispensary"));
        }else if (location.contains("Moi University")){
            encounter.setLocation(locationService.getLocation("Moi University Health Centre"));
        }else if (location.contains("Milo")){
            encounter.setLocation(locationService.getLocation("Milo Health Centre"));
        }else if (location.contains("SIO PORT HOSP")){
            encounter.setLocation(locationService.getLocation("Sio Port District Hospital"));
        }else if (location.contains("HURUMA DH")){
            encounter.setLocation(locationService.getLocation("Huruma District Hospital"));
        }else if (location.contains("BOKOLI SDH")){
            encounter.setLocation(locationService.getLocation("Bokoli Hospital"));
        }else if (location.contains("MAUTUMA SDH")){
            encounter.setLocation(locationService.getLocation("Mautuma Sub-District Hospital"));
        }else if (location.contains("MT. ELGON DH")){
            encounter.setLocation(locationService.getLocation("Mt Elgon District Hospital"));
        }else if (location.contains("CHEPTAIS SDH")){
            encounter.setLocation(locationService.getLocation("Cheptais Sub District Hospital"));
        }else if (location.contains("ZIWA SDH")){
            encounter.setLocation(locationService.getLocation("Ziwa Sub-District Hospital"));
        }else if (location.contains("UASIN GISHU DH")){
            encounter.setLocation(locationService.getLocation("Uasin Gishu District Hospital"));
        }else if (location.contains("CHANGARA DISP")){
            encounter.setLocation(locationService.getLocation("Changara (GOK) Dispensary"));
        }else if (location.contains("TENGES HC")){
            encounter.setLocation(locationService.getLocation("Tenges Health Centre"));
        }else if (location.contains("KHUNYANGU DH")){
            encounter.setLocation(locationService.getLocation("Khunyangu Sub-District Hospital"));
        }else if (location.contains("KABARNET DH")){
            encounter.setLocation(locationService.getLocation("Kabarnet District Hospital"));
        }else if (location.contains("GK PRISONS DISP BUSIA")){
            encounter.setLocation(locationService.getLocation("GK Prisons Dispensary (Busia)"));
        }
        else{
            encounter.setLocation(defaultLocation);
        }
    }

    private void checkForValueCodedForEntryPoint(Obs obs, List<Object> rowData) {
        String entryPointAnswer = rowData.get(8).toString();

        if (entryPointAnswer.startsWith("PEDIATRIC OUTPATIENT CLINIC")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160537AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.startsWith("PREVENTION OF MOTHER-TO-CHILD TRANSMISSION OF HIV")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.startsWith("MATERNAL CHILD HEALTH")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else {

            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }


    }

    private List<List<Object>> readExcelSheet(String path) {
        List<List<Object>> sheetData = new ArrayList();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        try {
            FileInputStream file = new FileInputStream(new File(path));

            HSSFWorkbook workbook = new HSSFWorkbook(file);

            HSSFSheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();
                if (row.getRowNum() == 0 || row.getRowNum() == 1 || row.getRowNum() == 2 || row.getRowNum() == 3 || row.getRowNum() == 4 || row.getRowNum() == 5) {
                    continue;
                }

                int expectedColumns = 57;
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
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(format.format(cell.getDateCellValue()));
                            } else {
                                rowData.add(cell.getNumericCellValue());
                            }
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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            kenyaUi.notifyError(session, "No such file or directory");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sheetData;
    }
}
