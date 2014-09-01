package org.openmrs.module.migrate;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 7/14/14.
 */
public class Moh361A {
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
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
    ConvertStringToDate convertStringToDate = new ConvertStringToDate();
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    //constructor
    public Moh361A(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void initMoh361A() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheet readExcelSheet = new ReadExcelSheet(path, session, kenyaUi);
        sheetData = readExcelSheet.readExcelSheet();

        savePatientInfo(sheetData);
    }

    private void savePatientInfo(List<List<Object>> sheetData) throws ParseException {

        for (int i = 0; i < sheetData.size(); i++) {

            List<Object> rowData = sheetData.get(i);
            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = (String) rowData.get(9);
            Date dob = convertStringToDate.convert((String) rowData.get(7));
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
            if (rowData.get(6) != "") {
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

                PatientIdentifier amrId = new PatientIdentifier();
                amrId.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("8d79403a-c2cc-11de-8d13-0010c6dffd0f"));
                amrId.setDateCreated(new Date());
                amrId.setLocation(defaultLocation);
                amrId.setIdentifier((String) rowData.get(5));
                amrId.setVoided(false);

                PatientIdentifier upn = null;
                if (rowData.get(4) != "") {
                    upn = new PatientIdentifier();
                    upn.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a"));
                    upn.setDateCreated(new Date());
                    upn.setLocation(defaultLocation);
                    upn.setIdentifier(rowData.get(4).toString().replaceAll("[^\\d]", ""));
                    upn.setVoided(false);
                    upn.setPreferred(true);

                    patient.addIdentifiers(Arrays.asList(upn, openmrsId, amrId));
                    if (!patientService.isIdentifierInUseByAnotherPatient(upn)) {
                        patientService.savePatient(patient);//saving the patient
                        savePatientObs(patient, rowData);
                        counter += 1;
                    } else {
                        kenyaUi.notifyError(session, "the patient identifier #" + upn + " already in use by another patient");
                        System.out.println("\n\n the patient identifier #" + upn + " already in use by another patient");
                        continue;
                    }

                } else {
                    amrId.setPreferred(true);

                    patient.addIdentifiers(Arrays.asList(openmrsId, amrId));
                    if (!patientService.isIdentifierInUseByAnotherPatient(amrId)) {
                        patientService.savePatient(patient);//saving the patient
                        savePatientObs(patient, rowData);
                        counter += 1;
                    } else {
                        kenyaUi.notifyError(session, "the patient identifier #" + amrId + " already in use by another patient");
                        System.out.println("\n\n the patient identifier #" + amrId + " already in use by another patient");
                        continue;
                    }

                }
            }
        }
        kenyaUi.notifySuccess(session, " " + counter + " patient(s) added");
        System.out.println(" \n\n" + counter + " patient(s) added");

    }

    private void savePatientObs(Patient patient, List<Object> rowData) throws ParseException {

        /*Patient Program*/
        if (rowData.get(4) != "") {
            PatientProgram hivProgram = new PatientProgram();
            hivProgram.setPatient(patient);//enroll in HIV Program
            hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
            if (rowData.get(3) == "") {
                hivProgram.setDateEnrolled(convertStringToDate.convert(rowData.get(2).toString()));
            } else {
                hivProgram.setDateEnrolled(convertStringToDate.convert(rowData.get(3).toString()));
            }
            workflowService.savePatientProgram(hivProgram);
        }

        String enrolledInTb = rowData.get(14).toString();//enroll in HIV Program
        PatientProgram tbProgram = new PatientProgram();
        if (enrolledInTb != "") {
            tbProgram.setPatient(patient);
            tbProgram.setProgram(workflowService.getProgramByUuid("9f144a34-3a4a-44a9-8486-6b7af6cc64f6"));
            enrollInToTBProgram(patient, tbProgram, enrolledInTb);
        }

        /*enrollmentEncounter*/
        Encounter enrollmentEncounter = new Encounter();
        enrollmentEncounter.setPatient(patient);
        enrollmentEncounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        enrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        enrollmentEncounter.setLocation(defaultLocation);
        enrollmentEncounter.setDateCreated(new Date());
        enrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        if (rowData.get(3) == "") {
            enrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(2).toString()));
        } else {
            enrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(3).toString()));
        }

        /*Patient Obs during enrollment*/
        Obs entryPointObs = new Obs();//entry point
        entryPointObs.setObsDatetime(convertStringToDate.convert(rowData.get(21).toString()));
        entryPointObs.setPerson(patient);
        entryPointObs.setConcept(conceptService.getConceptByUuid("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String entryPointAnswer = rowData.get(10).toString();
        checkForValueCodedForEntryPoint(entryPointObs, entryPointAnswer);
        enrollmentEncounter.addObs(entryPointObs);

        Obs transferInObs = new Obs();//transfer in
        transferInObs.setObsDatetime(convertStringToDate.convert(rowData.get(21).toString()));
        transferInObs.setPerson(patient);
        transferInObs.setConcept(conceptService.getConceptByUuid("160563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String isTransferAnswer = rowData.get(1).toString();
        checkForValueCodedForIsTransferIn(transferInObs, isTransferAnswer, rowData, patient, enrollmentEncounter);
        enrollmentEncounter.addObs(transferInObs);

        Obs dateConfirmedHivObs = new Obs();//Date confirmed HIV+
        dateConfirmedHivObs.setObsDatetime(convertStringToDate.convert(rowData.get(21).toString()));
        dateConfirmedHivObs.setPerson(patient);
        dateConfirmedHivObs.setConcept(conceptService.getConceptByUuid("160554AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateConfirmedHivObs.setValueDate(convertStringToDate.convert(rowData.get(11).toString()));
        enrollmentEncounter.addObs(dateConfirmedHivObs);

        encounterService.saveEncounter(enrollmentEncounter);//saving the enrollmentEncounter

         /*consultationEncounter*/
        Encounter consultationEncounter = new Encounter();
        consultationEncounter.setPatient(patient);
        consultationEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        consultationEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        consultationEncounter.setLocation(locationService.getLocationByUuid("f2904f27-f35f-41aa-aad1-eb7325cf72f6"));
        consultationEncounter.setDateCreated(new Date());
        consultationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));

        if (rowData.get(3) == "") {
            consultationEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(2).toString()));
        } else {
            consultationEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(3).toString()));
        }

        Obs dateArtStartedObs = new Obs();//date art started
        dateArtStartedObs.setObsDatetime(convertStringToDate.convert(rowData.get(20).toString()));
        dateArtStartedObs.setPerson(patient);
        dateArtStartedObs.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateArtStartedObs.setValueDate(convertStringToDate.convert(rowData.get(20).toString()));
        consultationEncounter.addObs(dateArtStartedObs);

        Obs whoStageObs = new Obs();//World Health Organization HIV stage
        whoStageObs.setPerson(patient);
        whoStageObs.setLocation(defaultLocation);
        whoStageObs.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String whoStageAnswer = rowData.get(17).toString();
        if (rowData.get(17) != "") {
            String[] whoStage = whoStageAnswer.split("\\n");
            whoStageObs.setObsDatetime(convertStringToDate.convert(whoStage[1]));
        }
        checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
        consultationEncounter.addObs(whoStageObs);

        encounterService.saveEncounter(consultationEncounter);//saving the consultationEncounter

         /*hivLastClinicalEncounter*/
        Encounter hivLastClinicalEncounter = new Encounter();
        hivLastClinicalEncounter.setPatient(patient);
        hivLastClinicalEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        hivLastClinicalEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        getLocation(hivLastClinicalEncounter, rowData);
        hivLastClinicalEncounter.setDateCreated(new Date());
        hivLastClinicalEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        hivLastClinicalEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(21).toString()));

        Obs lastReturnToClinicObs = new Obs();//Last return to clinic obs
        lastReturnToClinicObs.setObsDatetime(convertStringToDate.convert(rowData.get(21).toString()));
        lastReturnToClinicObs.setPerson(patient);
        lastReturnToClinicObs.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        lastReturnToClinicObs.setValueDate(convertStringToDate.convert(rowData.get(23).toString()));
        hivLastClinicalEncounter.addObs(lastReturnToClinicObs);

        encounterService.saveEncounter(hivLastClinicalEncounter);//saving hivLastClinicalEncounter

        getCtxObs(rowData, patient);

        checkIfPregnant(patient, rowData);

        checkForArvEligibility(rowData, patient);
    }

    private void checkForArvEligibility(List<Object> rowData, Patient patient) throws ParseException {

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        encounter.setLocation(locationService.getLocationByUuid("f2904f27-f35f-41aa-aad1-eb7325cf72f6"));
        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        if (rowData.get(18) != "") {
            encounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(18).toString()));
        }

        Obs dateEligibleForArvObs = new Obs();//date eligible for ARVs
        dateEligibleForArvObs.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
        dateEligibleForArvObs.setPerson(patient);
        dateEligibleForArvObs.setConcept(conceptService.getConceptByUuid("162227AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateEligibleForArvObs.setValueDate(convertStringToDate.convert(rowData.get(18).toString()));
        if (rowData.get(18) != "") {
            encounter.addObs(dateEligibleForArvObs);
        }

        Obs whoStageObs = new Obs();//WHO Stage Obs
        whoStageObs.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
        whoStageObs.setPerson(patient);
        whoStageObs.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        Obs cd4PercentObs = new Obs();
        cd4PercentObs.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
        cd4PercentObs.setPerson(patient);
        cd4PercentObs.setConcept(conceptService.getConceptByUuid("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        Obs cd4CountObs = new Obs();
        cd4CountObs.setObsDatetime(convertStringToDate.convert(rowData.get(18).toString()));
        cd4CountObs.setPerson(patient);
        cd4CountObs.setConcept(conceptService.getConceptByUuid("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        getReasonForArvsEligibility(patient, whoStageObs, cd4PercentObs, cd4CountObs, rowData);

        if (rowData.get(18) != "") {
            encounter.addObs(whoStageObs);
            encounter.addObs(cd4PercentObs);
            encounter.addObs(cd4CountObs);
        }

        if (rowData.get(19) != "") {
            encounterService.saveEncounter(encounter);
        }
    }

    private void getReasonForArvsEligibility(Patient patient, Obs whoStageObs, Obs cd4PercentObs, Obs cd4CountObs, List<Object> rowData) throws ParseException {
        String[] arvEligibility;
        String[] arvReasons;
        String whoStageAnswer;

        if (rowData.get(19) != "") {
            arvEligibility = rowData.get(19).toString().split("\\n");
            whoStageAnswer = arvEligibility[1];

            if (arvEligibility[0].contains("+")) {

                if (arvEligibility[0].contains("CD4") && arvEligibility[0].contains("HIV DNA PCR")) {//if CD4

                    enrollInToMch_csProgram(rowData, patient);

                    if (arvEligibility.length == 4) {
                        if (arvEligibility[2].contains("%")) {//if CD4 %
                            String[] cd4Percent = arvEligibility[2].trim().split("\\:");

                            cd4PercentObs.setValueNumeric(Double.valueOf(cd4Percent[1]));
                            checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                        } else {//else CD4 count

                            String[] cd4Count = arvEligibility[2].trim().split("\\:");

                            cd4CountObs.setValueNumeric(Double.valueOf(cd4Count[1]));
                            checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                        }

                    } else if (arvEligibility.length == 5) {
                        String[] cd4Count = arvEligibility[2].trim().split("\\:");
                        String[] cd4Percent = arvEligibility[3].trim().split("\\:");

                        cd4PercentObs.setValueNumeric(Double.valueOf(cd4Percent[1]));
                        cd4CountObs.setValueNumeric(Double.valueOf(cd4Count[1]));
                        checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                    }
                } else if (arvEligibility[0].contains("CD4")) {//if CD4
                    if (arvEligibility.length == 3) {
                        if (arvEligibility[2].contains("%")) {//if CD4 %
                            String[] cd4Percent = arvEligibility[2].trim().split("\\:");

                            cd4PercentObs.setValueNumeric(Double.valueOf(cd4Percent[1]));
                            checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                        } else {//else CD4 count

                            String[] cd4Count = arvEligibility[2].trim().split("\\:");

                            cd4CountObs.setValueNumeric(Double.valueOf(cd4Count[1]));
                            checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                        }

                    } else if (arvEligibility.length == 4) {
                        String[] cd4Count = arvEligibility[2].trim().split("\\:");
                        String[] cd4Percent = arvEligibility[3].trim().split("\\:");

                        cd4PercentObs.setValueNumeric(Double.valueOf(cd4Percent[1]));
                        cd4CountObs.setValueNumeric(Double.valueOf(cd4Count[1]));
                        checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                    }
                } else {//else HIV DNA PCR
                    String[] hivDnaPcr = arvEligibility[2].trim().split("\\:");

                    checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));
                    enrollInToMch_csProgram(rowData, patient);
                }

            } else {//clinical only and WHO Stage

                checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer, Double.valueOf(rowData.get(8).toString()));

            }

        }

    }

    private void enrollInToMch_csProgram(List<Object> rowData, Patient patient) throws ParseException {

        Encounter mch_csEnrollmentEncounter = new Encounter();
        mch_csEnrollmentEncounter.setPatient(patient);
        mch_csEnrollmentEncounter.setForm(formService.getFormByUuid("8553d869-bdc8-4287-8505-910c7c998aff"));
        mch_csEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("415f5136-ca4a-49a8-8db3-f994187c3af6"));
        mch_csEnrollmentEncounter.setLocation(defaultLocation);
        mch_csEnrollmentEncounter.setDateCreated(new Date());
        mch_csEnrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        mch_csEnrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(18).toString()));

        PatientProgram mch_csProgram = new PatientProgram();
        mch_csProgram.setPatient(patient);
        mch_csProgram.setProgram(workflowService.getProgramByUuid("c2ecdf11-97cd-432a-a971-cfd9bd296b83"));
        mch_csProgram.setDateEnrolled(convertStringToDate.convert(rowData.get(18).toString()));
        workflowService.savePatientProgram(mch_csProgram);
    }

    private void checkIfPregnant(Patient patient, List<Object> rowData) throws ParseException {
        String gender = rowData.get(9).toString();
        String isPregnant = rowData.get(15).toString();

        if (gender.contains("F")) {
            if (rowData.get(15) != "") {
                String[] obsMade = isPregnant.split("\\n");
                String[] firstObs = obsMade[0].trim().split("\\|");
                Date edd = convertStringToDate.convert(firstObs[0]);
                if (firstObs.length == 2) {
                    String referral = firstObs[1];
                }
                Encounter encounter = new Encounter();
                encounter.setPatient(patient);
                encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
                encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
                encounter.setLocation(defaultLocation);
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

    private void getLocation(Encounter hivLastClinicalEncounter, List<Object> rowData) {

        String location = rowData.get(22).toString();

        if (location.contains("Amase")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Amase Dispensary"));
        } else if (location.contains("LUKOLIS")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Lukolis Model Health Centre"));
        } else if (location.contains("Obekai")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Obekai Dispensary"));
        } else if (location.contains("Busia") || location.contains("Busia Module 1") || location.contains("Busia Module 2")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Busia District Hospital"));
        } else if (location.contains("Nambale")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Nambale Health Centre"));
        } else if (location.contains("Amukura")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Amukura Health Centre"));
        } else if (location.contains("Teso")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Teso District Hospital"));
        } else if (location.contains("MTRH Module 1") || location.contains("MTRH Module 2") || location.contains("MTRH Module 3") || location.contains("MTRH Module 4")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi Teaching Refferal Hospital"));
        } else if (location.contains("Kaptama (Friends) dispensary")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kaptama (Friends) Health Centre"));
        } else if (location.contains("Chulaimbo") || location.contains("Chulaimbo Module 1") || location.contains("Chulaimbo Module 2")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Chulaimbo Sub-District Hospital"));
        } else if (location.contains("Malaba")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Malaba Dispensary"));
        } else if (location.contains("Naitiri")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Naitiri Sub-District Hospital"));
        } else if (location.contains("Lupida")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Lupida Health Centre"));
        } else if (location.contains("Bumala A")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bumala A Health Centre"));
        } else if (location.contains("Bumala B")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bumala B Health Centre"));
        } else if (location.contains("Mukhobola")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Mukhobola Health Centre"));
        } else if (location.contains("Uasin Gishu District Hospital")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Uasin Gishu District Hospital"));
        } else if (location.contains("Iten")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Iten District Hospital"));
        } else if (location.contains("Burnt Forest")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Burnt Forest Rhdc (Eldoret East)"));
        } else if (location.contains("Kitale")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kitale District Hospital"));
        } else if (location.contains("ANGURAI")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Angurai Health Centre"));
        } else if (location.contains("Port Victoria")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Port Victoria Hospital"));
        } else if (location.contains("Mois Bridge")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi's Bridge Health Centre"));
        } else if (location.contains("Mosoriot")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Mosoriot Clinic"));
        } else if (location.contains("Turbo")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Turbo Health Centre"));
        } else if (location.contains("Madende Health Center")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Madende Dispensary"));
        } else if (location.contains("Makutano")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Makutano Dispensary"));
        } else if (location.contains("Kapenguria")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Kapenguria District Hospital"));
        } else if (location.contains("Webuye")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Webuye Health Centre"));
        } else if (location.contains("Osieko")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Osieko Dispensary"));
        } else if (location.contains("Moi University")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Moi University Health Centre"));
        } else if (location.contains("Milo")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Milo Health Centre"));
        } else if (location.contains("Sio Port")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Sio Port District Hospital"));
        } else if (location.contains("Huruma SDH")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Huruma District Hospital"));
        } else if (location.contains("BOKOLI")) {
            hivLastClinicalEncounter.setLocation(locationService.getLocation("Bokoli Hospital"));
        } else {
            hivLastClinicalEncounter.setLocation(defaultLocation);
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

        Encounter tbEnrollmentEncounter = new Encounter();
        tbEnrollmentEncounter.setPatient(patient);
        tbEnrollmentEncounter.setForm(formService.getFormByUuid("89994550-9939-40f3-afa6-173bce445c79"));
        tbEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("9d8498a4-372d-4dc4-a809-513a2434621e"));
        tbEnrollmentEncounter.setLocation(defaultLocation);
        tbEnrollmentEncounter.setDateCreated(new Date());
        tbEnrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        tbEnrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(dates1[0]));

        tbProgram.setDateEnrolled(convertStringToDate.convert(dates1[0]));
        workflowService.savePatientProgram(tbProgram);

        Obs tbTreatmentObs = new Obs();
        tbTreatmentObs.setObsDatetime(convertStringToDate.convert(dates1[0]));
        tbTreatmentObs.setConcept(conceptService.getConceptByUuid("1113AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        tbTreatmentObs.setValueDate(convertStringToDate.convert(dates1[0]));

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
                tbTreatmentDiscontinuationEncounter.setLocation(defaultLocation);
                tbTreatmentDiscontinuationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
                tbTreatmentDiscontinuationEncounter.setEncounterDatetime(convertStringToDate.convert(stopDate));

                Obs tbTreatmentDiscontinuationObs = new Obs();
                tbTreatmentDiscontinuationObs.setObsDatetime(convertStringToDate.convert(stopDate));
                tbTreatmentDiscontinuationObs.setConcept(conceptService.getConceptByUuid("159431AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                tbTreatmentDiscontinuationObs.setValueDate(convertStringToDate.convert(stopDate));

                tbTreatmentDiscontinuationEncounter.addObs(tbTreatmentDiscontinuationObs);
                encounterService.saveEncounter(tbTreatmentDiscontinuationEncounter);
            }
        }

    }

    private void getCtxObs(List<Object> rowData, Patient patient) throws ParseException {

        if (rowData.get(12) != "") {
            String[] ctxDates = rowData.get(12).toString().split("\\n");

            Encounter ctxEncounter = new Encounter();
            ctxEncounter.setPatient(patient);
            ctxEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
            ctxEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
            ctxEncounter.setLocation(defaultLocation);
            ctxEncounter.setDateCreated(new Date());
            ctxEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));

            Obs groupObs = new Obs();/*CTX Obs*/
            groupObs.setConcept(conceptService.getConceptByUuid("1442AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            groupObs.setPerson(patient);

            for (int j = 0; j < ctxDates.length; j++) {
                String[] dates = ctxDates[j].trim().split("-");
                String startDate = dates[0];

                if (startDate.contains("/")) {

                    ctxEncounter.setEncounterDatetime(convertStringToDate.convert(startDate));
                    groupObs.setObsDatetime(convertStringToDate.convert(startDate));

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
        if (startDate != "" && stopDate != "") {
            if (startDate.contains("/")) {
                long difference = formatter.parse(stopDate).getTime() - formatter.parse(startDate).getTime();
                differenceInDays = difference / (24 * 60 * 60 * 1000);
            }
        }
        return Double.valueOf(differenceInDays);
    }

    private void checkForValueCodedForWhoStage(Obs obs, String whoStageAnswer, Double age) {

        if (whoStageAnswer != "") {
            String[] whoStage = whoStageAnswer.split("\\n");

            if (whoStage[0].equals("WHO Stage 1")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

            } else if (whoStage[0].equals("WHO Stage 2")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1221AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("WHO Stage 3")) {
                if (age < 15) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1222AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("WHO Stage 4")) {
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

    private void checkForValueCodedForIsTransferIn(Obs obs, String isTransferAnswer, List<Object> rowData, Patient patient, Encounter enrollmentEncounter) throws ParseException {

        if (isTransferAnswer.equals("Transfer In")) {
            obs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            Obs transferInDateObs = new Obs();
            transferInDateObs.setObsDatetime(convertStringToDate.convert(rowData.get(2).toString()));
            transferInDateObs.setPerson(patient);
            transferInDateObs.setConcept(conceptService.getConceptByUuid("160534AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInDateObs.setValueDate(convertStringToDate.convert(rowData.get(2).toString()));

            enrollmentEncounter.addObs(transferInDateObs);
        } else {
            obs.setValueCoded(conceptService.getConceptByUuid("1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
    }

    private void checkForValueCodedForEntryPoint(Obs obs, String entryPointAnswer) {

        if (entryPointAnswer.equals("VCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160539AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("PMTCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("MCH")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("TB")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160541AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (entryPointAnswer.equals("HCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159938AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else {

            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }

    }

}
