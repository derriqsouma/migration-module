package org.openmrs.module.migrate.kisii;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;
import org.openmrs.module.migrate.ConvertStringToDate1;
import org.openmrs.module.migrate.maragua.ReadExcelSheetMaragua;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by derric on 3/1/15.
 */
public class PatientsInfo {
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
    public PatientsInfo(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetMaragua readExcelSheetMaragua = new ReadExcelSheetMaragua(path, session, kenyaUi);
        sheetData = readExcelSheetMaragua.readExcelSheet();

        savePatientInfo(sheetData);

    }

    private void savePatientInfo(List<List<Object>> sheetData) throws ParseException {

        for (int i = 0; i < sheetData.size(); i++) {

            List<Object> rowData = sheetData.get(i);
            String[] firstName;
            String[] lastNames;
            String fName = null, mName = null, lName = null;
            String gender = null;
            Date dob = null;

            lastNames = String.valueOf(rowData.get(2)).replaceAll("\\s+", " ").trim().split(" ");
            firstName = String.valueOf(rowData.get(1)).replaceAll("\\s+", " ").trim().split(" ");

            if (lastNames.length < 1 || firstName.length < 1 || rowData.get(3).toString().isEmpty()) {
                System.out.println("\n\n\n Patient not saved: " + rowData.get(2).toString() + " " + rowData.get(0).toString() + " \n\n\n");
                continue;
            } else {
                if (rowData.get(6).toString().equalsIgnoreCase("1.0")) {
                    gender = "M";
                }
                if (rowData.get(6).toString().equalsIgnoreCase("2.0")) {
                    gender = "F";
                }
//                String[] dob1 = String.valueOf(rowData.get(3)).replaceAll("\\s+", " ").split(" ");
                dob = convertStringToDate.convert(rowData.get(3).toString());

                if (lastNames.length == 1) {
                    lName = lastNames[0];
                    if (firstName.length == 1) {
                        fName = firstName[0];
                    } else {
                        fName = firstName[0];
                        mName = firstName[1];
                    }
                } else {
                    fName = firstName[0];
                    mName = lastNames[0];
                    lName = lastNames[1];

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
               /* PatientIdentifierType openmrsIdType = patientService.getPatientIdentifierTypeByUuid("dfacd928-0370-4315-99d7-6ec1c9f7ae76");
                String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "migration");
                openmrsId.setIdentifierType(openmrsIdType);
                openmrsId.setDateCreated(new Date());
                openmrsId.setLocation(defaultLocation);
                openmrsId.setIdentifier(generated);
                openmrsId.setVoided(false);*/

                PatientIdentifier upn;
                if (rowData.get(0) != "") {
                    String[] upn1 = String.valueOf(rowData.get(0)).replaceAll("\\.", "").split("E");
                    upn = new PatientIdentifier();
                    upn.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a"));
                    upn.setDateCreated(new Date());
                    upn.setLocation(defaultLocation);
                    upn.setIdentifier(upn1[0]);
                    upn.setVoided(false);
                    upn.setPreferred(true);

//                    patient.addIdentifiers(Arrays.asList(upn, openmrsId));
                    patient.addIdentifier(upn);
                    if (!patientService.isIdentifierInUseByAnotherPatient(upn)) {
                        System.out.println("\n\n\n " + upn + "  " + patient.getGivenName() + " \n\n\n");
                        patientService.savePatient(patient);//saving the patient
                        savePatientObs(patient, rowData);
                        counter += 1;
                    } else {
                        kenyaUi.notifyError(session, "the patient identifier #" + upn + " already in use by another patient");
                        System.out.println("\n\n the patient identifier #" + upn + " already in use by another patient");
                        continue;
                    }

                }

            }
        }
        kenyaUi.notifySuccess(session, " " + counter + " patient(s) added");
        System.out.println(" \n\n" + counter + " patient(s) added");

    }

    private void savePatientObs(Patient patient, List<Object> rowData) throws ParseException {
        Encounter hivEnrollmentEncounter = new Encounter();
        Encounter consultationEncounter = new Encounter();

        consultationEncounter.setPatient(patient);
        consultationEncounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        consultationEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        consultationEncounter.setLocation(locationService.getDefaultLocation());
        consultationEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        consultationEncounter.setDateCreated(convertStringToDate.convert(rowData.get(50).toString()));
        consultationEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(50).toString()));

        hivEnrollmentEncounter.setPatient(patient);
        hivEnrollmentEncounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        hivEnrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        hivEnrollmentEncounter.setLocation(defaultLocation);
        hivEnrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        hivEnrollmentEncounter.setDateCreated(convertStringToDate.convert(rowData.get(50).toString()));
        hivEnrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(50).toString()));

        //entry point
        if (rowData.get(8) != "") {
            Obs entryPoint = new Obs();//entryPoint
            entryPoint.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            entryPoint.setPerson(patient);
            entryPoint.setConcept(conceptService.getConceptByUuid("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            checkForValueCodedForEntryPoint(entryPoint,rowData);
            Date date = convertStringToDate.convert(rowData.get(50).toString());
            if (date.before(new Date())) {
                hivEnrollmentEncounter.addObs(entryPoint);
            }
        }
        if (rowData.get(10) != "") {
            Obs dateFirstEnrolled = new Obs();//date first enrolled into hiv
            dateFirstEnrolled.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            dateFirstEnrolled.setPerson(patient);
            dateFirstEnrolled.setConcept(conceptService.getConceptByUuid("160555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (rowData.get(17) != "") {
                if (convertStringToDate.convert(rowData.get(17).toString()).before(new Date())) {
                    dateFirstEnrolled.setValueDate(convertStringToDate.convert(rowData.get(17).toString()));
                    hivEnrollmentEncounter.addObs(dateFirstEnrolled);
                }
            }

            Obs transferInObs = new Obs();//transfer in
            transferInObs.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            transferInObs.setPerson(patient);
            transferInObs.setConcept(conceptService.getConceptByUuid("160563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInObs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            hivEnrollmentEncounter.addObs(transferInObs);

            Obs transferInDateObs = new Obs();
            transferInDateObs.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            transferInDateObs.setPerson(patient);
            transferInDateObs.setConcept(conceptService.getConceptByUuid("160534AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInDateObs.setValueDate(convertStringToDate.convert(rowData.get(10).toString()));
            hivEnrollmentEncounter.addObs(transferInDateObs);

            Obs transferInDistrict = new Obs();
            transferInDistrict.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            transferInDistrict.setPerson(patient);
            transferInDistrict.setConcept(conceptService.getConceptByUuid("161551AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInDistrict.setValueText(rowData.get(11).toString());
            hivEnrollmentEncounter.addObs(transferInDistrict);

            Obs transferInFrom = new Obs();
            transferInFrom.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            transferInFrom.setPerson(patient);
            transferInFrom.setConcept(conceptService.getConceptByUuid("160535AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInFrom.setValueText(rowData.get(12).toString());
            hivEnrollmentEncounter.addObs(transferInFrom);

            Obs transferInArtStartDate = new Obs();
            transferInArtStartDate.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
            transferInArtStartDate.setPerson(patient);
            transferInArtStartDate.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            transferInArtStartDate.setValueDate(convertStringToDate.convert(rowData.get(13).toString()));
            if (rowData.get(13) != "") {
                hivEnrollmentEncounter.addObs(transferInArtStartDate);
            }
        }

        visit(patient,rowData,consultationEncounter);

        if (rowData.get(50) != "") {
            encounterService.saveEncounter(hivEnrollmentEncounter);
            encounterService.saveEncounter(consultationEncounter);
        }
        enrollIntoHiv(patient,rowData);
        saveDrugOrders(patient,rowData);

    }

    private void enrollIntoHiv(Patient patient, List<Object> rowData) throws ParseException {
        String[]encounterDate = String.valueOf(rowData.get(50)).replaceAll("\\s+", " ").split(" ");
        String[] enrollmentDate = String.valueOf(rowData.get(17)).replaceAll("\\s+", " ").split(" ");

        //enroll into hiv
        PatientProgram hivProgram = new PatientProgram();

        hivProgram.setPatient(patient);
        hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
        if (rowData.get(17) != "") {

            hivProgram.setDateEnrolled(convertStringToDate.convert(enrollmentDate[0]));
            workflowService.savePatientProgram(hivProgram);
        }
    }

    private void checkForValueCodedForEntryPoint(Obs obs, List<Object> rowData) {

        if (rowData.get(8).toString().equals("2.0")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160539AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (rowData.get(8).toString().equals("1.0")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (rowData.get(8).toString().equals("MCH")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (rowData.get(8).toString().equals("3.0")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160541AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (rowData.get(8).toString().equals("6.0")) {

            obs.setValueCoded(conceptService.getConceptByUuid("160542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else if (rowData.get(8).toString().equals("HCT")) {

            obs.setValueCoded(conceptService.getConceptByUuid("159938AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        } else {

            obs.setValueCoded(conceptService.getConceptByUuid("5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }

    }

    private void visit(Patient patient, List<Object> rowData, Encounter encounter) throws ParseException {
        Obs whoStage = new Obs();
        whoStage.setPerson(patient);
        whoStage.setLocation(defaultLocation);
        whoStage.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        whoStage.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        checkForValueCodedForWhoStage(whoStage, rowData);
        encounter.addObs(whoStage);

        Obs dateEligibleForArvObs = new Obs();//date eligible for ARVs
        dateEligibleForArvObs.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        dateEligibleForArvObs.setPerson(patient);
        dateEligibleForArvObs.setConcept(conceptService.getConceptByUuid("162227AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateEligibleForArvObs.setValueDate(convertStringToDate.convert(rowData.get(23).toString()));
        if (rowData.get(23) != "") {
            encounter.addObs(dateEligibleForArvObs);
        }

        Obs cd4PercentObs = new Obs();
        cd4PercentObs.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        cd4PercentObs.setPerson(patient);
        cd4PercentObs.setConcept(conceptService.getConceptByUuid("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(31) != "") {
            cd4PercentObs.setValueNumeric(Double.valueOf(rowData.get(31).toString()));
        }
        encounter.addObs(cd4PercentObs);

        Obs cd4CountObs = new Obs();
        cd4CountObs.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        cd4CountObs.setPerson(patient);
        cd4CountObs.setConcept(conceptService.getConceptByUuid("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(30) != "") {
            cd4CountObs.setValueNumeric(Double.valueOf(rowData.get(30).toString()));
        }
        encounter.addObs(cd4CountObs);


        Obs weight = new Obs();//weight
        weight.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        weight.setPerson(patient);
        weight.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(35) != "") {
            weight.setValueNumeric(Double.parseDouble(rowData.get(35).toString()));
        }
        encounter.addObs(weight);

        Obs height = new Obs();//height
        height.setObsDatetime(convertStringToDate.convert(rowData.get(50).toString()));
        height.setPerson(patient);
        height.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(36) != "") {
            height.setValueNumeric(Double.parseDouble(rowData.get(36).toString()));
        }
        encounter.addObs(height);

    }

    private void checkForValueCodedForWhoStage(Obs obs, List<Object> rowData) {

        if (rowData.get(18) != "") {
            Double age = 0.0;
            String str =  rowData.get(18).toString();
            if (rowData.get(4) != "") {
                age = Double.valueOf(rowData.get(4).toString());
            }

            if (str.equals("1.0")) {
                if (age < 15 && age !=0.0) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

            } else if (str.equals("2.0")) {
                if (age < 15 && age !=0.0) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1221AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (str.equals("3.0")) {
                if (age < 15 && age !=0.0) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1222AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (str.equals("4.0")) {
                if (age < 15 && age !=0.0) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1223AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                } else {
                    obs.setValueCoded(conceptService.getConceptByUuid("1207AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else {

                obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
    }

    private void saveDrugOrders(Patient patient, List<Object> rowData) throws ParseException {
        if (rowData.get(32) != "" && rowData.get(33) != "") {

                String str = getRegimen(rowData.get(33).toString());
                if (str != null) {
                    String[] firstRegimen = str.split("\\+");

                    for (int i = 0; i < firstRegimen.length; i++) {
                        DrugOrder currentDrugOrder = new DrugOrder();

                        currentDrugOrder.setPatient(patient);
                        currentDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
                        currentDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(32).toString()));
                        currentDrugOrder.setFrequency("");
                        currentDrugOrder.setDose(300.0);
                        currentDrugOrder.setUnits("mg");

                        if (firstRegimen[i] != null) {
                            setDrugsConcepts(currentDrugOrder, firstRegimen[i]);
                            Context.getOrderService().saveOrder(currentDrugOrder);
                        }

                    }
                }


            if (rowData.get(38) != "" && rowData.get(39) != "") {


                    String str1 = getRegimen(rowData.get(39).toString());
                    if (str1 != null) {
                        String[] secondLineRegimen = str1.split("\\+");

                        List<DrugOrder> drugOrderList = Context.getOrderService().getDrugOrdersByPatient(patient);
                        if (!drugOrderList.isEmpty()) {
                            for (DrugOrder drugOrder : drugOrderList) {
                                drugOrder.setDiscontinued(true);
                                drugOrder.setDiscontinuedDate(convertStringToDate.convert(rowData.get(38).toString()));
                                getRegimenSwitchReason(drugOrder, rowData.get(41).toString());
                            }

                            for (int i = 0; i < secondLineRegimen.length; i++) {
                                DrugOrder lastDrugOrder = new DrugOrder();

                                lastDrugOrder.setPatient(patient);
                                lastDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
                                lastDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(38).toString()));
                                lastDrugOrder.setFrequency("");
                                lastDrugOrder.setDose(300.00);
                                lastDrugOrder.setUnits("mg");

                                if (secondLineRegimen[i] != null) {
                                    setDrugsConcepts(lastDrugOrder, secondLineRegimen[i]);
                                    Context.getOrderService().saveOrder(lastDrugOrder);
                                }

                            }
                        }
                    }

            }
        }
    }

    private void getRegimenSwitchReason(DrugOrder drugOrder, String s) {

        if (s.contains("1.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("843AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("2.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160566AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("3.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160569AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else {
            drugOrder.setDiscontinuedReasonNonCoded("");
        }
    }

    private String getRegimen(String s) {
        String regimen = null;
        if (s.equalsIgnoreCase("1.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("2.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("3.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("4.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("5.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("6.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("7.0")){
            regimen = "AZT+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("8.0")){
            regimen = "AZT+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("9.0")){
            regimen = "AZT+DDI+NFV";
        }else if (s.equalsIgnoreCase("10.0")){
            regimen = "AZT+DDI+NFV";
        }else if (s.equalsIgnoreCase("11.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("12.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("13.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("14.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("15.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("16.0")){
            regimen = "AZT+3TC+IDV/r";
        }else if (s.equalsIgnoreCase("17.0")){
            regimen = "AZT+DDL+IDV/r";
        }else if (s.equalsIgnoreCase("18.0")){
            regimen = "AZT+DDL+IDV/r";
        }else if (s.equalsIgnoreCase("19.0")){
            regimen = "TDF+ABC+LPV/r";
        }else if (s.equalsIgnoreCase("33.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("34.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("54.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("21.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("22.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("23.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("24.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("26.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("27.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("28.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("29.0")){
            regimen = "D4T+ABC+LPV/r";
        }else if (s.equalsIgnoreCase("30.0")){
            regimen = "D4T+ABC+NFV";
        }else if (s.equalsIgnoreCase("35.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("38.0")){
            regimen = "TDF+3TC+NVP";
        }else if (s.equalsIgnoreCase("39.0")){
            regimen = "TDF+3TC+EFV";
        }else if (s.equalsIgnoreCase("40.0")){
            regimen = "ABC+3TC+EFV";
        }else if (s.equalsIgnoreCase("42.0")){
            regimen = "ABC+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("43.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("44.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("45.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("46.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("47.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("48.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("49.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("50.0")){
            regimen = "ABC+3TC+AZT";
        }else if (s.equalsIgnoreCase("51.0")){
            regimen = "ABC+3TC+NVP";
        }else if (s.equalsIgnoreCase("52.0")){
            regimen = "ABC+3TC+NVP";
        }

        return regimen;
    }

    private void setDrugsConcepts(DrugOrder drugOrder, String regimen) {


        if (regimen.contains("3TC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("78643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ABC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("70056AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if (regimen.contains("AZT")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("86663AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
        else if (regimen.contains("DDI")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("74807AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("EFV")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("75523AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("LPV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("794AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("NVP")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("80586AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("IDV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("77995AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("D4T")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84309AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("TDF")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84795AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("IDV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("77995AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("NELFINAVIR'UNK")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("80487AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("EMTRICITABINE")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("75628AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ATAZANAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("71647AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ETRAVIRINE")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("159810AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("RALTEGRAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("154378AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if (regimen.equalsIgnoreCase("NFV")){
            drugOrder.setConcept(conceptService.getConceptByUuid(""));
        }
    }


}
