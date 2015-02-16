package org.openmrs.module.migrate.kakuma;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;
import org.openmrs.module.migrate.ConvertStringToDate1;
import org.openmrs.module.migrate.ReadExcelSheet;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by derric on 2/12/15.
 */
public class KakumaPatients {
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
    ConvertStringToDate1 convertStringToDate = new ConvertStringToDate1();
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    //constructor
    public KakumaPatients(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetKakuma readExcelSheetKakuma = new ReadExcelSheetKakuma(path, session, kenyaUi);
        sheetData = readExcelSheetKakuma.readExcelSheet();

        savePatientInfo(sheetData);

    }

    private void savePatientInfo(List<List<Object>> sheetData) throws ParseException {

        for (int i = 0; i < sheetData.size(); i++) {

            List<Object> rowData = sheetData.get(i);
            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = null;
            fullNames = String.valueOf(rowData.get(2)).replaceAll("\\s+", " ").split(" ");

            if (fullNames.length <= 1 || rowData.get(2).toString().isEmpty() || rowData.get(7).toString().isEmpty() || rowData.get(3).toString().isEmpty() || rowData.get(9).toString().isEmpty()) {
               System.out.println("\n\n\n Error \n\n\n");
                continue;
            } else {
                if (rowData.get(7).toString().contains("MALE")) {
                    gender = "M";
                }
                if (rowData.get(7).toString().contains("FEMALE")) {
                    gender = "F";
                }
                String[] dob1 = String.valueOf(rowData.get(9)).replaceAll("\\s+", " ").split(" ");
                Date dob = getBirthDate(dob1[0], Double.parseDouble((rowData.get(3).toString())));

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


                PatientIdentifier upn = null;
                if (rowData.get(1) != "") {
                    upn = new PatientIdentifier();
                    upn.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a"));
                    upn.setDateCreated(new Date());
                    upn.setLocation(defaultLocation);
                    upn.setIdentifier(rowData.get(1).toString().replaceAll("[^\\d]", ""));
                    upn.setVoided(false);
                    upn.setPreferred(true);

                    patient.addIdentifiers(Arrays.asList(upn, openmrsId));
                    if (!patientService.isIdentifierInUseByAnotherPatient(upn)) {
                        System.out.println("\n\n\n "+ upn +" \n\n\n");
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

        String[] enrollmentDate = String.valueOf(rowData.get(9)).replaceAll("\\s+", " ").split(" ");
        enrollIntoHiv(patient, rowData, enrollmentDate);

        /*enrollmentEncounter*/
        Encounter enrollmentEncounter = new Encounter();
        enrollmentEncounter.setPatient(patient);
        enrollmentEncounter.setForm(formService.getFormByUuid("37f6bd8d-586a-4169-95fa-5781f987fe62"));
        enrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("d1059fb9-a079-4feb-a749-eedd709ae542"));
        enrollmentEncounter.setLocation(defaultLocation);
        enrollmentEncounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
        enrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        enrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));

        Obs weight = new Obs();//weight
        weight.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        weight.setPerson(patient);
        weight.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(4) != "") {
            weight.setValueNumeric(Double.parseDouble(rowData.get(4).toString()));
        }
        enrollmentEncounter.addObs(weight);

        Obs height = new Obs();//height
        height.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        height.setPerson(patient);
        height.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(5) != "") {
            height.setValueNumeric(Double.parseDouble(rowData.get(5).toString()));
        }
        enrollmentEncounter.addObs(height);

        encounterService.saveEncounter(enrollmentEncounter);//saving the enrollmentEncounter

        visits(patient, rowData, enrollmentDate);
        saveDrugOrders(patient,rowData);

    }

    private void enrollIntoHiv(Patient patient, List<Object> rowData, String[] enrollmentDate) throws ParseException {
        //enroll into hiv
        PatientProgram hivProgram = new PatientProgram();
        Encounter encounter = new Encounter();

        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        encounter.setLocation(defaultLocation);
        encounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));


        Obs dateConfirmedHivObs = new Obs();//Date confirmed HIV+
        dateConfirmedHivObs.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        dateConfirmedHivObs.setPerson(patient);
        dateConfirmedHivObs.setConcept(conceptService.getConceptByUuid("160554AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        dateConfirmedHivObs.setValueDate(convertStringToDate.convert(enrollmentDate[0].toString()));
        encounter.addObs(dateConfirmedHivObs);
        encounterService.saveEncounter(encounter);

        hivProgram.setPatient(patient);
        hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
        hivProgram.setDateEnrolled(convertStringToDate.convert(enrollmentDate[0].toString()));
        workflowService.savePatientProgram(hivProgram);
    }

    private void visits(Patient patient, List<Object> rowData, String[] enrollmentDate) throws ParseException {

        /*consultationEncounter*/
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        encounter.setLocation(locationService.getDefaultLocation());
        encounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));

        if (rowData.get(10) != "") {

            Obs dateArtStartedObs = new Obs();//date art started
            String[] artStartDate = String.valueOf(rowData.get(10)).replaceAll("\\s+", " ").split(" ");

            dateArtStartedObs.setObsDatetime(convertStringToDate.convert(artStartDate[0].toString()));
            dateArtStartedObs.setPerson(patient);
            dateArtStartedObs.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            dateArtStartedObs.setValueDate(convertStringToDate.convert(artStartDate[0].toString()));
            encounter.addObs(dateArtStartedObs);
        }

        if (rowData.get(24) != "") {
            Obs nextAppointment = new Obs();//Last return to clinic obs
            String[] returnDate = String.valueOf(rowData.get(24)).replaceAll("\\s+", " ").split(" ");

            nextAppointment.setObsDatetime(convertStringToDate.convert(returnDate[0].toString()));
            nextAppointment.setPerson(patient);
            nextAppointment.setConcept(conceptService.getConceptByUuid("5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            nextAppointment.setValueDate(convertStringToDate.convert(returnDate[0].toString()));
            encounter.addObs(nextAppointment);
        }

        Obs weight = new Obs();//weight
        weight.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        weight.setPerson(patient);
        weight.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(4) != "") {
            weight.setValueNumeric(Double.parseDouble(rowData.get(4).toString()));
        }
        encounter.addObs(weight);

        Obs height = new Obs();//height
        height.setObsDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));
        height.setPerson(patient);
        height.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(5) != "") {
            height.setValueNumeric(Double.parseDouble(rowData.get(5).toString()));
        }
        encounter.addObs(height);

        encounterService.saveEncounter(encounter);

    }

    private void saveDrugOrders(Patient patient, List<Object> rowData) throws ParseException {
        String[] regimen = rowData.get(12).toString().split("\\(");
        String[] regimen1 = rowData.get(13).toString().split("\\(");

        String[] firstRegimen = regimen[0].toString().split("\\+");
        String[] lastRegimen = regimen1[0].toString().split("\\+");

        if (firstRegimen.length > 1) {
            for (int i = 0; i < firstRegimen.length; i++) {
                DrugOrder currentDrugOrder = new DrugOrder();

                currentDrugOrder.setPatient(patient);
                currentDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
                if (rowData.get(11) != "") {
                    currentDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(11).toString()));
                }
                currentDrugOrder.setFrequency("");
                currentDrugOrder.setDose(300.0);
                currentDrugOrder.setUnits("mg");

                if (firstRegimen[i] != null) {
                    setDrugsConcepts(currentDrugOrder, firstRegimen[i]);
                    Context.getOrderService().saveOrder(currentDrugOrder);
                }

            }
        }

    }

    private void setDrugsConcepts(DrugOrder drugOrder, String regimen) {


        if (regimen.contains("3TC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("78643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ABC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("70056AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if (regimen.contains("AZT")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("86663AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
        else if (regimen.contains("DIDANOSINE")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("74807AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("EFV")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("75523AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("AZT")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("86663AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("NVP")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("80586AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("LOPINAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("79040AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("D4T")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84309AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("TDF")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84795AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("RITONAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("83412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

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

        }
    }

    private Date getBirthDate(String dateInString, Double age) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calendar = Calendar.getInstance();
        Date date = null;
        if (dateInString != "") {
            date = formatter.parse(dateInString);
            int years = (int) Math.round(age);
            calendar.setTime(date);
            calendar.set(Calendar.YEAR, (calendar.get(Calendar.YEAR) - years));
            date = calendar.getTime();
        }
        return date;
    }


}
