package org.openmrs.module.migrate.page.controller;

import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.migrate.MigrateConstant;
import org.openmrs.module.migrate.Moh361A;
import org.openmrs.module.migrate.Moh361B;
import org.openmrs.module.migrate.Moh408;
import org.openmrs.module.migrate.kakuma.KakumaEid;
import org.openmrs.module.migrate.kakuma.KakumaPatients;
import org.openmrs.module.migrate.kakuma.KakumaVisits;
import org.openmrs.module.migrate.kisii.*;
import org.openmrs.module.migrate.maragua.MaraguaPatients;
import org.openmrs.module.migrate.maragua.MaraguaVisits;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Created by derric on 6/19/14.
 */

@AppPage(MigrateConstant.APP_MIGRATE)
public class MigrationHomePageController {

    String globalPath;

    @RequestMapping()
    public void controller(UiUtils ui,
                           @RequestParam(value = "moh361Afile", required = false) String moh361Afile,
                           @RequestParam(value = "moh361Bfile", required = false) String moh361Bfile,
                           @RequestParam(value = "moh408file", required = false) String moh408file,
                           @RequestParam(value = "kakuma_patients", required = false) String kakuma_patients,
                           @RequestParam(value = "kakuma_visits", required = false) String kakuma_visits,
                           @RequestParam(value = "kakuma_eid", required = false) String kakuma_eid,
                           @RequestParam(value = "maragua_patients", required = false) String maragua_patients,
                           @RequestParam(value = "maragua_visits", required = false) String maragua_visits,
                           @RequestParam(value = "kisii_patients", required = false) String kisii_patients,
                           @RequestParam(value = "kisii_visits", required = false) String kisii_visits,
                           @RequestParam(value = "regmen_subs", required = false) String regmen_subs,
                           @RequestParam(value = "address", required = false) String address,
                           @RequestParam(value = "family", required = false) String family,
                           @RequestParam(value = "path", required = false) String filePath,
                           PageModel model,
                           HttpSession session,
                           @SpringBean KenyaUiUtils kenyaUi) throws Exception {

        if (filePath != ""){
            savePath(filePath);
        }

        globalPath = getPath() + "/";

        if (kakuma_patients != "") {
            String path = globalPath + kakuma_patients;
            KakumaPatients kakumaPatients = new KakumaPatients(path, session, kenyaUi);
            kakumaPatients.init();

        }
        if (kakuma_visits != "") {
            String path = globalPath + kakuma_visits;
            KakumaVisits kakumaVisits = new KakumaVisits(path, session, kenyaUi);
            kakumaVisits.init();

        }
        if (kakuma_eid != "") {
            String path = globalPath + kakuma_eid;
            KakumaEid kakumaEid= new KakumaEid(path, session, kenyaUi);
            kakumaEid.init();

        }

        if (maragua_patients != "") {
            String path = globalPath + maragua_patients;

            MaraguaPatients maraguaPatients = new MaraguaPatients(path, session, kenyaUi);
            maraguaPatients.init();
        }
        if (maragua_visits != "") {
            String path = globalPath + maragua_visits;

            MaraguaVisits maraguaVisits = new MaraguaVisits(path, session, kenyaUi);
            maraguaVisits.init();
        }

        if (kisii_patients != "") {
            String path = globalPath + kisii_patients;
            PatientsInfo patientsInfo = new PatientsInfo(path, session, kenyaUi);
            patientsInfo.init();
        }

        if (kisii_visits != "") {
            String path = globalPath + kisii_visits;
            Visits visits = new Visits(path, session, kenyaUi);
            visits.init();

        }
        if (regmen_subs != "") {
            String path = globalPath + regmen_subs;
            RegimenSubstitution regimenSubstitution = new RegimenSubstitution(path, session, kenyaUi);
            regimenSubstitution.init();

        }
        if (address != "") {
            String path = globalPath + address;
            AddressInfo addressInfo= new AddressInfo(path, session, kenyaUi);
            addressInfo.init();

        }
        if (family != "") {
            String path = globalPath + family;
            FamilyInfo familyInfo= new FamilyInfo(path, session, kenyaUi);
            familyInfo.init();

        }


        if (moh361Afile != "") {
            String path = globalPath + moh361Afile;
            // model.addAttribute("file", moh361Afile);

            Moh361A moh361A = new Moh361A(path, session, kenyaUi);
            moh361A.initMoh361A();
        }

        if (moh361Bfile != "") {
            String path = globalPath + moh361Bfile;

            Moh361B moh361B = new Moh361B(path, session, kenyaUi);
            moh361B.initMoh361B();

        }

        if (moh408file != "") {
            String path = globalPath + moh408file;
            Moh408 moh408 = new Moh408(path, session, kenyaUi);
            moh408.initMoh408();
        }

    }

    @Transactional(readOnly = true)
    public String getPath() {
        AdministrationService adminService = Context.getAdministrationService();
        String path = adminService.getGlobalProperty(MigrateConstant.FILE_PATH);
        return path;
    }

    @Transactional
    public void savePath(String path) {
        AdministrationService adminService = Context.getAdministrationService();
        GlobalProperty url = adminService.getGlobalPropertyObject(MigrateConstant.FILE_PATH);
        url.setPropertyValue(path);
        adminService.saveGlobalProperty(url);
    }

}
