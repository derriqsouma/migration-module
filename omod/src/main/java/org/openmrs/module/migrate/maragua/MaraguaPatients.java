package org.openmrs.module.migrate.maragua;

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
public class MaraguaPatients {
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
    public MaraguaPatients(String path, HttpSession session, KenyaUiUtils kenyaUi) {
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
            String[] fullNames;
            String fName = "", mName = "", lName = "";
            String gender = null;
            Date dob = null;

            fullNames = String.valueOf(rowData.get(8)).replaceAll("\\s+", " ").trim().split(" ");

            if (fullNames.length <= 1 ){
                System.out.println("\n\n\n Error  "+ rowData.get(8).toString() +" \n\n\n");
                continue;
            } else {
                if (rowData.get(9).toString().equalsIgnoreCase("0.0")) {
                    gender = "M";
                }
                if (rowData.get(9).toString().equalsIgnoreCase("1.0")) {
                    gender = "F";
                }
                if (rowData.get(10).toString().isEmpty()) {
                    if (rowData.get(11) != "") {
                        String[] dob1 = String.valueOf(rowData.get(1)).replaceAll("\\s+", " ").split(" ");
                        dob = getBirthDate(dob1[0], Double.parseDouble((rowData.get(11).toString())));
                    }
                }else {
                    String[] dob1 = String.valueOf(rowData.get(10)).replaceAll("\\s+", " ").split(" ");
                    dob = convertStringToDate.convert(dob1[0]);
                }

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
              /*  PatientIdentifierType openmrsIdType = patientService.getPatientIdentifierTypeByUuid("dfacd928-0370-4315-99d7-6ec1c9f7ae76");
                String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "migration");
                openmrsId.setIdentifierType(openmrsIdType);
                openmrsId.setDateCreated(new Date());
                openmrsId.setLocation(defaultLocation);
                openmrsId.setIdentifier(generated);
                openmrsId.setVoided(false);*/


                PatientIdentifier upn;
                if (rowData.get(0) != "") {
                    String[] upn1 = String.valueOf(rowData.get(0)).split("\\.");
                    upn = new PatientIdentifier();
                    upn.setIdentifierType(patientService.getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a"));
                    upn.setDateCreated(new Date());
                    upn.setLocation(defaultLocation);
                    upn.setIdentifier(upn1[0]);
                    upn.setVoided(false);
                    upn.setPreferred(true);

                    patient.addIdentifier(upn);
                    if (!patientService.isIdentifierInUseByAnotherPatient(upn)) {
                        System.out.println("\n\n\n "+ upn + "  " + patient.getGivenName() + " \n\n\n");
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

    private void savePatientObs(Patient patient, List<Object> rowData) throws ParseException {
        String[] enrollmentDate = String.valueOf(rowData.get(1)).replaceAll("\\s+", " ").split(" ");

        enrollIntoHiv(patient, rowData);

        /*enrollmentEncounter*/
        Encounter enrollmentEncounter = new Encounter();
        enrollmentEncounter.setPatient(patient);
        enrollmentEncounter.setForm(formService.getFormByUuid("37f6bd8d-586a-4169-95fa-5781f987fe62"));
        enrollmentEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("d1059fb9-a079-4feb-a749-eedd709ae542"));
        enrollmentEncounter.setLocation(defaultLocation);
        enrollmentEncounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
        enrollmentEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        enrollmentEncounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));


        if (rowData.get(19).toString().contains("1") && rowData.get(20) != "") {
            String[] date = String.valueOf(rowData.get(20)).replaceAll("\\s+", " ").split(" ");
            Obs dead = new Obs();//dead
            dead.setObsDatetime(convertStringToDate.convert(date[0]));
            dead.setPerson(patient);
            dead.setConcept(conceptService.getConceptByUuid("160432AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            dead.setValueCoded(conceptService.getConceptByUuid("160432AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            if (convertStringToDate.convert(date[0]).before(new Date())) {
                enrollmentEncounter.addObs(dead);
            }

        }
        if (convertStringToDate.convert(enrollmentDate[0].toString()).before(new Date())) {
            encounterService.saveEncounter(enrollmentEncounter);//saving the enrollmentEncounter
        }

    }

    private void enrollIntoHiv(Patient patient, List<Object> rowData) throws ParseException {
        String[] enrollmentDate = String.valueOf(rowData.get(16)).replaceAll("\\s+", " ").split(" ");
        //enroll into hiv
        PatientProgram hivProgram = new PatientProgram();
        Encounter encounter = new Encounter();

        if (rowData.get(16) != "") {
            encounter.setPatient(patient);
            encounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
            encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
            encounter.setLocation(defaultLocation);
            encounter.setDateCreated(convertStringToDate.convert(enrollmentDate[0].toString()));
            encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
            encounter.setEncounterDatetime(convertStringToDate.convert(enrollmentDate[0].toString()));

            if (rowData.get(12).toString().contains("1") && rowData.get(13) != "") {
                String[] date = String.valueOf(rowData.get(13)).replaceAll("\\s+", " ").split(" ");
                Obs transferInObs = new Obs();//transfer in
                transferInObs.setObsDatetime(convertStringToDate.convert(date[0]));
                transferInObs.setPerson(patient);
                transferInObs.setConcept(conceptService.getConceptByUuid("160563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                transferInObs.setValueCoded(conceptService.getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

                if(convertStringToDate.convert(date[0]).before(new Date())) {
                    encounter.addObs(transferInObs);
                }

                Obs transferInDateObs = new Obs();
                transferInDateObs.setObsDatetime(convertStringToDate.convert(date[0]));
                transferInDateObs.setPerson(patient);
                transferInDateObs.setConcept(conceptService.getConceptByUuid("160534AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                transferInDateObs.setValueDate(convertStringToDate.convert(date[0]));

                if(convertStringToDate.convert(date[0]).before(new Date())) {
                    encounter.addObs(transferInDateObs);
                }
            }
            if(convertStringToDate.convert(enrollmentDate[0].toString()).before(new Date())) {
                encounterService.saveEncounter(encounter);
            }
            else {
                System.out.println("\n\n Date \n\n\n");
            }

            hivProgram.setPatient(patient);
            hivProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
            hivProgram.setDateEnrolled(convertStringToDate.convert(enrollmentDate[0].toString()));
            workflowService.savePatientProgram(hivProgram);
        }
    }


}
