package org.openmrs.module.migrate.kakuma;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;
import org.openmrs.module.migrate.ConvertStringToDate1;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 2/16/15.
 */
public class KakumaEid {
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
    ConvertStringToDate1 convertStringToDate = new ConvertStringToDate1();
    PatientService patientService = Context.getPatientService();
    PersonService personService = Context.getPersonService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public KakumaEid(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();
        ReadExcelSheetKakuma readExcelSheetKakuma = new ReadExcelSheetKakuma(path, session, kenyaUi);
        sheetData = readExcelSheetKakuma.readExcelSheet();

        saveInfantInfo(sheetData);
    }

    private void saveInfantInfo(List<List<Object>> sheetData) throws ParseException {
        int counter = 0;
        for (int i = 0; i < sheetData.size(); i++) {
            List<Object> rowData = sheetData.get(i);

            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = (String) rowData.get(7);
            Date dob = null;
            fullNames = String.valueOf(rowData.get(3)).replaceAll("\\s+", " ").split(" ");

            if (fullNames.length <= 1 || rowData.get(3).toString().isEmpty() || rowData.get(5).toString().isEmpty() || rowData.get(6).toString().isEmpty() ) {
                System.out.println("\n\n\n Error \n\n\n");
                continue;
            }else {
                if (rowData.get(6).toString().contains("MALE")) {
                    gender = "M";
                }
                if (rowData.get(6).toString().contains("FEMALE")) {
                    gender = "F";
                }
                String[] dob1 = String.valueOf(rowData.get(5)).replaceAll("\\s+", " ").split(" ");
                dob = convertStringToDate.convert(dob1[0].toString());

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
                String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "migration");
                openmrsId.setIdentifierType(openmrsIdType);
                openmrsId.setDateCreated(new Date());
                openmrsId.setLocation(defaultLocation);
                openmrsId.setIdentifier(generated);
                openmrsId.setVoided(false);
                openmrsId.setPreferred(true);

                patient.addIdentifier(openmrsId);
                if (!patientService.isIdentifierInUseByAnotherPatient(openmrsId)) {
                    patientService.savePatient(patient);//saving the patient
                    addRelationship(rowData, patient);
                    enrollInToMch_csProgram(rowData,patient);
                    counter += 1;
                } else {
                        kenyaUi.notifyError(session, "the patient identifier #" + openmrsId + " already in use by another patient");
                        System.out.println("\n\n the patient identifier #" + openmrsId + " already in use by another patient");
                        continue;
                }


            }
        }
        System.out.println(counter + " infant(s) added");
    }

    private void enrollInToMch_csProgram(List<Object> rowData, Patient patient) throws ParseException {

        String[] enrollmentDate = String.valueOf(rowData.get(4)).replaceAll("\\s+", " ").split(" ");

        PatientProgram mch_csProgram = new PatientProgram();//enroll into MCHCS Program
        mch_csProgram.setPatient(patient);
        mch_csProgram.setProgram(workflowService.getProgramByUuid("c2ecdf11-97cd-432a-a971-cfd9bd296b83"));
        if (rowData.get(4) != "") {
            mch_csProgram.setDateEnrolled(convertStringToDate.convert(enrollmentDate[0].toString()));
        }else {
            mch_csProgram.setDateEnrolled(new Date());
        }
        workflowService.savePatientProgram(mch_csProgram);

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("8553d869-bdc8-4287-8505-910c7c998aff"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("415f5136-ca4a-49a8-8db3-f994187c3af6"));
        if (rowData.get(4) != "") {
            encounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
            encounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        }else {
            encounter.setDateCreated(new Date());
            encounter.setEncounterDatetime(new Date());
        }
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));


        Obs infantFeedingObs = new Obs();// infant feeding
        if (rowData.get(4) != "") {
            infantFeedingObs.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        }else {
            infantFeedingObs.setObsDatetime(new Date());
        }
        infantFeedingObs.setPerson(patient);
        infantFeedingObs.setConcept(conceptService.getConceptByUuid("1151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        checkForValueCodedForInfantFeedingMethod(infantFeedingObs, rowData);
        encounter.addObs(infantFeedingObs);

        encounterService.saveEncounter(encounter);
    }

    private void saveInfantObs(Patient patient, List<Object> rowData) throws ParseException {

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("755b59e6-acbb-4853-abaf-be302039f902"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("bcc6da85-72f2-4291-b206-789b8186a021"));
        encounter.setLocation(defaultLocation);

        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(new Date());

        if (rowData.get(14) != "") {
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
            } else {
                resultsObs.setValueCoded(conceptService.getConceptByUuid("1301AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            LabTestsGroupObs.addGroupMember(resultsObs);

            encounter.addObs(LabTestsGroupObs);
        }

        Obs infantFeedingObs = new Obs();// infant feeding
        infantFeedingObs.setObsDatetime(new Date());
        infantFeedingObs.setPerson(patient);
        infantFeedingObs.setConcept(conceptService.getConceptByUuid("1151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        checkForValueCodedForInfantFeedingMethod(infantFeedingObs, rowData);
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

        if (rowData.get(9) != "") {
            if (rowData.get(9).toString().equals("EBF")) {
                obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            } else if (rowData.get(9).toString().equals("MF")) {
                obs.setValueCoded(conceptService.getConceptByUuid("6046AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            }
        }
    }

    private void addRelationship(List<Object> rowData, Patient patient) {
        String upn = rowData.get(0).toString();
        Patient patient1;
        List<Patient> patientList = patientService.getPatients(null, upn, null, true);

        if (patientList.size() > 0) {
            Relationship relationship = new Relationship();
            patient1 = patientService.getPatient(patientList.get(0).getPatientId());

            relationship.setPersonA(patient1);
            relationship.setPersonB(patient);
            relationship.setRelationshipType(personService.getRelationshipTypeByUuid("8d91a210-c2cc-11de-8d13-0010c6dffd0f"));
//            if (patient.getNames() != patient1.getNames()) {
                personService.saveRelationship(relationship);
//            }
        }
    }
}
