package org.openmrs.module.migrate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by derric on 7/31/14.
 */
public class ConvertStringToDate1 {

    /*constructor*/
    public ConvertStringToDate1() {
    }

    public Date convert(String dateInString) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

        Date date = null;
        if (dateInString != "") {
            if (dateInString.contains("/")) {

                date = formatter.parse(dateInString);
            }

        }
        return date;
    }
}
