package org.openmrs.module.migrate.fragment.controller;

import org.openmrs.module.migrate.HivProgramEnrollment;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Created by derric on 7/10/14.
 */
public class EnrollIntoHivProgramFragmentController {

    public SimpleObject controller(@RequestParam(value = "file", required = false) String file,
                                   UiUtils ui,
                                   HttpSession session,
                                   FragmentModel model) {

        model.addAttribute("file", file);

            HivProgramEnrollment hivProgramEnrollment = new HivProgramEnrollment();
            hivProgramEnrollment.init();

        return null;
    }
}
