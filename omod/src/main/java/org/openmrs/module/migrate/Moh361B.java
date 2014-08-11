package org.openmrs.module.migrate;

import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 7/14/14.
 */
public class Moh361B {
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
    PatientService patientService = Context.getPatientService();
    ConvertStringToDate convertStringToDate = new ConvertStringToDate();
    Patient patient = new Patient();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public Moh361B(String path,HttpSession session, KenyaUiUtils kenyaUi){
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void initMoh361B() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheet readExcelSheet = new ReadExcelSheet(path,session,kenyaUi);
        sheetData = readExcelSheet.readExcelSheet();

        checkForPatient(sheetData);

    }
    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            patient = null;
            List<Object> rowData = sheetData.get(i);
            String upn = rowData.get(2).toString().replaceAll("[^\\d]", "");
            String amrId = rowData.get(5).toString();

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);
            List<Patient> patientListUsingAmrId = patientService.getPatients(null, amrId, null, true);

            if (patientListUsingUpn.size() > 0 || patientListUsingAmrId.size() > 0) {
                if (upn.isEmpty()){
                    patient = patientService.getPatient(patientListUsingAmrId.get(0).getPatientId());
                    savePatientObsAtArvStart(patient,rowData);
//                    saveDrugOrders(patient,rowData);
                }else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    savePatientObsAtArvStart(patient,rowData);
//                    saveDrugOrders(patient,rowData);
                }
            }
        }
    }

    private void saveDrugOrders(Patient patient, List<Object> rowData) throws ParseException {

        String[] firstRegimen = rowData.get(15).toString().split("-");
        String[] lastRegimen = rowData.get(17).toString().split("-");

        for (int i = 0; i < firstRegimen.length; i++) {
            DrugOrder currentDrugOrder = new DrugOrder();

            currentDrugOrder.setPatient(patient);
            currentDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
            currentDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(1).toString()));
            currentDrugOrder.setFrequency("");
            currentDrugOrder.setDose(300.0);
            currentDrugOrder.setUnits("mg");

            if (firstRegimen[i] != null) {
                setDrugsConcepts(currentDrugOrder, firstRegimen[i]);
                Context.getOrderService().saveOrder(currentDrugOrder);
            }

        }

        if (rowData.get(17) != "") {
            if (rowData.get(15).toString().matches(rowData.get(17).toString())) {
//            List<DrugOrder>drugOrderList =  Context.getOrderService().getDrugOrdersByPatient(patient);
//            for(DrugOrder drugOrder : drugOrderList){
//                drugOrder.setDateChanged(convertToDate(rowData.get(16).toString()));
//            }

            } else {

                List<DrugOrder> drugOrderList = Context.getOrderService().getDrugOrdersByPatient(patient);
                for (DrugOrder drugOrder : drugOrderList) {
                    drugOrder.setDiscontinued(true);
                    drugOrder.setDiscontinuedDate(convertStringToDate.convert(rowData.get(16).toString()));
                    drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

                for (int i = 0; i < lastRegimen.length; i++) {
                    DrugOrder lastDrugOrder = new DrugOrder();

                    lastDrugOrder.setPatient(patient);
                    lastDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
                    lastDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(16).toString()));
                    lastDrugOrder.setFrequency("");
                    lastDrugOrder.setDose(300.00);
                    lastDrugOrder.setUnits("mg");

                    if (lastRegimen[i] != null) {
                        setDrugsConcepts(lastDrugOrder, lastRegimen[i]);
                        Context.getOrderService().saveOrder(lastDrugOrder);
                    }

                }
            }
        }
    }

    private void setDrugsConcepts(DrugOrder drugOrder, String regimen) {


        if (regimen.contains("LAMIVUDINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("78643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("ABACAVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("70056AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("DIDANOSINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("74807AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("EFAVIRENZ")){

            drugOrder.setConcept(conceptService.getConceptByUuid("75523AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("AZT")){

            drugOrder.setConcept(conceptService.getConceptByUuid("86663AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("NEVIRAPINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("80586AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("LOPINAVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("79040AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("STAVUDINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("84309AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("TENOFOVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("84795AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("RITONAVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("83412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("NELFINAVIR'UNK")){

            drugOrder.setConcept(conceptService.getConceptByUuid("80487AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("EMTRICITABINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("75628AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("ATAZANAVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("71647AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("ETRAVIRINE")){

            drugOrder.setConcept(conceptService.getConceptByUuid("159810AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if(regimen.contains("RALTEGRAVIR")){

            drugOrder.setConcept(conceptService.getConceptByUuid("154378AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
    }

    private void savePatientObsAtArvStart(Patient patient, List<Object> rowData) throws ParseException {

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setForm(formService.getFormByUuid("23b4ebbd-29ad-455e-be0e-04aa6bc30798"));
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        getLocation(encounter, rowData);
        encounter.setDateCreated(new Date());
        encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        encounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(1).toString()));

       /* Obs lastVisitToClinicObs = new Obs();//Last return to clinic obs
        lastVisitToClinicObs.setObsDatetime(convertToDate(rowData.get(22).toString()));
        lastVisitToClinicObs.setPerson(patient);
        lastVisitToClinicObs.setConcept(conceptService.getConceptByUuid(""));
        lastVisitToClinicObs.setValueDate(convertToDate(rowData.get(22).toString()));
        encounter.addObs(lastVisitToClinicObs);
*/
        Obs firstArvDateObs = new Obs();//first ARV dates
        firstArvDateObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        firstArvDateObs.setPerson(patient);
        firstArvDateObs.setConcept(conceptService.getConceptByUuid("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        firstArvDateObs.setValueDate(convertStringToDate.convert(rowData.get(1).toString()));
        encounter.addObs(firstArvDateObs);

        Obs whoStageObs = new Obs();//World Health Organization HIV stage
        whoStageObs.setPerson(patient);
        whoStageObs.setLocation(defaultLocation);
        whoStageObs.setConcept(conceptService.getConceptByUuid("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        String whoStageAnswer = rowData.get(9).toString();
        whoStageObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        checkForValueCodedForWhoStage(whoStageObs, whoStageAnswer);
        encounter.addObs(whoStageObs);

        Obs cd4CountObs = new Obs();//CD4 count Obs
        cd4CountObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        cd4CountObs.setPerson(patient);
        cd4CountObs.setConcept(conceptService.getConceptByUuid("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(10).toString() != "") {
            cd4CountObs.setValueNumeric(Double.valueOf(rowData.get(10).toString()));
            encounter.addObs(cd4CountObs);
        }

        Obs cd4PercentObs = new Obs();//CD4% Obs
        cd4PercentObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        cd4PercentObs.setPerson(patient);
        cd4PercentObs.setConcept(conceptService.getConceptByUuid("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(11).toString() != "") {
            cd4PercentObs.setValueNumeric(Double.valueOf(rowData.get(11).toString()));
            encounter.addObs(cd4PercentObs);
        }

        Obs heightObs = new Obs();//height Obs
        heightObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        heightObs.setPerson(patient);
        heightObs.setConcept(conceptService.getConceptByUuid("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(12).toString() != "") {
            heightObs.setValueNumeric(Double.valueOf(rowData.get(12).toString()));
            encounter.addObs(heightObs);
        }

        Obs weightObs = new Obs();//weight Obs
        weightObs.setObsDatetime(convertStringToDate.convert(rowData.get(1).toString()));
        weightObs.setPerson(patient);
        weightObs.setConcept(conceptService.getConceptByUuid("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        if (rowData.get(13).toString() != "") {
            weightObs.setValueNumeric(Double.valueOf(rowData.get(13).toString()));
            encounter.addObs(weightObs);
        }

        encounterService.saveEncounter(encounter);

        Encounter lastEncounter = new Encounter();
        lastEncounter.setPatient(patient);
        lastEncounter.setEncounterType(encounterService.getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));
        lastEncounter.setLocation(defaultLocation);
        lastEncounter.setDateCreated(new Date());
        lastEncounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
        lastEncounter.setEncounterDatetime(convertStringToDate.convert(rowData.get(22).toString()));

        if (rowData.get(22) != ""){
            encounterService.saveEncounter(lastEncounter);
        }

    }

    private void checkForValueCodedForWhoStage(Obs obs, String whoStageAnswer){

        if (whoStageAnswer != "") {
            String[] whoStage = whoStageAnswer.split(" ");

            if (whoStage[0].equals("whostage1")) {
                if (whoStage[1].equals("Ped")) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1204AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }

            } else if (whoStage[0].equals("whostage2")) {
                if (whoStage[1].equals("Ped")) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1221AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("whostage3")) {
                if (whoStage[1].equals("Ped")) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1222AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else if (whoStage[0].equals("whostage4")) {
                if (whoStage[1].equals("Ped")) {
                    obs.setValueCoded(conceptService.getConceptByUuid("1223AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }else{
                    obs.setValueCoded(conceptService.getConceptByUuid("1207AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
            } else {

                obs.setValueCoded(conceptService.getConceptByUuid("1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
    }

    private void getLocation(Encounter encounter, List<Object> rowData) {

        String location = rowData.get(14).toString();

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

}
